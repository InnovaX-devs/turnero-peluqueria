package com.innovax.barberia.barbero;

import com.innovax.barberia.auth.RecuperacionService;
import com.innovax.barberia.auth.Rol;
import com.innovax.barberia.auth.Usuario;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.auth.UsuarioRepository;
import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.barbero.dto.BarberoRequests;
import com.innovax.barberia.barbero.dto.BarberoResponse;
import com.innovax.barberia.barbero.dto.HorarioResponse;
import com.innovax.barberia.barbero.dto.OpcionesBarberoResponse;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.horario.HorarioLaboral;
import com.innovax.barberia.horario.HorarioLaboralRepository;
import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.servicio.ServicioRepository;
import com.innovax.barberia.sucursal.Sucursal;
import com.innovax.barberia.sucursal.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** ABM de barberos. Todo se filtra por la barbería del token, nunca por el request. */
@Service
@RequiredArgsConstructor
public class BarberoService {

    private static final Locale ES = Locale.of("es", "AR");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BarberoRepository barberos;
    private final BarberiaRepository barberias;
    private final SucursalRepository sucursales;
    private final ServicioRepository servicios;
    private final HorarioLaboralRepository horarios;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioActual usuarioActual;
    private final FotoStorageService fotos;
    private final RecuperacionService recuperacion;

    // ------------------------------------------------------------------ lectura

    @Transactional(readOnly = true)
    public List<BarberoResponse> listar(boolean incluirInactivos) {
        return barberos.findByBarberiaIdOrderByNombreAscApellidoAsc(usuarioActual.barberiaId()).stream()
                .filter(b -> incluirInactivos || b.isActivo())
                .map(BarberoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public BarberoResponse obtener(UUID id) {
        return BarberoResponse.de(buscar(id));
    }

    /** Sucursales y servicios activos que se pueden asignar. */
    @Transactional(readOnly = true)
    public OpcionesBarberoResponse opciones() {
        UUID barberiaId = usuarioActual.barberiaId();
        return new OpcionesBarberoResponse(
                sucursales.findByBarberiaIdAndActivaTrueOrderByNombreAsc(barberiaId).stream()
                        .map(s -> new OpcionesBarberoResponse.SucursalOpcion(s.getId(), s.getNombre(), s.getDireccion()))
                        .toList(),
                servicios.findByBarberiaIdAndActivoTrueOrderByOrdenAscNombreAsc(barberiaId).stream()
                        .map(s -> new OpcionesBarberoResponse.ServicioOpcion(s.getId(), s.getNombre(), s.getDuracionMin()))
                        .toList());
    }

    // ------------------------------------------------------------------ alta / edición / baja lógica

    @Transactional
    public BarberoResponse crear(BarberoRequests.Crear request) {
        Barberia barberia = barberias.findById(usuarioActual.barberiaId())
                .orElseThrow(() -> new IllegalStateException("La barbería del token no existe"));
        Barbero barbero = new Barbero();
        barbero.setBarberia(barberia);
        aplicarDatos(barbero, request.nombre(), request.apellido(), request.sucursalIds(), request.servicioIds());

        Usuario usuario = null;
        if (request.usuario() != null) {
            usuario = nuevoUsuario(barberia, true, request.usuario());
            barbero.setUsuario(usuario);
        }
        barberos.save(barbero);
        if (usuario != null) {
            recuperacion.invitar(usuario);
        }
        return BarberoResponse.de(barbero);
    }

    @Transactional
    public BarberoResponse actualizar(UUID id, BarberoRequests.Actualizar request) {
        Barbero barbero = buscar(id);
        aplicarDatos(barbero, request.nombre(), request.apellido(), request.sucursalIds(), request.servicioIds());

        // Si le sacaron una sucursal, sus horarios en esa sucursal ya no tienen sentido
        Set<UUID> sucursalesActuales = barbero.getSucursales().stream()
                .map(Sucursal::getId).collect(Collectors.toSet());
        List<HorarioLaboral> sobrantes = horarios.findByBarberoIdOrderByDiaSemanaAscHoraInicioAsc(id).stream()
                .filter(h -> !sucursalesActuales.contains(h.getSucursal().getId()))
                .toList();
        horarios.deleteAll(sobrantes);

        return BarberoResponse.de(barbero);
    }

    /**
     * Baja lógica: nunca se borra, para conservar el historial de atenciones.
     * Si tiene usuario de barbero, se activa/desactiva junto con él (el login ya filtra por usuario activo).
     * Un usuario ADMIN vinculado a un barbero no se toca, para no dejar al dueño sin acceso.
     */
    @Transactional
    public BarberoResponse cambiarActivo(UUID id, boolean activo) {
        Barbero barbero = buscar(id);
        barbero.setActivo(activo);
        Usuario usuario = barbero.getUsuario();
        if (usuario != null && usuario.getRol() == Rol.BARBERO) {
            usuario.setActivo(activo);
        }
        return BarberoResponse.de(barbero);
    }

    // ------------------------------------------------------------------ usuario de acceso

    /**
     * Crea el usuario del barbero con una contraseña que nadie conoce y le manda un email para que elija la suya.
     * Si el barbero está inactivo no se manda nada: al reactivarlo, el admin puede reenviar la invitación.
     */
    @Transactional
    public BarberoResponse crearUsuario(UUID id, BarberoRequests.CrearUsuario request) {
        Barbero barbero = buscar(id);
        if (barbero.getUsuario() != null) {
            throw new ConflictoException("email", "Este barbero ya tiene un usuario");
        }
        Usuario usuario = nuevoUsuario(barbero.getBarberia(), barbero.isActivo(), request);
        barbero.setUsuario(usuario);
        if (barbero.isActivo()) {
            recuperacion.invitar(usuario);
        }
        return BarberoResponse.de(barbero);
    }

    /** Manda un link nuevo (invalida los anteriores). Sirve si la invitación venció o no llegó. */
    @Transactional
    public void reenviarInvitacion(UUID id) {
        Barbero barbero = buscar(id);
        Usuario usuario = barbero.getUsuario();
        if (usuario == null || usuario.getRol() != Rol.BARBERO) {
            throw new DatosInvalidosException("usuario", "Este barbero no tiene usuario para entrar al panel");
        }
        if (!usuario.isActivo()) {
            throw new DatosInvalidosException("usuario",
                    "El barbero está inactivo. Reactivalo para reenviar la invitación");
        }
        recuperacion.invitar(usuario);
    }

    // ------------------------------------------------------------------ foto

    @Transactional
    public BarberoResponse subirFoto(UUID id, MultipartFile archivo) {
        Barbero barbero = buscar(id);
        String anterior = barbero.getFotoUrl();
        barbero.setFotoUrl(fotos.subir(barbero.getBarberia().getId(), id, archivo));
        fotos.borrarSilencioso(anterior);
        return BarberoResponse.de(barbero);
    }

    @Transactional
    public BarberoResponse quitarFoto(UUID id) {
        Barbero barbero = buscar(id);
        String anterior = barbero.getFotoUrl();
        barbero.setFotoUrl(null);
        fotos.borrarSilencioso(anterior);
        return BarberoResponse.de(barbero);
    }

    // ------------------------------------------------------------------ horarios

    @Transactional(readOnly = true)
    public List<HorarioResponse> horarios(UUID id) {
        buscar(id);
        return horarios.findByBarberoIdOrderByDiaSemanaAscHoraInicioAsc(id).stream()
                .map(HorarioResponse::de)
                .toList();
    }

    /** Reemplaza todo el horario semanal. Los turnos ya reservados no se tocan. */
    @Transactional
    public List<HorarioResponse> guardarHorarios(UUID id, BarberoRequests.GuardarHorarios request) {
        Barbero barbero = buscar(id);
        Map<UUID, Sucursal> asignadas = barbero.getSucursales().stream()
                .collect(Collectors.toMap(Sucursal::getId, Function.identity()));
        validarHorarios(request.horarios(), asignadas.keySet());

        horarios.borrarDeBarbero(id);
        List<HorarioLaboral> nuevos = request.horarios().stream().map(h -> {
            HorarioLaboral nuevo = new HorarioLaboral();
            nuevo.setBarbero(barbero);
            nuevo.setSucursal(asignadas.get(h.sucursalId()));
            nuevo.setDiaSemana(DayOfWeek.of(h.diaSemana()));
            nuevo.setHoraInicio(h.horaInicio());
            nuevo.setHoraFin(h.horaFin());
            return nuevo;
        }).toList();

        return horarios.saveAll(nuevos).stream()
                .sorted(Comparator.comparing((HorarioLaboral h) -> h.getDiaSemana().getValue())
                        .thenComparing(HorarioLaboral::getHoraInicio))
                .map(HorarioResponse::de)
                .toList();
    }

    private void validarHorarios(List<BarberoRequests.Horario> tramos, Set<UUID> sucursalesDelBarbero) {
        Map<Integer, List<BarberoRequests.Horario>> porDia = new HashMap<>();
        for (BarberoRequests.Horario h : tramos) {
            if (!sucursalesDelBarbero.contains(h.sucursalId())) {
                throw new DatosInvalidosException("horarios",
                        "Hay horarios en una sucursal donde el barbero no trabaja. Asignale la sucursal primero.");
            }
            if (!h.horaFin().isAfter(h.horaInicio())) {
                throw new DatosInvalidosException("horarios", "La hora de fin tiene que ser posterior a la de inicio");
            }
            porDia.computeIfAbsent(h.diaSemana(), d -> new java.util.ArrayList<>()).add(h);
        }
        // Un barbero no puede estar en dos lugares a la vez: sin superposición en el día, ni entre sucursales
        for (Map.Entry<Integer, List<BarberoRequests.Horario>> dia : porDia.entrySet()) {
            List<BarberoRequests.Horario> ordenados = dia.getValue().stream()
                    .sorted(Comparator.comparing(BarberoRequests.Horario::horaInicio))
                    .toList();
            LocalTime finMaximo = null;
            for (BarberoRequests.Horario h : ordenados) {
                if (finMaximo != null && h.horaInicio().isBefore(finMaximo)) {
                    String nombreDia = DayOfWeek.of(dia.getKey()).getDisplayName(TextStyle.FULL, ES);
                    throw new DatosInvalidosException("horarios", "Los tramos del " + nombreDia + " se superponen");
                }
                finMaximo = finMaximo == null || h.horaFin().isAfter(finMaximo) ? h.horaFin() : finMaximo;
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private Barbero buscar(UUID id) {
        return barberos.findByIdAndBarberiaId(id, usuarioActual.barberiaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Barbero no encontrado"));
    }

    private void aplicarDatos(Barbero barbero, String nombre, String apellido,
                              Set<UUID> sucursalIds, Set<UUID> servicioIds) {
        UUID barberiaId = barbero.getBarberia().getId();
        barbero.setNombre(nombre.trim());
        barbero.setApellido(apellido.trim());

        Map<UUID, Sucursal> activas = sucursales.findByBarberiaIdAndActivaTrueOrderByNombreAsc(barberiaId).stream()
                .collect(Collectors.toMap(Sucursal::getId, Function.identity()));
        Set<Sucursal> nuevasSucursales = sucursalIds.stream().map(sid -> {
            Sucursal s = activas.get(sid);
            if (s == null) {
                throw new DatosInvalidosException("sucursalIds", "Alguna sucursal elegida no existe o está inactiva");
            }
            return s;
        }).collect(Collectors.toSet());

        Map<UUID, Servicio> activos = servicios.findByBarberiaIdAndActivoTrueOrderByOrdenAscNombreAsc(barberiaId).stream()
                .collect(Collectors.toMap(Servicio::getId, Function.identity()));
        Set<Servicio> nuevosServicios = servicioIds.stream().map(sid -> {
            Servicio s = activos.get(sid);
            if (s == null) {
                throw new DatosInvalidosException("servicioIds", "Algún servicio elegido no existe o está inactivo");
            }
            return s;
        }).collect(Collectors.toSet());

        barbero.getSucursales().clear();
        barbero.getSucursales().addAll(nuevasSucursales);
        barbero.getServicios().clear();
        barbero.getServicios().addAll(nuevosServicios);
    }

    /** El usuario nace con una contraseña aleatoria que nadie conoce: la define el barbero desde la invitación. */
    private Usuario nuevoUsuario(Barberia barberia, boolean activo, BarberoRequests.CrearUsuario request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarios.findByEmailIgnoreCase(email).isPresent()) {
            throw emailEnUso();
        }
        Usuario usuario = new Usuario();
        usuario.setBarberia(barberia);
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(passwordInutilizable()));
        usuario.setRol(Rol.BARBERO);
        usuario.setActivo(activo);
        try {
            return usuarios.saveAndFlush(usuario); // el UNIQUE de la base cubre la carrera entre dos altas
        } catch (DataIntegrityViolationException e) {
            throw emailEnUso();
        }
    }

    private static String passwordInutilizable() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); // 43 chars, entra en los 72 de BCrypt
    }

    private ConflictoException emailEnUso() {
        return new ConflictoException("email", "Ese email ya está en uso");
    }
}
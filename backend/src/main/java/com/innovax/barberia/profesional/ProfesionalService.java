package com.innovax.barberia.profesional;

import com.innovax.barberia.auth.RecuperacionService;
import com.innovax.barberia.auth.Rol;
import com.innovax.barberia.auth.Usuario;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.auth.UsuarioRepository;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.profesional.dto.ProfesionalRequests;
import com.innovax.barberia.profesional.dto.ProfesionalResponse;
import com.innovax.barberia.profesional.dto.HorarioResponse;
import com.innovax.barberia.profesional.dto.OpcionesProfesionalResponse;
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
public class ProfesionalService {

    private static final Locale ES = Locale.of("es", "AR");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ProfesionalRepository profesionales;
    private final NegocioRepository negocios;
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
    public List<ProfesionalResponse> listar(boolean incluirInactivos) {
        return profesionales.findByNegocioIdOrderByNombreAscApellidoAsc(usuarioActual.negocioId()).stream()
                .filter(b -> incluirInactivos || b.isActivo())
                .map(ProfesionalResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfesionalResponse obtener(UUID id) {
        return ProfesionalResponse.de(buscar(id));
    }

    /** Sucursales y servicios activos que se pueden asignar. */
    @Transactional(readOnly = true)
    public OpcionesProfesionalResponse opciones() {
        UUID negocioId = usuarioActual.negocioId();
        return new OpcionesProfesionalResponse(
                sucursales.findByNegocioIdAndActivaTrueOrderByNombreAsc(negocioId).stream()
                        .map(s -> new OpcionesProfesionalResponse.SucursalOpcion(s.getId(), s.getNombre(), s.getDireccion()))
                        .toList(),
                servicios.findByNegocioIdAndActivoTrueOrderByOrdenAscNombreAsc(negocioId).stream()
                        .map(s -> new OpcionesProfesionalResponse.ServicioOpcion(s.getId(), s.getNombre(), s.getDuracionMin()))
                        .toList());
    }

    // ------------------------------------------------------------------ alta / edición / baja lógica

    @Transactional
    public ProfesionalResponse crear(ProfesionalRequests.Crear request) {
        Negocio negocio = negocios.findById(usuarioActual.negocioId())
                .orElseThrow(() -> new IllegalStateException("El negocio del token no existe"));
        Profesional profesional = new Profesional();
        profesional.setNegocio(negocio);
        aplicarDatos(profesional, request.nombre(), request.apellido(), request.sucursalIds(), request.servicioIds());

        Usuario usuario = null;
        if (request.usuario() != null) {
            usuario = nuevoUsuario(negocio, true, request.usuario());
            profesional.setUsuario(usuario);
        }
        profesionales.save(profesional);
        if (usuario != null) {
            recuperacion.invitar(usuario);
        }
        return ProfesionalResponse.de(profesional);
    }

    @Transactional
    public ProfesionalResponse actualizar(UUID id, ProfesionalRequests.Actualizar request) {
        Profesional profesional = buscar(id);
        aplicarDatos(profesional, request.nombre(), request.apellido(), request.sucursalIds(), request.servicioIds());

        // Si le sacaron una sucursal, sus horarios en esa sucursal ya no tienen sentido
        Set<UUID> sucursalesActuales = profesional.getSucursales().stream()
                .map(Sucursal::getId).collect(Collectors.toSet());
        List<HorarioLaboral> sobrantes = horarios.findByProfesionalIdOrderByDiaSemanaAscHoraInicioAsc(id).stream()
                .filter(h -> !sucursalesActuales.contains(h.getSucursal().getId()))
                .toList();
        horarios.deleteAll(sobrantes);

        return ProfesionalResponse.de(profesional);
    }

    /**
     * Baja lógica: nunca se borra, para conservar el historial de atenciones.
     * Si tiene usuario de barbero, se activa/desactiva junto con él (el login ya filtra por usuario activo).
     * Un usuario ADMIN vinculado a un barbero no se toca, para no dejar al dueño sin acceso.
     */
    @Transactional
    public ProfesionalResponse cambiarActivo(UUID id, boolean activo) {
        Profesional profesional = buscar(id);
        profesional.setActivo(activo);
        Usuario usuario = profesional.getUsuario();
        if (usuario != null && usuario.getRol() == Rol.PROFESIONAL) {
            usuario.setActivo(activo);
        }
        return ProfesionalResponse.de(profesional);
    }

    // ------------------------------------------------------------------ usuario de acceso

    /**
     * Crea el usuario del barbero con una contraseña que nadie conoce y le manda un email para que elija la suya.
     * Si el barbero está inactivo no se manda nada: al reactivarlo, el admin puede reenviar la invitación.
     */
    @Transactional
    public ProfesionalResponse crearUsuario(UUID id, ProfesionalRequests.CrearUsuario request) {
        Profesional profesional = buscar(id);
        if (profesional.getUsuario() != null) {
            throw new ConflictoException("email", "Este profesional ya tiene un usuario");
        }
        Usuario usuario = nuevoUsuario(profesional.getNegocio(), profesional.isActivo(), request);
        profesional.setUsuario(usuario);
        if (profesional.isActivo()) {
            recuperacion.invitar(usuario);
        }
        return ProfesionalResponse.de(profesional);
    }

    /** Manda un link nuevo (invalida los anteriores). Sirve si la invitación venció o no llegó. */
    @Transactional
    public void reenviarInvitacion(UUID id) {
        Profesional profesional = buscar(id);
        Usuario usuario = profesional.getUsuario();
        if (usuario == null || usuario.getRol() != Rol.PROFESIONAL) {
            throw new DatosInvalidosException("usuario", "Este profesional no tiene usuario para entrar al panel");
        }
        if (!usuario.isActivo()) {
            throw new DatosInvalidosException("usuario",
                    "El profesional está inactivo. Reactivalo para reenviar la invitación");
        }
        recuperacion.invitar(usuario);
    }

    // ------------------------------------------------------------------ foto

    @Transactional
    public ProfesionalResponse subirFoto(UUID id, MultipartFile archivo) {
        Profesional profesional = buscar(id);
        String anterior = profesional.getFotoUrl();
        profesional.setFotoUrl(fotos.subir(profesional.getNegocio().getId(), id, archivo));
        fotos.borrarSilencioso(anterior);
        return ProfesionalResponse.de(profesional);
    }

    @Transactional
    public ProfesionalResponse quitarFoto(UUID id) {
        Profesional profesional = buscar(id);
        String anterior = profesional.getFotoUrl();
        profesional.setFotoUrl(null);
        fotos.borrarSilencioso(anterior);
        return ProfesionalResponse.de(profesional);
    }

    // ------------------------------------------------------------------ horarios

    @Transactional(readOnly = true)
    public List<HorarioResponse> horarios(UUID id) {
        buscar(id);
        return horarios.findByProfesionalIdOrderByDiaSemanaAscHoraInicioAsc(id).stream()
                .map(HorarioResponse::de)
                .toList();
    }

    /** Reemplaza todo el horario semanal. Los turnos ya reservados no se tocan. */
    @Transactional
    public List<HorarioResponse> guardarHorarios(UUID id, ProfesionalRequests.GuardarHorarios request) {
        Profesional profesional = buscar(id);
        Map<UUID, Sucursal> asignadas = profesional.getSucursales().stream()
                .collect(Collectors.toMap(Sucursal::getId, Function.identity()));
        validarHorarios(request.horarios(), asignadas.keySet());

        horarios.borrarDeProfesional(id);
        List<HorarioLaboral> nuevos = request.horarios().stream().map(h -> {
            HorarioLaboral nuevo = new HorarioLaboral();
            nuevo.setProfesional(profesional);
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

    private void validarHorarios(List<ProfesionalRequests.Horario> tramos, Set<UUID> sucursalesDelProfesional) {
        Map<Integer, List<ProfesionalRequests.Horario>> porDia = new HashMap<>();
        for (ProfesionalRequests.Horario h : tramos) {
            if (!sucursalesDelProfesional.contains(h.sucursalId())) {
                throw new DatosInvalidosException("horarios",
                        "Hay horarios en una sucursal donde el profesional no trabaja. Asignale la sucursal primero.");
            }
            if (!h.horaFin().isAfter(h.horaInicio())) {
                throw new DatosInvalidosException("horarios", "La hora de fin tiene que ser posterior a la de inicio");
            }
            porDia.computeIfAbsent(h.diaSemana(), d -> new java.util.ArrayList<>()).add(h);
        }
        // Un barbero no puede estar en dos lugares a la vez: sin superposición en el día, ni entre sucursales
        for (Map.Entry<Integer, List<ProfesionalRequests.Horario>> dia : porDia.entrySet()) {
            List<ProfesionalRequests.Horario> ordenados = dia.getValue().stream()
                    .sorted(Comparator.comparing(ProfesionalRequests.Horario::horaInicio))
                    .toList();
            LocalTime finMaximo = null;
            for (ProfesionalRequests.Horario h : ordenados) {
                if (finMaximo != null && h.horaInicio().isBefore(finMaximo)) {
                    String nombreDia = DayOfWeek.of(dia.getKey()).getDisplayName(TextStyle.FULL, ES);
                    throw new DatosInvalidosException("horarios", "Los tramos del " + nombreDia + " se superponen");
                }
                finMaximo = finMaximo == null || h.horaFin().isAfter(finMaximo) ? h.horaFin() : finMaximo;
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private Profesional buscar(UUID id) {
        return profesionales.findByIdAndNegocioId(id, usuarioActual.negocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesional no encontrado"));
    }

    private void aplicarDatos(Profesional profesional, String nombre, String apellido,
                              Set<UUID> sucursalIds, Set<UUID> servicioIds) {
        UUID negocioId = profesional.getNegocio().getId();
        profesional.setNombre(nombre.trim());
        profesional.setApellido(apellido.trim());

        Map<UUID, Sucursal> activas = sucursales.findByNegocioIdAndActivaTrueOrderByNombreAsc(negocioId).stream()
                .collect(Collectors.toMap(Sucursal::getId, Function.identity()));
        Set<Sucursal> nuevasSucursales = sucursalIds.stream().map(sid -> {
            Sucursal s = activas.get(sid);
            if (s == null) {
                throw new DatosInvalidosException("sucursalIds", "Alguna sucursal elegida no existe o está inactiva");
            }
            return s;
        }).collect(Collectors.toSet());

        Map<UUID, Servicio> activos = servicios.findByNegocioIdAndActivoTrueOrderByOrdenAscNombreAsc(negocioId).stream()
                .collect(Collectors.toMap(Servicio::getId, Function.identity()));
        Set<Servicio> nuevosServicios = servicioIds.stream().map(sid -> {
            Servicio s = activos.get(sid);
            if (s == null) {
                throw new DatosInvalidosException("servicioIds", "Algún servicio elegido no existe o está inactivo");
            }
            return s;
        }).collect(Collectors.toSet());

        profesional.getSucursales().clear();
        profesional.getSucursales().addAll(nuevasSucursales);
        profesional.getServicios().clear();
        profesional.getServicios().addAll(nuevosServicios);
    }

    /** El usuario nace con una contraseña aleatoria que nadie conoce: la define el barbero desde la invitación. */
    private Usuario nuevoUsuario(Negocio negocio, boolean activo, ProfesionalRequests.CrearUsuario request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarios.findByEmailIgnoreCase(email).isPresent()) {
            throw emailEnUso();
        }
        Usuario usuario = new Usuario();
        usuario.setNegocio(negocio);
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(passwordInutilizable()));
        usuario.setRol(Rol.PROFESIONAL);
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
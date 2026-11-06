package com.innovax.barberia.atencion;

import com.innovax.barberia.atencion.dto.AtencionResponse;
import com.innovax.barberia.atencion.dto.OpcionesRegistroResponse;
import com.innovax.barberia.atencion.dto.RegistrarCorteRequest;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.profesional.Profesional;
import com.innovax.barberia.profesional.ProfesionalRepository;
import com.innovax.barberia.cliente.Cliente;
import com.innovax.barberia.cliente.ClienteRepository;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.servicio.ServicioRepository;
import com.innovax.barberia.sucursal.Sucursal;
import com.innovax.barberia.sucursal.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Registro de cortes en el local (cliente sin turno). Con reservas_online = false es la
 * función principal: todo tiene valores por defecto para que el caso común sea rápido.
 */
@Service
@RequiredArgsConstructor
public class RegistroCorteService {

    /** Margen para diferencias de reloj entre el celular y el servidor. */
    private static final Duration TOLERANCIA_FUTURO = Duration.ofMinutes(5);
    private static final Set<EstadoAtencion> ACTIVAS = EnumSet.of(EstadoAtencion.RESERVADA, EstadoAtencion.COMPLETADA);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final UsuarioActual usuarioActual;
    private final NegocioRepository negocios;
    private final ProfesionalRepository profesionales;
    private final ServicioRepository servicios;
    private final SucursalRepository sucursales;
    private final ClienteRepository clientes;
    private final AtencionRepository atenciones;

    @Transactional(readOnly = true)
    public OpcionesRegistroResponse opciones() {
        Negocio negocio = negocioActual();
        UUID propio = usuarioActual.profesionalId().orElse(null);
        // Admin y recepción eligen entre todos; el profesional solo se ve a sí mismo
        List<Profesional> visibles = profesionales.findByNegocioIdAndActivoTrueOrderByNombreAscApellidoAsc(negocio.getId())
                .stream()
                .filter(b -> usuarioActual.veTodoElNegocio() || b.getId().equals(propio))
                .toList();
        return OpcionesRegistroResponse.de(negocio, propio,
                servicios.findByNegocioIdAndActivoTrueOrderByOrdenAscNombreAsc(negocio.getId()),
                visibles,
                sucursales.findByNegocioIdAndActivaTrueOrderByNombreAsc(negocio.getId()));
    }

    @Transactional
    public AtencionResponse registrar(RegistrarCorteRequest request) {
        Negocio negocio = negocioActual();
        ZoneId zona = ZoneId.of(negocio.getZonaHoraria());

        Profesional profesional = resolverProfesional(negocio, request.profesionalId());
        Sucursal sucursal = resolverSucursal(negocio, profesional, request.sucursalId());
        Servicio servicio = servicios.findById(request.servicioId())
                .filter(Servicio::isActivo) 
                .filter(s -> s.getNegocio().getId().equals(negocio.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
        Cliente cliente = null;
        if (request.clienteId() != null) {
            cliente = clientes.findById(request.clienteId())
                    .filter(c -> c.getNegocio().getId().equals(negocio.getId()))
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
        }

        Instant inicio = resolverInicio(zona, request.fecha(), request.hora());
        Instant fin = acomodarSuperposiciones(profesional, zona, inicio,
                inicio.plus(Duration.ofMinutes(servicio.getDuracionMin())));

        Atencion atencion = new Atencion();
        atencion.setNegocio(negocio);
        atencion.setSucursal(sucursal);
        atencion.setProfesional(profesional);
        atencion.setCliente(cliente);
        atencion.setServicio(servicio);
        atencion.setInicio(inicio);
        atencion.setFin(fin);
        atencion.setEstado(EstadoAtencion.COMPLETADA);
        atencion.setOrigen(OrigenAtencion.MANUAL);
        atencion.setPrecioFinal(request.precioFinal());
        atencion.setMetodoPago(request.metodoPago());
        atencion.setNotas(request.notas() == null || request.notas().isBlank() ? null : request.notas().trim());

        try {
            atenciones.saveAndFlush(atencion);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("excl_atencion_superpuesta")) {
                throw new ConflictoException("horario", "Ese barbero ya tiene otra atención en ese horario");
            }
            throw e;
        }
        return AtencionResponse.de(atencion);
    }

    /** Profesional logueado: siempre él mismo. Admin o recepción: el que elija (o él mismo si también atiende). */
    private Profesional resolverProfesional(Negocio negocio, UUID profesionalPedido) {
        UUID profesionalId;
        if (!usuarioActual.veTodoElNegocio()) {
            profesionalId = usuarioActual.profesionalId()
                    .orElseThrow(() -> new AccessDeniedException("El usuario no está vinculado a un barbero"));
            if (profesionalPedido != null) {
                usuarioActual.verificarAccesoAProfesional(profesionalPedido); // 403 si pide otro
            }
        } else if (profesionalPedido != null) {
            profesionalId = profesionalPedido;
        } else {
            profesionalId = usuarioActual.profesionalId()
                    .orElseThrow(() -> new DatosInvalidosException("profesionalId", "Elegí el barbero"));
        }
        return profesionales.findById(profesionalId)
                .filter(b -> b.getNegocio().getId().equals(negocio.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Barbero no encontrado"));
    }

    /** Si el barbero trabaja en una sola sucursal, no hace falta elegirla. */
    private Sucursal resolverSucursal(Negocio negocio, Profesional profesional, UUID sucursalId) {
        if (sucursalId == null) {
            List<Sucursal> activas = profesional.getSucursales().stream().filter(Sucursal::isActiva).toList();
            if (activas.size() != 1) {
                throw new DatosInvalidosException("sucursalId", "Elegí la sucursal");
            }
            return activas.get(0);
        }
        Sucursal sucursal = sucursales.findById(sucursalId)
                .filter(s -> s.getNegocio().getId().equals(negocio.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
        if (!profesional.getSucursales().contains(sucursal)) {
            throw new DatosInvalidosException("sucursalId", "Ese barbero no trabaja en esa sucursal");
        }
        return sucursal;
    }

    /** Sin fecha ni hora: ahora. Con fecha y hora: se permiten cortes pasados, no futuros. */
    private Instant resolverInicio(ZoneId zona, LocalDate fecha, LocalTime hora) {
        Instant ahora = Instant.now();
        if (fecha == null && hora == null) {
            return ahora;
        }
        if (fecha == null || hora == null) {
            throw new DatosInvalidosException("fecha", "Indicá la fecha y la hora");
        }
        
        Instant inicio = ZonedDateTime.of(fecha, hora, zona).toInstant();
        if(inicio.isAfter(ahora.plus(TOLERANCIA_FUTURO))){
            throw new DatosInvalidosException("fecha", "No se puede registar un corte en el futuro");
        }

        return inicio;
    }

    /**
     * La base no permite dos atenciones activas superpuestas del mismo barbero. Un corte real
     * suele durar menos que la duración nominal del servicio, así que dos cortes seguidos
     * chocarían. Se acomodan así:
     * - una atención COMPLETADA que empezó antes termina cuando empieza esta;
     * - si hay una atención que empieza después, esta termina cuando empieza aquella;
     * - un turno RESERVADO que empezó antes y sigue pendiente es un conflicto real: 409.
     */
    private Instant acomodarSuperposiciones(Profesional profesional, ZoneId zona, Instant inicio, Instant fin) {
        Instant finAjustado = fin;
        for (Atencion otra : atenciones.findOcupadas(profesional.getId(), ACTIVAS, inicio, fin)) {
            if (otra.getInicio().equals(inicio)) {
                throw new ConflictoException("horario", "Ese barbero ya tiene una atención registrada a esa misma hora");
            }
            if (otra.getInicio().isAfter(inicio)) {
                if (otra.getInicio().isBefore(finAjustado)) {
                    finAjustado = otra.getInicio();
                }
            } else if (otra.getEstado() == EstadoAtencion.COMPLETADA) {
                otra.setFin(inicio);
            } else {
                throw new ConflictoException("horario", "Hay un turno reservado a las "
                        + HORA.format(otra.getInicio().atZone(zona))
                        + " que todavía no se marcó como atendido o ausente");
            }
        }
        // Se guardan los recortes antes de insertar el corte nuevo: si no, la base ve la superposición
        atenciones.flush();
        return finAjustado;
    }

    private Negocio negocioActual() {
        return negocios.findById(usuarioActual.negocioId())
                .orElseThrow(() -> new IllegalStateException("La barbería del token no existe"));
    }
}
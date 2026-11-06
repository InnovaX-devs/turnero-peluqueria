package com.innovax.barberia.agenda;

import com.innovax.barberia.agenda.dto.HorariosLibresResponse;
import com.innovax.barberia.agenda.dto.NuevoTurnoRequest;
import com.innovax.barberia.agenda.dto.TurnoAgendaResponse;
import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.OrigenAtencion;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.cliente.Cliente;
import com.innovax.barberia.cliente.ClienteRepository;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.disponibilidad.CalculadoraDisponibilidad;
import com.innovax.barberia.disponibilidad.Intervalo;
import com.innovax.barberia.horario.BloqueoRepository;
import com.innovax.barberia.horario.HorarioLaboral;
import com.innovax.barberia.horario.HorarioLaboralRepository;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.notificacion.NotificacionService;
import com.innovax.barberia.profesional.Profesional;
import com.innovax.barberia.profesional.ProfesionalRepository;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Dar turno desde el panel (issue #71): por teléfono o en el mostrador, sin pasar por la reserva online.
 *
 * <p>Turno normal: tiene que ser un horario libre de la grilla del profesional (mismo cálculo que la
 * reserva online, pero sin la anticipación mínima ni el interruptor de reservas online, que son reglas
 * para el cliente). Lo puede dar admin, recepción o el propio profesional.
 *
 * <p>Sobreturno: solo admin y recepción. Tiene que ir encima de otro turno o fuera del horario del
 * profesional; si el horario está libre dentro de la grilla, se pide cargarlo como turno normal (así un
 * sobreturno nunca ocupa un hueco que la reserva online podría ofrecer). Nunca encima de un bloqueo.
 */
@Service
@RequiredArgsConstructor
public class TurnoPanelService {

    static final String MENSAJE_HORARIO_OCUPADO = "Ese horario ya no está libre. Elegí otro o cargalo como sobreturno.";

    /** Margen para diferencias de reloj entre la compu y el servidor. */
    private static final Duration TOLERANCIA_PASADO = Duration.ofMinutes(5);
    private static final Set<EstadoAtencion> OCUPAN = EnumSet.of(EstadoAtencion.RESERVADA, EstadoAtencion.COMPLETADA);

    private final UsuarioActual usuarioActual;
    private final NegocioRepository negocios;
    private final ProfesionalRepository profesionales;
    private final ServicioRepository servicios;
    private final SucursalRepository sucursales;
    private final ClienteRepository clientes;
    private final AtencionRepository atenciones;
    private final BloqueoRepository bloqueos;
    private final HorarioLaboralRepository horarios;
    private final NotificacionService notificaciones;

    @Transactional(readOnly = true)
    public HorariosLibresResponse horariosLibres(UUID profesionalId, UUID servicioId, UUID sucursalId, LocalDate fecha) {
        Negocio negocio = negocioActual();
        Profesional profesional = resolverProfesional(negocio, profesionalId);
        Sucursal sucursal = resolverSucursal(negocio, profesional, sucursalId);
        Servicio servicio = resolverServicio(negocio, profesional, servicioId);
        return new HorariosLibresResponse(fecha, libres(negocio, profesional, sucursal, servicio, fecha));
    }

    @Transactional
    public TurnoAgendaResponse crear(NuevoTurnoRequest request) {
        Negocio negocio = negocioActual();
        ZoneId zona = ZoneId.of(negocio.getZonaHoraria());
        boolean sobreturno = Boolean.TRUE.equals(request.sobreturno());
        if (sobreturno && !usuarioActual.veTodoElNegocio()) {
            throw new AccessDeniedException("Solo el admin o recepción pueden dar sobreturnos");
        }

        Profesional profesional = resolverProfesional(negocio, request.profesionalId());
        Sucursal sucursal = resolverSucursal(negocio, profesional, request.sucursalId());
        Servicio servicio = resolverServicio(negocio, profesional, request.servicioId());
        Cliente cliente = clientes.findByIdAndNegocioId(request.clienteId(), negocio.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));

        Instant inicio = ZonedDateTime.of(request.fecha(), request.hora(), zona).toInstant();
        Instant fin = inicio.plus(Duration.ofMinutes(servicio.getDuracionMin()));

        if (sobreturno) {
            validarSobreturno(profesional, sucursal, servicio, request.fecha(), request.hora(), inicio, fin);
        } else if (!libres(negocio, profesional, sucursal, servicio, request.fecha()).contains(request.hora())) {
            throw new ConflictoException("horario", MENSAJE_HORARIO_OCUPADO);
        }

        Atencion atencion = new Atencion();
        atencion.setNegocio(negocio);
        atencion.setSucursal(sucursal);
        atencion.setProfesional(profesional);
        atencion.setCliente(cliente);
        atencion.setServicio(servicio);
        atencion.setInicio(inicio);
        atencion.setFin(fin);
        atencion.setEstado(EstadoAtencion.RESERVADA);
        atencion.setOrigen(OrigenAtencion.MANUAL);
        atencion.setPrecioFinal(servicio.getPrecio());
        atencion.setSobreturno(sobreturno);
        String notas = request.notas() == null ? "" : request.notas().trim();
        atencion.setNotas(notas.isEmpty() ? null : notas);

        try {
            // Si otro turno ganó ese horario justo en el medio, la regla de la base lo rechaza acá
            atenciones.saveAndFlush(atencion);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("excl_atencion_superpuesta")) {
                throw new ConflictoException("horario", MENSAJE_HORARIO_OCUPADO);
            }
            throw e;
        }

        notificaciones.crearConfirmacion(atencion);
        return TurnoAgendaResponse.de(atencion);
    }

    // ------------------------------------------------------------------ reglas

    /** Horarios libres de la grilla del profesional ese día (sin anticipación mínima: la da el negocio). */
    private List<LocalTime> libres(Negocio negocio, Profesional profesional, Sucursal sucursal, Servicio servicio, LocalDate fecha) {
        ZoneId zona = ZoneId.of(negocio.getZonaHoraria());
        List<HorarioLaboral> delDia = horarios.findByProfesionalIdAndSucursalIdAndDiaSemana(
                profesional.getId(), sucursal.getId(), fecha.getDayOfWeek());
        if (delDia.isEmpty()) {
            return List.of();
        }
        Instant inicioDia = ZonedDateTime.of(fecha, LocalTime.MIN, zona).toInstant();
        Instant finDia = ZonedDateTime.of(fecha.plusDays(1), LocalTime.MIN, zona).toInstant();
        return CalculadoraDisponibilidad.slotsLibresDeUnProfesional(
                delDia,
                ocupados(profesional, inicioDia, finDia),
                servicio.getDuracionMin(),
                negocio.getIntervaloSlotsMin(),
                fecha,
                zona,
                Instant.now(),
                0);
    }

    private void validarSobreturno(Profesional profesional, Sucursal sucursal, Servicio servicio,
                                   LocalDate fecha, LocalTime hora, Instant inicio, Instant fin) {
        if (inicio.isBefore(Instant.now().minus(TOLERANCIA_PASADO))) {
            throw new DatosInvalidosException("hora", "Ese horario ya pasó");
        }
        if (!bloqueos.findSolapados(profesional.getId(), inicio, fin).isEmpty()) {
            throw new ConflictoException("horario", "El barbero tiene bloqueado ese horario");
        }
        boolean pisaOtroTurno = !atenciones.findOcupadas(profesional.getId(), OCUPAN, inicio, fin).isEmpty();
        LocalTime horaFin = hora.plusMinutes(servicio.getDuracionMin());
        boolean dentroDelHorario = horaFin.isAfter(hora) // no cruza la medianoche
                && horarios.findByProfesionalIdAndSucursalIdAndDiaSemana(profesional.getId(), sucursal.getId(), fecha.getDayOfWeek())
                .stream()
                .anyMatch(h -> !hora.isBefore(h.getHoraInicio()) && !horaFin.isAfter(h.getHoraFin()));
        if (!pisaOtroTurno && dentroDelHorario) {
            throw new DatosInvalidosException("sobreturno",
                    "Ese horario está libre: cargalo como turno normal, sin marcar sobreturno");
        }
    }

    private List<Intervalo> ocupados(Profesional profesional, Instant desde, Instant hasta) {
        List<Intervalo> ocupados = new ArrayList<>();
        atenciones.findOcupadas(profesional.getId(), OCUPAN, desde, hasta)
                .forEach(a -> ocupados.add(new Intervalo(a.getInicio(), a.getFin())));
        bloqueos.findSolapados(profesional.getId(), desde, hasta)
                .forEach(b -> ocupados.add(new Intervalo(b.getInicio(), b.getFin())));
        return ocupados;
    }

    // ------------------------------------------------------------------ resolución de datos

    /** Admin y recepción: cualquier profesional del negocio. Un profesional: solo él mismo (403 si pide otro). */
    private Profesional resolverProfesional(Negocio negocio, UUID profesionalId) {
        usuarioActual.verificarAccesoAProfesional(profesionalId);
        return profesionales.findById(profesionalId)
                .filter(p -> p.getNegocio().getId().equals(negocio.getId()))
                .filter(Profesional::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Barbero no encontrado"));
    }

    /** Si el profesional trabaja en una sola sucursal, no hace falta elegirla. */
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
                .filter(Sucursal::isActiva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
        if (!profesional.getSucursales().contains(sucursal)) {
            throw new DatosInvalidosException("sucursalId", "Ese barbero no trabaja en esa sucursal");
        }
        return sucursal;
    }

    private Servicio resolverServicio(Negocio negocio, Profesional profesional, UUID servicioId) {
        Servicio servicio = servicios.findById(servicioId)
                .filter(Servicio::isActivo)
                .filter(s -> s.getNegocio().getId().equals(negocio.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
        if (!profesional.getServicios().contains(servicio)) {
            throw new DatosInvalidosException("servicioId", "Ese barbero no hace ese servicio");
        }
        return servicio;
    }

    private Negocio negocioActual() {
        return negocios.findById(usuarioActual.negocioId())
                .orElseThrow(() -> new IllegalStateException("El negocio del token no existe"));
    }
}

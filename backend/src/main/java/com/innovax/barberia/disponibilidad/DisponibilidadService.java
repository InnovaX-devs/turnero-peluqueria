package com.innovax.barberia.disponibilidad;

import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.profesional.Profesional;
import com.innovax.barberia.profesional.ProfesionalRepository;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.disponibilidad.dto.DiasDisponiblesResponse;
import com.innovax.barberia.disponibilidad.dto.DisponibilidadResponse;
import com.innovax.barberia.disponibilidad.dto.SlotDisponibleResponse;
import com.innovax.barberia.horario.BloqueoRepository;
import com.innovax.barberia.horario.HorarioLaboral;
import com.innovax.barberia.horario.HorarioLaboralRepository;
import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.servicio.ServicioRepository;
import com.innovax.barberia.sucursal.Sucursal;
import com.innovax.barberia.sucursal.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    /** Solo estas ocupan la agenda; canceladas y ausentes liberan el horario. */
    private static final Set<EstadoAtencion> ESTADOS_QUE_OCUPAN = EnumSet.of(EstadoAtencion.RESERVADA, EstadoAtencion.COMPLETADA);

    /** Máximo de días que se pueden pedir de una vez en /dias (el calendario pide de a una semana). */
    private static final int RANGO_MAXIMO_DIAS = 62;

    private final SucursalRepository sucursalRepository;
    private final ServicioRepository servicioRepository;
    private final ProfesionalRepository profesionalRepository;
    private final HorarioLaboralRepository horarioLaboralRepository;
    private final AtencionRepository atencionRepository;
    private final BloqueoRepository bloqueoRepository;

    @Transactional(readOnly = true)
    public DisponibilidadResponse consultar(UUID sucursalId, UUID servicioId, UUID profesionalId, LocalDate fecha) {
        Contexto ctx = cargarContexto(sucursalId, servicioId, profesionalId);
        return new DisponibilidadResponse(fecha, slotsDelDia(ctx, fecha));
    }

    @Transactional(readOnly = true)
    public DiasDisponiblesResponse consultarDias(UUID sucursalId, UUID servicioId, UUID profesionalId, LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new DatosInvalidosException("hasta", "No puede ser anterior a «desde»");
        }

        Contexto ctx = cargarContexto(sucursalId, servicioId, profesionalId);

        // Tope fijo solo para no hacer consultas enormes. Los días más allá de la anticipación
        // máxima de la barbería no fallan: vuelven sin disponibilidad (ver slotsDelDia).
        long diasPedidos = ChronoUnit.DAYS.between(desde, hasta) + 1;
        if (diasPedidos > RANGO_MAXIMO_DIAS) {
            throw new DatosInvalidosException("hasta", "El rango no puede superar los " + RANGO_MAXIMO_DIAS + " días");
        }

        List<LocalDate> diasConDisponibilidad = new ArrayList<>();
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            if (!slotsDelDia(ctx, fecha).isEmpty()) {
                diasConDisponibilidad.add(fecha);
            }
        }
        return new DiasDisponiblesResponse(diasConDisponibilidad);
    }

        /**
     * Chequeo puntual de un slot concreto (barbero + fecha + hora), usado al confirmar una
     * reserva justo antes de guardarla. Reutiliza la misma lógica que arma la grilla pública
     * para no mostrar un horario como libre y después rechazarlo por una razón distinta
     * (fuera de horario laboral, ya pasó la anticipación mínima, etc.).
     *
     * <p>Esto NO reemplaza la protección real contra doble reserva: es solo un chequeo rápido
     * para devolver un error claro en el caso común. La última palabra la tiene la restricción
     * de la base de datos, que se valida al guardar (ver AtencionService).
     */
    @Transactional(readOnly = true)
    public boolean estaDisponible(UUID sucursalId, UUID servicioId, UUID profesionalId, LocalDate fecha, LocalTime hora) {
        if (profesionalId == null) {
            return false;
        }
        Contexto ctx = cargarContexto(sucursalId, servicioId, profesionalId);
        if (ctx.profesionales().isEmpty()) {
            return false;
        }
        return slotsDelDia(ctx, fecha).stream()
                .anyMatch(slot -> slot.hora().equals(hora) && slot.profesionales().contains(profesionalId));
    }

    /**
     * Primer día (desde hoy) en que el barbero tiene al menos un horario libre para ese servicio,
     * dentro de la anticipación máxima. Corta en el primer día que encuentra, así que en el caso
     * común (hoy o mañana) hace muy pocas consultas.
     */
    @Transactional(readOnly = true)
    public Optional<LocalDate> proximoDiaDisponible(UUID sucursalId, UUID servicioId, UUID profesionalId) {
        Contexto ctx = cargarContexto(sucursalId, servicioId, profesionalId);
        if (ctx.profesionales().isEmpty()) {
            return Optional.empty();
        }
        LocalDate hoy = LocalDate.now(ctx.zonaHoraria());
        LocalDate limite = hoy.plusDays(ctx.negocio().getAnticipacionMaximaDias());
        for (LocalDate fecha = hoy; !fecha.isAfter(limite); fecha = fecha.plusDays(1)) {
            if (!slotsDelDia(ctx, fecha).isEmpty()) {
                return Optional.of(fecha);
            }
        }
        return Optional.empty();
    }

    private List<SlotDisponibleResponse> slotsDelDia(Contexto ctx, LocalDate fecha) {
        Instant ahora = Instant.now();

        // Más allá de la anticipación máxima no se ofrece nada (ej. hoy + 30 días)
        LocalDate hoy = ahora.atZone(ctx.zonaHoraria()).toLocalDate();
        if (fecha.isAfter(hoy.plusDays(ctx.negocio().getAnticipacionMaximaDias()))) {
            return List.of();
        }

        DayOfWeek diaSemana = fecha.getDayOfWeek();
        Instant inicioDia = ZonedDateTime.of(fecha, LocalTime.MIN, ctx.zonaHoraria()).toInstant();
        Instant finDia = ZonedDateTime.of(fecha.plusDays(1), LocalTime.MIN, ctx.zonaHoraria()).toInstant();

        Map<UUID, List<LocalTime>> slotsPorProfesional = new LinkedHashMap<>();

        for (Profesional profesional : ctx.profesionales()) {
            List<HorarioLaboral> horarios = horarioLaboralRepository
                    .findByProfesionalIdAndSucursalIdAndDiaSemana(profesional.getId(), ctx.sucursal().getId(), diaSemana);
            if (horarios.isEmpty()) {
                continue;
            }

            List<Intervalo> ocupados = new ArrayList<>();
            atencionRepository.findOcupadas(profesional.getId(), ESTADOS_QUE_OCUPAN, inicioDia, finDia)
                    .forEach(a -> ocupados.add(new Intervalo(a.getInicio(), a.getFin())));
            bloqueoRepository.findSolapados(profesional.getId(), inicioDia, finDia)
                    .forEach(b -> ocupados.add(new Intervalo(b.getInicio(), b.getFin())));

            List<LocalTime> libres = CalculadoraDisponibilidad.slotsLibresDeUnProfesional(
                    horarios,
                    ocupados,
                    ctx.servicio().getDuracionMin(),
                    ctx.negocio().getIntervaloSlotsMin(),
                    fecha,
                    ctx.zonaHoraria(),
                    ahora,
                    ctx.negocio().getAnticipacionMinimaMin());

            if (!libres.isEmpty()) {
                slotsPorProfesional.put(profesional.getId(), libres);
            }
        }

        return CalculadoraDisponibilidad.combinarPorProfesional(slotsPorProfesional);
    }

    private Contexto cargarContexto(UUID sucursalId, UUID servicioId, UUID profesionalId) {
        // Desactivados cuentan como inexistentes: no se pueden consultar ni reservar
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .filter(Sucursal::isActiva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
        Servicio servicio = servicioRepository.findById(servicioId)
                .filter(Servicio::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));

        if (!sucursal.getNegocio().isReservasOnline()) {
            throw new DatosInvalidosException("negocio",
                    sucursal.getNegocio().getRubro().negocio().esteCapitalizado() + " no acepta reservas online por ahora");
        }

        if (!servicio.getNegocio().getId().equals(sucursal.getNegocio().getId())) {
            throw new DatosInvalidosException("servicio",
                    "No pertenece " + sucursal.getNegocio().getRubro().negocio().al() + " de la sucursal");
        }

        List<Profesional> profesionales = profesionalId != null
                ? profesionalesParaUnoPuntual(profesionalId, sucursal, servicio)
                : profesionalRepository.findByActivoTrueAndSucursalesContainingAndServiciosContaining(sucursal, servicio);

        return new Contexto(sucursal, servicio, profesionales, sucursal.getNegocio(), ZoneId.of(sucursal.getNegocio().getZonaHoraria()));
    }

    /** Si piden un barbero puntual que no atiende ahí o no hace ese servicio, no hay error: simplemente no hay disponibilidad. */
    private List<Profesional> profesionalesParaUnoPuntual(UUID profesionalId, Sucursal sucursal, Servicio servicio) {
        Profesional profesional = profesionalRepository.findById(profesionalId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesional no encontrado"));

        boolean atiendeAca = profesional.isActivo()
                && profesional.getSucursales().contains(sucursal)
                && profesional.getServicios().contains(servicio);

        return atiendeAca ? List.of(profesional) : List.of();
    }

    private record Contexto(Sucursal sucursal, Servicio servicio, List<Profesional> profesionales, Negocio negocio, ZoneId zonaHoraria) {
    }
}
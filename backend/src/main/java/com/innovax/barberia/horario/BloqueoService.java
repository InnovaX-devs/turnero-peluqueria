package com.innovax.barberia.horario;

import com.innovax.barberia.agenda.dto.BloqueoAgendaResponse;
import com.innovax.barberia.agenda.dto.TurnoAgendaResponse;
import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.barbero.Barbero;
import com.innovax.barberia.barbero.BarberoRepository;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.horario.dto.BloqueoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bloqueos de horario (almuerzo, franco, etc.). Los bloqueos ya impactan en la disponibilidad
 * porque DisponibilidadService los suma a los horarios ocupados del barbero.
 *
 * <p>Permisos: un barbero gestiona solo sus bloqueos; un admin, los de cualquier barbero de su
 * barbería.
 */
@Service
@RequiredArgsConstructor
public class BloqueoService {

    private final UsuarioActual usuarioActual;
    private final BarberiaRepository barberias;
    private final BarberoRepository barberos;
    private final BloqueoRepository bloqueos;
    private final AtencionRepository atenciones;

    /**
     * Turnos RESERVADOS que quedarían tapados por este bloqueo. No guarda nada: el panel lo usa
     * para avisar antes de confirmar. Los turnos no se cancelan ni se mueven al bloquear.
     */
    @Transactional(readOnly = true)
    public List<TurnoAgendaResponse> turnosEnConflicto(BloqueoRequest request) {
        Barberia barberia = barberiaActual();
        List<Barbero> destino = resolverBarberos(barberia, request.barberoIds());
        Rango rango = calcularRango(barberia, request);
        return destino.stream()
                .flatMap(barbero -> atenciones
                        .findOcupadas(barbero.getId(), Set.of(EstadoAtencion.RESERVADA), rango.inicio(), rango.fin())
                        .stream())
                .sorted(Comparator.comparing(Atencion::getInicio))
                .map(TurnoAgendaResponse::de)
                .toList();
    }

    @Transactional
    public List<BloqueoAgendaResponse> crear(BloqueoRequest request) {
        Barberia barberia = barberiaActual();
        // Se valida todo antes de guardar: si un barbero o el horario no sirven, no se crea ningún bloqueo
        List<Barbero> destino = resolverBarberos(barberia, request.barberoIds());
        Rango rango = calcularRango(barberia, request);

        List<Bloqueo> nuevos = destino.stream().map(barbero -> {
            Bloqueo bloqueo = new Bloqueo();
            aplicar(bloqueo, barbero, rango, request.motivo());
            return bloqueo;
        }).toList();
        return bloqueos.saveAllAndFlush(nuevos).stream().map(BloqueoAgendaResponse::de).toList();
    }

    @Transactional
    public BloqueoAgendaResponse actualizar(UUID id, BloqueoRequest request) {
        Barberia barberia = barberiaActual();
        Bloqueo bloqueo = bloqueoPropio(barberia, id);
        List<UUID> pedidos = request.barberoIds() == null ? List.of() : request.barberoIds();
        if (pedidos.size() > 1) {
            throw new DatosInvalidosException("barberoIds", "Al editar un bloqueo se puede elegir un solo barbero");
        }
        UUID barberoId = pedidos.isEmpty() ? bloqueo.getBarbero().getId() : pedidos.get(0);
        Barbero barbero = resolverBarbero(barberia, barberoId);
        aplicar(bloqueo, barbero, calcularRango(barberia, request), request.motivo());
        return BloqueoAgendaResponse.de(bloqueos.saveAndFlush(bloqueo));
    }

    @Transactional
    public void eliminar(UUID id) {
        bloqueos.delete(bloqueoPropio(barberiaActual(), id));
    }

    private void aplicar(Bloqueo bloqueo, Barbero barbero, Rango rango, String motivoPedido) {
        bloqueo.setBarbero(barbero);
        bloqueo.setInicio(rango.inicio());
        bloqueo.setFin(rango.fin());
        String motivo = motivoPedido == null ? "" : motivoPedido.trim();
        bloqueo.setMotivo(motivo.isEmpty() ? null : motivo);
    }

    /** Sin horas: el día completo, de 00:00 a 00:00 del día siguiente en la zona de la barbería. */
    private Rango calcularRango(Barberia barberia, BloqueoRequest request) {
        ZoneId zona = ZoneId.of(barberia.getZonaHoraria());
        LocalDate fecha = request.fecha();
        LocalTime desde = request.horaDesde();
        LocalTime hasta = request.horaHasta();

        if (desde == null && hasta == null) {
            return new Rango(
                    fecha.atStartOfDay(zona).toInstant(),
                    fecha.plusDays(1).atStartOfDay(zona).toInstant());
        }
        if (desde == null || hasta == null) {
            throw new DatosInvalidosException("horaDesde",
                    "Indicá desde y hasta qué hora, o dejá las dos vacías para bloquear el día completo");
        }
        if (!hasta.isAfter(desde)) {
            throw new DatosInvalidosException("horaHasta", "La hora final tiene que ser posterior a la inicial");
        }
        return new Rango(
                ZonedDateTime.of(fecha, desde, zona).toInstant(),
                ZonedDateTime.of(fecha, hasta, zona).toInstant());
    }

    /** El bloqueo tiene que ser de un barbero de esta barbería (si no, 404) y accesible para el usuario (si no, 403). */
    private Bloqueo bloqueoPropio(Barberia barberia, UUID id) {
        Bloqueo bloqueo = bloqueos.findById(id)
                .filter(b -> b.getBarbero().getBarberia().getId().equals(barberia.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Bloqueo no encontrado"));
        usuarioActual.verificarAccesoABarbero(bloqueo.getBarbero().getId());
        return bloqueo;
    }

    /** Sin barberos indicados: el del usuario logueado. Repetidos se cuentan una sola vez. */
    private List<Barbero> resolverBarberos(Barberia barberia, List<UUID> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) {
            return List.of(resolverBarbero(barberia, null));
        }
        return pedidos.stream().distinct().map(id -> resolverBarbero(barberia, id)).toList();
    }

    /** Barbero logueado: siempre él mismo. Admin: el que elija (o él mismo si también corta). */
    private Barbero resolverBarbero(Barberia barberia, UUID barberoPedido) {
        UUID barberoId;
        if (!usuarioActual.esAdmin()) {
            barberoId = usuarioActual.barberoId()
                    .orElseThrow(() -> new AccessDeniedException("El usuario no está vinculado a un barbero"));
            if (barberoPedido != null) {
                usuarioActual.verificarAccesoABarbero(barberoPedido); // 403 si pide otro
            }
        } else if (barberoPedido != null) {
            barberoId = barberoPedido;
        } else {
            barberoId = usuarioActual.barberoId()
                    .orElseThrow(() -> new DatosInvalidosException("barberoId", "Elegí el barbero"));
        }
        return barberos.findById(barberoId)
                .filter(b -> b.getBarberia().getId().equals(barberia.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Barbero no encontrado"));
    }

    private Barberia barberiaActual() {
        return barberias.findById(usuarioActual.barberiaId())
                .orElseThrow(() -> new IllegalStateException("La barbería del token no existe"));
    }

    private record Rango(Instant inicio, Instant fin) {
    }
}
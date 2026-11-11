package com.innovax.barberia.horario;

import com.innovax.barberia.agenda.dto.BloqueoAgendaResponse;
import com.innovax.barberia.agenda.dto.TurnoAgendaResponse;
import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.profesional.Profesional;
import com.innovax.barberia.profesional.ProfesionalRepository;
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
    private final NegocioRepository negocios;
    private final ProfesionalRepository profesionales;
    private final BloqueoRepository bloqueos;
    private final AtencionRepository atenciones;

    /**
     * Turnos RESERVADOS que quedarían tapados por este bloqueo. No guarda nada: el panel lo usa
     * para avisar antes de confirmar. Los turnos no se cancelan ni se mueven al bloquear.
     */
    @Transactional(readOnly = true)
    public List<TurnoAgendaResponse> turnosEnConflicto(BloqueoRequest request) {
        Negocio negocio = negocioActual();
        List<Profesional> destino = resolverProfesionales(negocio, request.profesionalIds());
        Rango rango = calcularRango(negocio, request);
        return destino.stream()
                .flatMap(profesional -> atenciones
                        .findOcupadas(profesional.getId(), Set.of(EstadoAtencion.RESERVADA), rango.inicio(), rango.fin())
                        .stream())
                .sorted(Comparator.comparing(Atencion::getInicio))
                .map(TurnoAgendaResponse::de)
                .toList();
    }

    @Transactional
    public List<BloqueoAgendaResponse> crear(BloqueoRequest request) {
        Negocio negocio = negocioActual();
        // Se valida todo antes de guardar: si un barbero o el horario no sirven, no se crea ningún bloqueo
        List<Profesional> destino = resolverProfesionales(negocio, request.profesionalIds());
        Rango rango = calcularRango(negocio, request);

        List<Bloqueo> nuevos = destino.stream().map(profesional -> {
            Bloqueo bloqueo = new Bloqueo();
            aplicar(bloqueo, profesional, rango, request.motivo());
            return bloqueo;
        }).toList();
        return bloqueos.saveAllAndFlush(nuevos).stream().map(BloqueoAgendaResponse::de).toList();
    }

    @Transactional
    public BloqueoAgendaResponse actualizar(UUID id, BloqueoRequest request) {
        Negocio negocio = negocioActual();
        Bloqueo bloqueo = bloqueoPropio(negocio, id);
        List<UUID> pedidos = request.profesionalIds() == null ? List.of() : request.profesionalIds();
        if (pedidos.size() > 1) {
            throw new DatosInvalidosException("profesionalIds", "Al editar un bloqueo se puede elegir un solo profesional");
        }
        UUID profesionalId = pedidos.isEmpty() ? bloqueo.getProfesional().getId() : pedidos.get(0);
        Profesional profesional = resolverProfesional(negocio, profesionalId);
        aplicar(bloqueo, profesional, calcularRango(negocio, request), request.motivo());
        return BloqueoAgendaResponse.de(bloqueos.saveAndFlush(bloqueo));
    }

    @Transactional
    public void eliminar(UUID id) {
        bloqueos.delete(bloqueoPropio(negocioActual(), id));
    }

    private void aplicar(Bloqueo bloqueo, Profesional profesional, Rango rango, String motivoPedido) {
        bloqueo.setProfesional(profesional);
        bloqueo.setInicio(rango.inicio());
        bloqueo.setFin(rango.fin());
        String motivo = motivoPedido == null ? "" : motivoPedido.trim();
        bloqueo.setMotivo(motivo.isEmpty() ? null : motivo);
    }

    /** Sin horas: el día completo, de 00:00 a 00:00 del día siguiente en la zona de la barbería. */
    private Rango calcularRango(Negocio negocio, BloqueoRequest request) {
        ZoneId zona = ZoneId.of(negocio.getZonaHoraria());
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
    private Bloqueo bloqueoPropio(Negocio negocio, UUID id) {
        Bloqueo bloqueo = bloqueos.findById(id)
                .filter(b -> b.getProfesional().getNegocio().getId().equals(negocio.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Bloqueo no encontrado"));
        usuarioActual.verificarAccesoAProfesional(bloqueo.getProfesional().getId());
        return bloqueo;
    }

    /** Sin barberos indicados: el del usuario logueado. Repetidos se cuentan una sola vez. */
    private List<Profesional> resolverProfesionales(Negocio negocio, List<UUID> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) {
            return List.of(resolverProfesional(negocio, null));
        }
        return pedidos.stream().distinct().map(id -> resolverProfesional(negocio, id)).toList();
    }

    /** Profesional logueado: siempre él mismo. Admin o recepción: el que elija (o él mismo si también atiende). */
    private Profesional resolverProfesional(Negocio negocio, UUID profesionalPedido) {
        UUID profesionalId;
        if (!usuarioActual.veTodoElNegocio()) {
            profesionalId = usuarioActual.profesionalId()
                    .orElseThrow(() -> new AccessDeniedException("El usuario no está vinculado a un profesional"));
            if (profesionalPedido != null) {
                usuarioActual.verificarAccesoAProfesional(profesionalPedido); // 403 si pide otro
            }
        } else if (profesionalPedido != null) {
            profesionalId = profesionalPedido;
        } else {
            profesionalId = usuarioActual.profesionalId()
                    .orElseThrow(() -> new DatosInvalidosException("profesionalId", "Elegí el profesional"));
        }
        return profesionales.findById(profesionalId)
                .filter(b -> b.getNegocio().getId().equals(negocio.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesional no encontrado"));
    }

    private Negocio negocioActual() {
        return negocios.findById(usuarioActual.negocioId())
                .orElseThrow(() -> new IllegalStateException("El negocio del token no existe"));
    }

    private record Rango(Instant inicio, Instant fin) {
    }
}
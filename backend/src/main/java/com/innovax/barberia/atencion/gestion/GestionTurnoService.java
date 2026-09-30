package com.innovax.barberia.atencion.gestion;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.gestion.dto.ReprogramarTurnoRequest;
import com.innovax.barberia.atencion.gestion.dto.TurnoPublicoResponse;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.disponibilidad.DisponibilidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Cancelar o reprogramar un turno desde el link que recibe el cliente (issue #14).
 * Sin login: el token de gestión (UUID aleatorio) es la llave. Solo se puede tocar un
 * turno RESERVADA y con al menos {@code anticipacionCancelacionMin} de anticipación.
 */
@Service
@RequiredArgsConstructor
public class GestionTurnoService {

    private static final String MENSAJE_HORARIO_OCUPADO = "Ese horario ya no está disponible";

    private final GestionTurnoRepository turnos;
    private final AtencionRepository atenciones;
    private final DisponibilidadService disponibilidadService;

    @Transactional(readOnly = true)
    public TurnoPublicoResponse obtener(UUID token) {
        return TurnoPublicoResponse.de(buscar(token), Instant.now());
    }

    @Transactional
    public TurnoPublicoResponse cancelar(UUID token) {
        Atencion atencion = buscar(token);
        Instant ahora = Instant.now();
        validarModificable(atencion, ahora);

        atencion.setEstado(EstadoAtencion.CANCELADA);
        atencion.setEstadoCambiadoEn(ahora);
        atencion.setEstadoCambiadoPor(null); // lo canceló el cliente, no un usuario del panel
        atenciones.saveAndFlush(atencion);
        return TurnoPublicoResponse.de(atencion, ahora);
    }

    /**
     * Mueve el turno a otro día y hora con el mismo barbero y servicio. El token no cambia.
     *
     * <p>El turno actual ocupa su propio horario: si el cliente lo corre media hora, se
     * superpondría consigo mismo. Por eso primero se libera (se marca cancelado y se hace flush),
     * después se chequea la disponibilidad con el mismo criterio que la grilla pública, y recién
     * ahí se vuelve a reservar en el horario nuevo. Si algo falla, la transacción se revierte
     * entera y el turno queda como estaba.
     */
    @Transactional
    public TurnoPublicoResponse reprogramar(UUID token, ReprogramarTurnoRequest request) {
        Atencion atencion = buscar(token);
        Instant ahora = Instant.now();
        validarModificable(atencion, ahora);

        var barberia = atencion.getBarberia();
        if (!barberia.isReservasOnline()) {
            throw new DatosInvalidosException("barberia",
                    "La barbería no está tomando turnos online. Comunicate con ellos para cambiarlo.");
        }

        if (!atencion.getServicio().isActivo()) {
            throw new DatosInvalidosException("servicioId",
                    "Ese servicio ya no está disponible. Comunicate con la barbería para cambiar el turno.");
        }

        atencion.setEstado(EstadoAtencion.CANCELADA);
        atenciones.saveAndFlush(atencion);

        if (!disponibilidadService.estaDisponible(atencion.getSucursal().getId(), atencion.getServicio().getId(),
                atencion.getBarbero().getId(), request.fecha(), request.hora())) {
            throw new ConflictoException("horario", MENSAJE_HORARIO_OCUPADO);
        }

        ZoneId zona = ZoneId.of(barberia.getZonaHoraria());
        Instant inicio = ZonedDateTime.of(request.fecha(), request.hora(), zona).toInstant();
        atencion.setInicio(inicio);
        atencion.setFin(inicio.plus(Duration.ofMinutes(atencion.getServicio().getDuracionMin())));
        atencion.setEstado(EstadoAtencion.RESERVADA);
        try {
            // Si otra reserva ganó ese horario justo en el medio, la EXCLUDE constraint lo rechaza acá
            atenciones.saveAndFlush(atencion);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("excl_atencion_superpuesta")) {
                throw new ConflictoException("horario", MENSAJE_HORARIO_OCUPADO);
            }
            throw e;
        }
        return TurnoPublicoResponse.de(atencion, ahora);
    }

    private Atencion buscar(UUID token) {
        return turnos.findByTokenGestion(token)
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos ese turno"));
    }

    private static void validarModificable(Atencion atencion, Instant ahora) {
        if (atencion.getEstado() != EstadoAtencion.RESERVADA) {
            throw new ConflictoException("estado", atencion.getEstado() == EstadoAtencion.CANCELADA
                    ? "Este turno ya estaba cancelado"
                    : "Este turno ya pasó");
        }
        int minutos = atencion.getBarberia().getAnticipacionCancelacionMin();
        if (!ahora.isBefore(atencion.getInicio().minusSeconds(minutos * 60L))) {
            throw new ConflictoException("turno", "Los cambios online se aceptan hasta " + anticipacion(minutos)
                    + " antes del turno. Comunicate con la barbería.");
        }
    }

    /** 120 → "2 horas", 90 → "1 h 30 min", 30 → "30 minutos". */
    static String anticipacion(int minutos) {
        int horas = minutos / 60;
        int resto = minutos % 60;
        if (horas == 0) {
            return resto + (resto == 1 ? " minuto" : " minutos");
        }
        if (resto == 0) {
            return horas + (horas == 1 ? " hora" : " horas");
        }
        return horas + " h " + resto + " min";
    }
}

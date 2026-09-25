package com.innovax.barberia.agenda.dto;

import com.innovax.barberia.horario.Bloqueo;

import java.time.Instant;
import java.util.UUID;

public record BloqueoAgendaResponse(
        UUID id,
        Instant inicio,
        Instant fin,
        String motivo,
        UUID barberoId,
        String barberoNombre
) {

    public static BloqueoAgendaResponse de(Bloqueo b) {
        return new BloqueoAgendaResponse(
                b.getId(),
                b.getInicio(),
                b.getFin(),
                b.getMotivo(),
                b.getBarbero().getId(),
                b.getBarbero().getNombre() + " " + b.getBarbero().getApellido());
    }
}
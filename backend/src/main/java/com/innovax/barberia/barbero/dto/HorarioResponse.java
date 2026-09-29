package com.innovax.barberia.barbero.dto;

import com.innovax.barberia.horario.HorarioLaboral;

import java.time.LocalTime;
import java.util.UUID;

public record HorarioResponse(UUID sucursalId, int diaSemana, LocalTime horaInicio, LocalTime horaFin) {

    public static HorarioResponse de(HorarioLaboral h) {
        return new HorarioResponse(h.getSucursal().getId(), h.getDiaSemana().getValue(),
                h.getHoraInicio(), h.getHoraFin());
    }
}
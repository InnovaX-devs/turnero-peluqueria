package com.innovax.barberia.horario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Bloqueo de un barbero en un día. Fecha y horas en la zona horaria de la barbería.
 * Con horaDesde y horaHasta es un rango horario (ej. almuerzo); sin ninguna de las dos,
 * bloquea el día completo (ej. franco).
 */
public record BloqueoRequest(

        /** null = el barbero logueado (al editar, null = no cambiar de barbero) */
        UUID barberoId,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        LocalTime horaDesde,

        LocalTime horaHasta,

        @Size(max = 200, message = "El motivo puede tener hasta 200 caracteres")
        String motivo
) {
}
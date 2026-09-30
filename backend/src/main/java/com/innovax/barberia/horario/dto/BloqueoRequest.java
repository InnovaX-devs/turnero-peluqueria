package com.innovax.barberia.horario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Bloqueo de uno o varios barberos en un día. Fecha y horas en la zona horaria de la barbería.
 * Con horaDesde y horaHasta es un rango horario (ej. almuerzo); sin ninguna de las dos,
 * bloquea el día completo (ej. franco).
 */
public record BloqueoRequest(

        /**
         * Barberos a bloquear (se crea un bloqueo por cada uno). null o vacío = el barbero logueado.
         * Al editar un bloqueo: máximo uno (null o vacío = no cambiar de barbero).
         */
        List<UUID> barberoIds,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        LocalTime horaDesde,

        LocalTime horaHasta,

        @Size(max = 200, message = "El motivo puede tener hasta 200 caracteres")
        String motivo
) {
}
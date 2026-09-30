package com.innovax.barberia.atencion.gestion.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Nuevo día y hora, en la hora local de la barbería (igual que al reservar). */
public record ReprogramarTurnoRequest(
        @NotNull(message = "Elegí el día") LocalDate fecha,
        @NotNull(message = "Elegí el horario") LocalTime hora) {
}

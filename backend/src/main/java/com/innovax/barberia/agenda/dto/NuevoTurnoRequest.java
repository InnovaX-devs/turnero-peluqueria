package com.innovax.barberia.agenda.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Turno dado desde el panel (por teléfono, en el mostrador). Fecha y hora en la zona del negocio.
 * {@code sobreturno} solo lo pueden pedir admin y recepción.
 */
public record NuevoTurnoRequest(
        @NotNull(message = "Elegí el cliente")
        UUID clienteId,

        @NotNull(message = "Elegí el servicio")
        UUID servicioId,

        @NotNull(message = "Elegí el barbero")
        UUID profesionalId,

        /** null = la única sucursal del profesional */
        UUID sucursalId,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "La hora es obligatoria")
        LocalTime hora,

        @Size(max = 2000, message = "Las notas pueden tener hasta 2000 caracteres")
        String notas,

        /** true = encima de otro turno o fuera del horario del profesional. null = false. */
        Boolean sobreturno
) {
}

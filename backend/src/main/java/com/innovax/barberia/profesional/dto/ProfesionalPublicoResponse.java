package com.innovax.barberia.profesional.dto;

import com.innovax.barberia.profesional.Profesional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Barbero tal como se muestra en la página pública.
 * {@code proximoDiaDisponible} es null si no tiene horarios libres dentro de la anticipación máxima.
 */
public record ProfesionalPublicoResponse(
        UUID id,
        String nombre,
        String apellido,
        String fotoUrl,
        LocalDate proximoDiaDisponible) {

    public static ProfesionalPublicoResponse de(Profesional b, LocalDate proximoDiaDisponible) {
        return new ProfesionalPublicoResponse(b.getId(), b.getNombre(), b.getApellido(), b.getFotoUrl(), proximoDiaDisponible);
    }
}

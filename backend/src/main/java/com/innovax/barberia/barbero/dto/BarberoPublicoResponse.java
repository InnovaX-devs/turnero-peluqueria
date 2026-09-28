package com.innovax.barberia.barbero.dto;

import com.innovax.barberia.barbero.Barbero;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Barbero tal como se muestra en la página pública.
 * {@code proximoDiaDisponible} es null si no tiene horarios libres dentro de la anticipación máxima.
 */
public record BarberoPublicoResponse(
        UUID id,
        String nombre,
        String apellido,
        String fotoUrl,
        LocalDate proximoDiaDisponible) {

    public static BarberoPublicoResponse de(Barbero b, LocalDate proximoDiaDisponible) {
        return new BarberoPublicoResponse(b.getId(), b.getNombre(), b.getApellido(), b.getFotoUrl(), proximoDiaDisponible);
    }
}

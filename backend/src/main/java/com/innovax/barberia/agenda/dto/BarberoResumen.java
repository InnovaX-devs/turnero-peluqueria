package com.innovax.barberia.agenda.dto;

import com.innovax.barberia.barbero.Barbero;

import java.util.UUID;

/** Lo mínimo para poblar el selector "ver por barbero" de la agenda. */
public record BarberoResumen(
        UUID id,
        String nombre
) {

    public static BarberoResumen de(Barbero b) {
        return new BarberoResumen(b.getId(), b.getNombre() + " " + b.getApellido());
    }
}
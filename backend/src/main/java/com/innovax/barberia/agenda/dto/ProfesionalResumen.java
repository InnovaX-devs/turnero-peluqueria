package com.innovax.barberia.agenda.dto;

import com.innovax.barberia.profesional.Profesional;

import java.util.UUID;

/** Lo mínimo para poblar el selector "ver por barbero" de la agenda. */
public record ProfesionalResumen(
        UUID id,
        String nombre
) {

    public static ProfesionalResumen de(Profesional b) {
        return new ProfesionalResumen(b.getId(), b.getNombre() + " " + b.getApellido());
    }
}
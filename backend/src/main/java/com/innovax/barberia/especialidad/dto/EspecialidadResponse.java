package com.innovax.barberia.especialidad.dto;

import com.innovax.barberia.especialidad.Especialidad;

import java.util.UUID;

/** Se usa tanto en el panel como en la página pública: solo id y nombre. */
public record EspecialidadResponse(UUID id, String nombre) {

    public static EspecialidadResponse de(Especialidad e) {
        return new EspecialidadResponse(e.getId(), e.getNombre());
    }
}

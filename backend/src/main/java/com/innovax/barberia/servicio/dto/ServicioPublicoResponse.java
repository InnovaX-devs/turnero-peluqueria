package com.innovax.barberia.servicio.dto;

import com.innovax.barberia.servicio.Servicio;

import java.util.UUID;

/** Servicio tal como se muestra en la página pública. {@code precio} en centavos. */
public record ServicioPublicoResponse(UUID id, String nombre, int duracionMin, long precio) {

    public static ServicioPublicoResponse de(Servicio s) {
        return new ServicioPublicoResponse(s.getId(), s.getNombre(), s.getDuracionMin(), s.getPrecio());
    }
}

package com.innovax.barberia.servicio.dto;

import com.innovax.barberia.servicio.Servicio;

import java.util.UUID;

/** Servicio para el panel de admin. {@code precio} en centavos. */
public record ServicioResponse(UUID id, String nombre, int duracionMin, long precio, boolean activo, int orden) {

    public static ServicioResponse de(Servicio s) {
        return new ServicioResponse(s.getId(), s.getNombre(), s.getDuracionMin(), s.getPrecio(),
                s.isActivo(), s.getOrden());
    }
}
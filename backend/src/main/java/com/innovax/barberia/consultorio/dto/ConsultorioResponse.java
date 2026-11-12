package com.innovax.barberia.consultorio.dto;

import com.innovax.barberia.consultorio.Consultorio;

import java.util.UUID;

public record ConsultorioResponse(UUID id, UUID sucursalId, String nombre, boolean activo) {

    public static ConsultorioResponse de(Consultorio c) {
        return new ConsultorioResponse(c.getId(), c.getSucursal().getId(), c.getNombre(), c.isActivo());
    }
}

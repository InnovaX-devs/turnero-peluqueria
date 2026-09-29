package com.innovax.barberia.barbero.dto;

import com.innovax.barberia.barbero.Barbero;
import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.sucursal.Sucursal;

import java.util.List;
import java.util.UUID;

/** Barbero para el panel admin. {@code usuarioEmail} es null si no tiene login. */
public record BarberoResponse(
        UUID id,
        String nombre,
        String apellido,
        String fotoUrl,
        boolean activo,
        List<UUID> sucursalIds,
        List<UUID> servicioIds,
        String usuarioEmail) {

    /** Accede a colecciones lazy: llamarlo siempre dentro de una transacción. */
    public static BarberoResponse de(Barbero b) {
        return new BarberoResponse(
                b.getId(),
                b.getNombre(),
                b.getApellido(),
                b.getFotoUrl(),
                b.isActivo(),
                b.getSucursales().stream().map(Sucursal::getId).toList(),
                b.getServicios().stream().map(Servicio::getId).toList(),
                b.getUsuario() == null ? null : b.getUsuario().getEmail());
    }
}
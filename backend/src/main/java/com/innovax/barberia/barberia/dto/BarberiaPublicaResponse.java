package com.innovax.barberia.barberia.dto;

import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.sucursal.Sucursal;

import java.util.List;
import java.util.UUID;

/** Solo lo que se puede mostrar en público: nada de configuración interna. */
public record BarberiaPublicaResponse(
        String nombre,
        String slug,
        String logoUrl,
        boolean reservasOnline,
        List<SucursalPublica> sucursales
) {

    public record SucursalPublica(UUID id, String nombre, String direccion, String ciudad, String telefono) {
        static SucursalPublica de(Sucursal s) {
            return new SucursalPublica(s.getId(), s.getNombre(), s.getDireccion(), s.getCiudad(), s.getTelefono());
        }
    }

    public static BarberiaPublicaResponse de(Barberia b, List<Sucursal> sucursales) {
        return new BarberiaPublicaResponse(b.getNombre(), b.getSlug(), b.getLogoUrl(), b.isReservasOnline(),
                sucursales.stream().map(SucursalPublica::de).toList());
    }
}
package com.innovax.barberia.negocio.dto;

import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.sucursal.Sucursal;

import java.util.List;
import java.util.UUID;

/** Solo lo que se puede mostrar en público: nada de configuración interna. */
public record NegocioPublicoResponse(
        String nombre,
        String slug,
        String logoUrl,
        boolean reservasOnline,
        /** Hasta cuántos días adelante se puede reservar: el calendario no deja avanzar más. */
        int anticipacionMaximaDias,
        List<SucursalPublica> sucursales
) {

    public record SucursalPublica(UUID id, String nombre, String direccion, String ciudad, String telefono) {
        static SucursalPublica de(Sucursal s) {
            return new SucursalPublica(s.getId(), s.getNombre(), s.getDireccion(), s.getCiudad(), s.getTelefono());
        }
    }

    public static NegocioPublicoResponse de(Negocio b, List<Sucursal> sucursales) {
        return new NegocioPublicoResponse(b.getNombre(), b.getSlug(), b.getLogoUrl(), b.isReservasOnline(), b.getAnticipacionMaximaDias(),
                sucursales.stream().map(SucursalPublica::de).toList());
    }
}
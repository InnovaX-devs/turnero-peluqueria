package com.innovax.barberia.atencion.dto;

import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.barbero.Barbero;
import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.sucursal.Sucursal;

import java.util.List;
import java.util.UUID;

/** Todo lo que necesita el formulario de "Registrar corte", en una sola llamada. */
public record OpcionesRegistroResponse(
        String zonaHoraria,
        /** null si el usuario no está vinculado a un barbero */
        UUID barberoPropioId,
        List<ServicioOpcion> servicios,
        List<BarberoOpcion> barberos,
        List<SucursalOpcion> sucursales
) {

    public record ServicioOpcion(UUID id, String nombre, int duracionMin, long precio) {
    }

    public record BarberoOpcion(UUID id, String nombre, List<UUID> sucursalIds) {
    }

    public record SucursalOpcion(UUID id, String nombre) {
    }

    public static OpcionesRegistroResponse de(Barberia barberia, UUID barberoPropioId, List<Servicio> servicios,
                                              List<Barbero> barberos, List<Sucursal> sucursales) {
        return new OpcionesRegistroResponse(
                barberia.getZonaHoraria(),
                barberoPropioId,
                servicios.stream()
                        .map(s -> new ServicioOpcion(s.getId(), s.getNombre(), s.getDuracionMin(), s.getPrecio()))
                        .toList(),
                barberos.stream()
                        .map(b -> new BarberoOpcion(b.getId(), b.getNombre() + " " + b.getApellido(),
                                b.getSucursales().stream().filter(Sucursal::isActiva).map(Sucursal::getId).toList()))
                        .toList(),
                sucursales.stream().map(s -> new SucursalOpcion(s.getId(), s.getNombre())).toList());
    }
}
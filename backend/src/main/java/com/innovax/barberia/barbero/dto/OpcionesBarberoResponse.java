package com.innovax.barberia.barbero.dto;

import java.util.List;
import java.util.UUID;

public record OpcionesBarberoResponse(List<SucursalOpcion> sucursales, List<ServicioOpcion> servicios) {

    public record SucursalOpcion(UUID id, String nombre, String direccion) {
    }

    public record ServicioOpcion(UUID id, String nombre, int duracionMin) {
    }
}
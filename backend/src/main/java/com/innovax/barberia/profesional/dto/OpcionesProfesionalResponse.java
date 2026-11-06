package com.innovax.barberia.profesional.dto;

import java.util.List;
import java.util.UUID;

public record OpcionesProfesionalResponse(List<SucursalOpcion> sucursales, List<ServicioOpcion> servicios) {

    public record SucursalOpcion(UUID id, String nombre, String direccion) {
    }

    public record ServicioOpcion(UUID id, String nombre, int duracionMin) {
    }
}
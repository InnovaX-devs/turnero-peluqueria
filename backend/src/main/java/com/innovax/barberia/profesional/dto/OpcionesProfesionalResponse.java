package com.innovax.barberia.profesional.dto;

import com.innovax.barberia.especialidad.dto.EspecialidadResponse;

import java.util.List;
import java.util.UUID;

/** Lo que se puede asignar a un profesional. {@code especialidades} solo tiene sentido en el rubro salud. */
public record OpcionesProfesionalResponse(
        List<SucursalOpcion> sucursales,
        List<ServicioOpcion> servicios,
        List<EspecialidadResponse> especialidades) {

    public record SucursalOpcion(UUID id, String nombre, String direccion) {
    }

    public record ServicioOpcion(UUID id, String nombre, int duracionMin) {
    }
}
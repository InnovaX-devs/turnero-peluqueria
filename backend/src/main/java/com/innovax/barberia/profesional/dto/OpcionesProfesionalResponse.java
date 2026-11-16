package com.innovax.barberia.profesional.dto;

import com.innovax.barberia.cobertura.dto.CoberturaResponse;
import com.innovax.barberia.especialidad.dto.EspecialidadResponse;

import java.util.List;
import java.util.UUID;

/**
 * Lo que se puede asignar a un profesional. {@code especialidades} y {@code consultorios} (activos, de
 * sucursales activas; se asignan a los tramos del horario) solo tienen sentido en el rubro salud.
 */
public record OpcionesProfesionalResponse(
        List<SucursalOpcion> sucursales,
        List<ServicioOpcion> servicios,
        List<EspecialidadResponse> especialidades,
        List<ConsultorioOpcion> consultorios,
        /** Obras sociales / prepagas activas del negocio (issue #67, rubro salud). */
        List<CoberturaResponse> coberturas) {

    public record SucursalOpcion(UUID id, String nombre, String direccion) {
    }

    public record ServicioOpcion(UUID id, String nombre, int duracionMin) {
    }

    public record ConsultorioOpcion(UUID id, UUID sucursalId, String nombre) {
    }
}
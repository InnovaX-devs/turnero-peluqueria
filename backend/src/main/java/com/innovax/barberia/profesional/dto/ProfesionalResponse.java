package com.innovax.barberia.profesional.dto;

import com.innovax.barberia.especialidad.Especialidad;
import com.innovax.barberia.profesional.Profesional;
import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.sucursal.Sucursal;

import java.util.List;
import java.util.UUID;

/** Barbero para el panel admin. {@code usuarioEmail} es null si no tiene login. */
public record ProfesionalResponse(
        UUID id,
        String nombre,
        String apellido,
        String fotoUrl,
        String matricula,
        boolean activo,
        List<UUID> sucursalIds,
        List<UUID> servicioIds,
        List<UUID> especialidadIds,
        String usuarioEmail) {

    /** Accede a colecciones lazy: llamarlo siempre dentro de una transacción. */
    public static ProfesionalResponse de(Profesional b) {
        return new ProfesionalResponse(
                b.getId(),
                b.getNombre(),
                b.getApellido(),
                b.getFotoUrl(),
                b.getMatricula(),
                b.isActivo(),
                b.getSucursales().stream().map(Sucursal::getId).toList(),
                b.getServicios().stream().filter(Servicio::isActivo).map(Servicio::getId).toList(),
                b.getEspecialidades().stream().map(Especialidad::getId).toList(),
                b.getUsuario() == null ? null : b.getUsuario().getEmail());
    }
}
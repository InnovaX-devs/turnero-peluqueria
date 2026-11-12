package com.innovax.barberia.consultorio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Requests del ABM de consultorios (solo admin). */
public final class ConsultorioRequests {

    private ConsultorioRequests() {
    }

    public record Crear(
            @NotNull(message = "Elegí la sucursal")
            UUID sucursalId,

            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 80, message = "El nombre puede tener hasta 80 caracteres")
            String nombre) {
    }

    public record Renombrar(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 80, message = "El nombre puede tener hasta 80 caracteres")
            String nombre) {
    }

    public record CambiarActivo(
            @NotNull(message = "Indicá si el consultorio queda activo")
            Boolean activo) {
    }
}

package com.innovax.barberia.servicio.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Requests del ABM de servicios (solo admin). {@code precio} en centavos. */
public final class ServicioRequests {

    private ServicioRequests() {
    }

    public record Crear(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 120, message = "El nombre puede tener hasta 120 caracteres")
            String nombre,

            @NotNull(message = "La duración es obligatoria")
            @Min(value = 1, message = "La duración tiene que ser de al menos 1 minuto")
            @Max(value = 480, message = "La duración puede ser de hasta 480 minutos")
            Integer duracionMin,

            @NotNull(message = "El precio es obligatorio")
            @Min(value = 0, message = "El precio no puede ser negativo")
            @Max(value = 1_000_000_000L, message = "El precio es demasiado alto")
            Long precio) {
    }

    public record Actualizar(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 120, message = "El nombre puede tener hasta 120 caracteres")
            String nombre,

            @NotNull(message = "La duración es obligatoria")
            @Min(value = 1, message = "La duración tiene que ser de al menos 1 minuto")
            @Max(value = 480, message = "La duración puede ser de hasta 480 minutos")
            Integer duracionMin,

            @NotNull(message = "El precio es obligatorio")
            @Min(value = 0, message = "El precio no puede ser negativo")
            @Max(value = 1_000_000_000L, message = "El precio es demasiado alto")
            Long precio) {
    }

    public record CambiarActivo(
            @NotNull(message = "Indicá si el servicio queda activo")
            Boolean activo) {
    }

    /** Todos los servicios ACTIVOS, en el orden en que se van a mostrar en la página pública. */
    public record Reordenar(
            @NotNull(message = "Indicá el orden")
            @Size(max = 200, message = "Demasiados servicios")
            List<@NotNull(message = "Id inválido") UUID> ids) {
    }
}
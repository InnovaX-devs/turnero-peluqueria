package com.innovax.barberia.cobertura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CoberturaRequests {

    private CoberturaRequests() {
    }

    /** Alta o renombre de una cobertura (solo admin). */
    public record Guardar(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 80, message = "El nombre puede tener hasta 80 caracteres")
            String nombre) {
    }

    /** Activar / desactivar (baja lógica). */
    public record CambiarActiva(@NotNull(message = "Indicá si está activa") Boolean activa) {
    }
}

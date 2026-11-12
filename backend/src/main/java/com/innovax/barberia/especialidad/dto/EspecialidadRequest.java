package com.innovax.barberia.especialidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta o renombre de una especialidad (solo admin). */
public record EspecialidadRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre puede tener hasta 80 caracteres")
        String nombre) {
}

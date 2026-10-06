package com.innovax.barberia.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** "Mi cuenta": el usuario logueado cambia su propia contraseña. Pide la actual para confirmar que es él. */
public record CambiarPasswordRequest(
        @NotBlank(message = "Ingresá tu contraseña actual")
        String actual,
        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña tiene que tener entre 8 y 72 caracteres")
        String nueva) {
}

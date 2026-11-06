package com.innovax.barberia.recepcion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Requests del ABM de usuarios de recepción (solo admin). */
public final class RecepcionRequests {

    private RecepcionRequests() {
    }

    /** Solo el email: la contraseña la elige la persona desde el link de invitación. */
    public record Crear(
            @NotBlank(message = "El email es obligatorio")
            @Email(message = "El email no es válido")
            @Size(max = 255, message = "El email puede tener hasta 255 caracteres")
            String email) {
    }

    public record CambiarActivo(
            @NotNull(message = "Indicá si el usuario queda activo")
            Boolean activo) {
    }
}

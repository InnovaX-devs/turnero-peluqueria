package com.innovax.barberia.atencion.dto;

import jakarta.validation.constraints.NotNull;

/** Body para marcar (true) o desmarcar (false) que el cliente llegó y está en la sala de espera. */
public record MarcarLlegadaRequest(
        @NotNull(message = "Indicá si el cliente llegó") Boolean llego
) {
}

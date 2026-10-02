package com.innovax.barberia.cliente.dto;

import jakarta.validation.constraints.Size;

/** Una nota en blanco (o null) borra la nota del cliente. */
public record ActualizarNotasRequest(

        @Size(max = 2000, message = "Las notas pueden tener hasta 2000 caracteres")
        String notas
) {
}
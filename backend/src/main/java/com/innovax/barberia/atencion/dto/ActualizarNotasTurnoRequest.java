package com.innovax.barberia.atencion.dto;

import jakarta.validation.constraints.Size;

/** Nota de consulta de un turno (issue #72). Una nota en blanco (o null) la borra. */
public record ActualizarNotasTurnoRequest(

        @Size(max = 2000, message = "Las notas pueden tener hasta 2000 caracteres")
        String notas
) {
}

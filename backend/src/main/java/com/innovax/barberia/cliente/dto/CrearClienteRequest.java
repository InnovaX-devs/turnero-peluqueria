package com.innovax.barberia.cliente.dto;

import com.innovax.barberia.cliente.Sexo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CrearClienteRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre puede tener hasta 120 caracteres")
        String nombre,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$",
                message = "Teléfono inválido: poné el código de área, ej: 358 4111111")
        String telefono,

        /** Obligatorio en salud (7 u 8 números; se aceptan puntos). En los otros rubros se ignora. */
        @Size(max = 15, message = "El DNI tiene que tener 7 u 8 números, sin puntos")
        String dni,

        /** Solo salud. */
        @Past(message = "La fecha de nacimiento tiene que ser anterior a hoy")
        LocalDate fechaNacimiento,

        /** Solo salud. */
        Sexo sexo
) {
}
package com.innovax.barberia.atencion.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.innovax.barberia.cliente.Sexo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Lo que manda la página pública de reserva. El slot (fecha + hora + barbero) tiene que
 * venir de lo que devolvió /api/public/disponibilidad: acá se vuelve a validar, no se confía
 * en que siga libre.
 */
public record CrearReservaRequest(

        @NotNull(message = "La sucursal es obligatoria")
        UUID sucursalId,

        @NotNull(message = "El servicio es obligatorio")
        UUID servicioId,

        // "barberoId": nombre del campo antes del issue #64 (páginas de reserva viejas abiertas)
        @NotNull(message = "El profesional es obligatorio")
        @JsonAlias("barberoId")
        UUID profesionalId,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "La hora es obligatoria")
        LocalTime hora,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre puede tener hasta 120 caracteres")
        String clienteNombre,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$",
                message = "Formato inválido: tiene que incluir el código de país, ej: +5493584111111")
        String clienteTelefono,

        /** Obligatorio en salud (7 u 8 números; se aceptan puntos): ahí el paciente se identifica por DNI. */
        @Size(max = 15, message = "El DNI tiene que tener 7 u 8 números, sin puntos")
        String clienteDni,

        /** Solo salud, opcional. */
        @Past(message = "La fecha de nacimiento tiene que ser anterior a hoy")
        LocalDate clienteFechaNacimiento,

        /** Solo salud, opcional. */
        Sexo clienteSexo,

        @Size(max = 2000, message = "Las notas pueden tener hasta 2000 caracteres")
        String notas
) {
}
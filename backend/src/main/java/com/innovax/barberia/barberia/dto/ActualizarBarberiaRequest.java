package com.innovax.barberia.barberia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Configuración editable de la barbería. Se manda completa (PUT).
 * Los flags y números son objetos (Boolean/Integer) para que un campo faltante dé error
 * y no se interprete como "false" o "0" sin querer.
 */
public record ActualizarBarberiaRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre puede tener hasta 120 caracteres")
        String nombre,

        @NotBlank(message = "El slug es obligatorio")
        @Size(min = 3, max = 80, message = "El slug tiene que tener entre 3 y 80 caracteres")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "Solo minúsculas, números y guiones (sin espacios ni tildes), ej: mi-barberia")
        String slug,

        /** Opcional: null = sin logo. */
        @Size(max = 500, message = "La URL del logo puede tener hasta 500 caracteres")
        @Pattern(regexp = "^https?://\\S+$", message = "La URL del logo tiene que empezar con http:// o https://")
        String logoUrl,

        @NotNull(message = "Indicá si se permiten reservas online")
        Boolean reservasOnline,

        @NotNull(message = "La anticipación mínima es obligatoria")
        @Min(value = 0, message = "La anticipación mínima no puede ser negativa")
        @Max(value = 10080, message = "La anticipación mínima puede ser de hasta 7 días")
        Integer anticipacionMinimaMin,

        @NotNull(message = "La anticipación máxima es obligatoria")
        @Min(value = 1, message = "La anticipación máxima tiene que ser de al menos 1 día")
        @Max(value = 365, message = "La anticipación máxima puede ser de hasta 365 días")
        Integer anticipacionMaximaDias,

        /** Solo 15 o 30 (se valida en el service). */
        @NotNull(message = "El intervalo de horarios es obligatorio")
        Integer intervaloSlotsMin,

        /** Opcional: si no viene, se mantiene el valor actual. */
        @Min(value = 0, message = "La anticipación para cancelar no puede ser negativa")
        @Max(value = 10080, message = "La anticipación para cancelar puede ser de hasta 7 días")
        Integer anticipacionCancelacionMin
) {
}
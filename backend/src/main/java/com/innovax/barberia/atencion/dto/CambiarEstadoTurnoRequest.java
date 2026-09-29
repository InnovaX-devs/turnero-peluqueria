package com.innovax.barberia.atencion.dto;

import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.MetodoPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Body para cambiar el estado de un turno desde el detalle del panel.
 *
 * <p>{@code precioFinal} y {@code metodoPago} solo se usan cuando {@code estado} es
 * {@code COMPLETADA} (permiten ajustar el precio final -por si se agregó algo o se hizo un
 * descuento- y registrar cómo pagó). Para CANCELADA/AUSENTE se ignoran.
 */
public record CambiarEstadoTurnoRequest(
        @NotNull(message = "El estado es obligatorio") EstadoAtencion estado,
        /** En centavos. Null = no se ajusta, se mantiene el precio actual del turno. */
        @PositiveOrZero(message = "No puede ser negativo") Long precioFinal,
        MetodoPago metodoPago
) {
}
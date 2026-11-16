package com.innovax.barberia.atencion.dto;

import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.MetodoPago;
import com.innovax.barberia.cobertura.TipoPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Body para cambiar el estado de un turno desde el detalle del panel.
 *
 * <p>{@code precioFinal} y {@code metodoPago} solo se usan cuando {@code estado} es
 * {@code COMPLETADA} (permiten ajustar el precio final -por si se agregó algo o se hizo un
 * descuento- y registrar cómo pagó). Para CANCELADA/AUSENTE se ignoran.
 *
 * <p>Issue #67 (salud): {@code tipoPago} dice cómo se paga. Null = se mantiene el del turno (PARTICULAR si
 * se reservó sin obra social).
 * <ul>
 *   <li>PARTICULAR: {@code metodoPago} obligatorio, como siempre.</li>
 *   <li>COBERTURA: no se cobra nada; {@code metodoPago} y {@code copago} se ignoran.</li>
 *   <li>COBERTURA_CON_COPAGO: {@code copago} (centavos) y {@code metodoPago} obligatorios.</li>
 * </ul>
 * Con cobertura se usa la del turno o, si no tiene, la del paciente. {@code numeroAutorizacion} es opcional.
 */
public record CambiarEstadoTurnoRequest(
        @NotNull(message = "El estado es obligatorio") EstadoAtencion estado,
        /** En centavos. Null = no se ajusta, se mantiene el precio actual del turno. */
        @PositiveOrZero(message = "No puede ser negativo") Long precioFinal,
        MetodoPago metodoPago,
        TipoPago tipoPago,
        /** En centavos. Solo COBERTURA_CON_COPAGO. */
        @PositiveOrZero(message = "No puede ser negativo") Long copago,
        @Size(max = 60, message = "El número de autorización puede tener hasta 60 caracteres") String numeroAutorizacion
) {
}
package com.innovax.barberia.caja;

import com.innovax.barberia.atencion.MetodoPago;
import com.innovax.barberia.cobertura.TipoPago;

import java.time.Instant;
import java.util.UUID;

/**
 * Una atención completada, con lo justo para armar la caja (sin cargar entidades enteras).
 * {@code precioFinal} es lo que entró a la caja: el precio, o el copago si se atendió por obra social.
 */
public record FilaCaja(
        Instant inicio,
        long precioFinal,
        MetodoPago metodoPago,
        UUID profesionalId,
        String profesionalNombre,
        String profesionalApellido,
        UUID servicioId,
        String servicioNombre,
        String clienteNombre) {

    /** Lo usa la consulta de {@link CajaRepository}: con copago, a la caja entra solo el copago. */
    public FilaCaja(Instant inicio, long precioFinal, TipoPago tipoPago, Long copago, MetodoPago metodoPago,
                    UUID profesionalId, String profesionalNombre, String profesionalApellido,
                    UUID servicioId, String servicioNombre, String clienteNombre) {
        this(inicio,
                tipoPago == TipoPago.COBERTURA_CON_COPAGO ? (copago == null ? 0L : copago)
                        : tipoPago == TipoPago.COBERTURA ? 0L : precioFinal,
                metodoPago, profesionalId, profesionalNombre, profesionalApellido, servicioId, servicioNombre, clienteNombre);
    }
}

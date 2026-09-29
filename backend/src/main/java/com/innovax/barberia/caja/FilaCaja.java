package com.innovax.barberia.caja;

import com.innovax.barberia.atencion.MetodoPago;

import java.time.Instant;
import java.util.UUID;

/** Una atención completada, con lo justo para armar la caja (sin cargar entidades enteras). */
public record FilaCaja(
        Instant inicio,
        long precioFinal,
        MetodoPago metodoPago,
        UUID barberoId,
        String barberoNombre,
        String barberoApellido,
        UUID servicioId,
        String servicioNombre,
        String clienteNombre) {
}

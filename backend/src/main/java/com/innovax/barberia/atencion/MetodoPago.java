package com.innovax.barberia.atencion;

/** Cómo pagó el cliente en el local. No hay cobro online. */
public enum MetodoPago {
    EFECTIVO,
    TRANSFERENCIA,
    MERCADOPAGO,   // QR o transferencia en el local
    TARJETA
}

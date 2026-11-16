package com.innovax.barberia.cobertura;

import java.time.Instant;
import java.util.UUID;

/** Una atención completada por obra social / prepaga, con lo justo para el reporte de facturación. */
public record FilaCobertura(
        UUID atencionId,
        Instant inicio,
        UUID coberturaId,
        String coberturaNombre,
        TipoPago tipoPago,
        long precioFinal,
        Long copago,
        String numeroAutorizacion,
        String pacienteNombre,
        String pacienteDni,
        String numeroAfiliado,
        String plan,
        String profesionalNombre,
        String profesionalApellido,
        String profesionalMatricula,
        String servicioNombre) {
}

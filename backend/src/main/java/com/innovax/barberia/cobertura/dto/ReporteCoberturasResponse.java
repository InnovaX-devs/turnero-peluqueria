package com.innovax.barberia.cobertura.dto;

import com.innovax.barberia.cobertura.TipoPago;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reporte para facturar a mano a cada obra social / prepaga (issue #67): atenciones completadas en el rango.
 * Importes en centavos. {@code totalPrestaciones} es la suma del valor de las prestaciones (precio final);
 * {@code totalCopagos}, lo que ya pagaron los pacientes.
 */
public record ReporteCoberturasResponse(
        LocalDate desde,
        LocalDate hasta,
        int cantidad,
        long totalPrestaciones,
        long totalCopagos,
        List<Resumen> porCobertura,
        List<Detalle> detalle) {

    public record Resumen(UUID coberturaId, String nombre, int cantidad, long totalPrestaciones, long totalCopagos) {
    }

    /** {@code fecha} en la zona horaria del negocio. Afiliado y plan son los que tiene hoy el paciente en la ficha. */
    public record Detalle(
            UUID atencionId,
            LocalDateTime fecha,
            UUID coberturaId,
            String coberturaNombre,
            TipoPago tipoPago,
            String pacienteNombre,
            String pacienteDni,
            String numeroAfiliado,
            String plan,
            String profesional,
            String matricula,
            String prestacion,
            long precio,
            Long copago,
            String numeroAutorizacion) {
    }
}

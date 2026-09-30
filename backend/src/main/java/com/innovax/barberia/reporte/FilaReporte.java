package com.innovax.barberia.reporte;

import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.OrigenAtencion;

import java.time.Instant;
import java.util.UUID;

/** Una atención del período, con lo justo para los reportes. cliente* es null en cortes anónimos. */
public record FilaReporte(
        Instant inicio,
        EstadoAtencion estado,
        OrigenAtencion origen,
        long precioFinal,
        UUID barberoId,
        String barberoNombre,
        String barberoApellido,
        UUID clienteId,
        String clienteNombre,
        String clienteTelefono) {
}

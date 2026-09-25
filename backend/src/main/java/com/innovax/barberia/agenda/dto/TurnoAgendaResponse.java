package com.innovax.barberia.agenda.dto;

import com.innovax.barberia.agenda.EstadoPago;
import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.MetodoPago;
import com.innovax.barberia.atencion.OrigenAtencion;

import java.time.Instant;
import java.util.UUID;

/**
 * Un turno tal como lo ve el panel: a diferencia de {@link com.innovax.barberia.atencion.dto.AtencionResponse}
 * (que es la respuesta pública de la reserva online), acá sí va el teléfono del cliente, porque
 * lo usa el barbero/admin para escribirle por WhatsApp desde el detalle del turno.
 */
public record TurnoAgendaResponse(
        UUID id,
        Instant inicio,
        Instant fin,
        EstadoAtencion estado,
        OrigenAtencion origen,
        EstadoPago estadoPago,
        MetodoPago metodoPago,
        UUID barberoId,
        String barberoNombre,
        UUID servicioId,
        String servicioNombre,
        long precioFinal,
        /** Null = corte anónimo (cargado a mano sin cliente asociado). */
        String clienteNombre,
        String clienteTelefono,
        String notas
) {

    public static TurnoAgendaResponse de(Atencion a) {
        return new TurnoAgendaResponse(
                a.getId(),
                a.getInicio(),
                a.getFin(),
                a.getEstado(),
                a.getOrigen(),
                a.getMetodoPago() != null ? EstadoPago.PAGADO : EstadoPago.PENDIENTE,
                a.getMetodoPago(),
                a.getBarbero().getId(),
                a.getBarbero().getNombre() + " " + a.getBarbero().getApellido(),
                a.getServicio().getId(),
                a.getServicio().getNombre(),
                a.getPrecioFinal(),
                a.getCliente() != null ? a.getCliente().getNombre() : null,
                a.getCliente() != null ? a.getCliente().getTelefono() : null,
                a.getNotas());
    }
}
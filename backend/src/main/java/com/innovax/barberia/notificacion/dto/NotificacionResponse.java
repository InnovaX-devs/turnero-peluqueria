package com.innovax.barberia.notificacion.dto;

import com.innovax.barberia.notificacion.EstadoNotificacion;
import com.innovax.barberia.notificacion.Notificacion;
import com.innovax.barberia.notificacion.TipoCanal;
import com.innovax.barberia.notificacion.TipoNotificacion;

import java.time.Instant;
import java.util.UUID;

/** Un mensaje al cliente tal como lo ve el panel. Se arma con la transacción abierta (relaciones lazy). */
public record NotificacionResponse(
        UUID id,
        UUID atencionId,
        TipoNotificacion tipo,
        TipoCanal canal,
        EstadoNotificacion estado,
        String clienteNombre,
        String destinatario,
        String barberoNombre,
        Instant turnoInicio,
        String mensaje,
        /** Link wa.me con el mensaje ya escrito. Null si el canal no es WhatsApp. */
        String linkWhatsApp,
        Instant programadaPara,
        Instant enviadaEn,
        String error
) {

    public static NotificacionResponse de(Notificacion n, String linkWhatsApp) {
        var turno = n.getAtencion();
        return new NotificacionResponse(
                n.getId(),
                turno.getId(),
                n.getTipo(),
                n.getCanal(),
                n.getEstado(),
                turno.getCliente() != null ? turno.getCliente().getNombre() : null,
                n.getDestinatario(),
                turno.getBarbero().getNombre() + " " + turno.getBarbero().getApellido(),
                turno.getInicio(),
                n.getMensaje(),
                linkWhatsApp,
                n.getProgramadaPara(),
                n.getEnviadaEn(),
                n.getError());
    }
}
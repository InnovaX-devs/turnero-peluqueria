package com.innovax.barberia.notificacion;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Canal de la primera versión: no manda el mensaje solo, deja listo un link wa.me para que
 * lo abra una persona (el barbero desde el panel o el cliente desde la pantalla de reserva).
 * "Enviar" acá significa validar el teléfono y poder generar ese link.
 */
@Component
public class WhatsAppLinkCanal implements CanalMensajeria {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private final NotificacionLinks links;

    public WhatsAppLinkCanal(NotificacionLinks links) {
        this.links = links;
    }

    @Override
    public TipoCanal tipo() {
        return TipoCanal.WHATSAPP_LINK;
    }

    @Override
    public ResultadoEnvio enviar(Notificacion notificacion) {
        if (!E164.matcher(notificacion.getDestinatario()).matches()) {
            return ResultadoEnvio.fallo("El teléfono no está en formato internacional (+549...)");
        }
        return ResultadoEnvio.ok();
    }

    /** El link que se abre para mandar el mensaje por WhatsApp. */
    public String link(Notificacion notificacion) {
        return links.whatsApp(notificacion.getDestinatario(), notificacion.getMensaje());
    }
}
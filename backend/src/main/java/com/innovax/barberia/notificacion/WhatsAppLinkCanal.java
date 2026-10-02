package com.innovax.barberia.notificacion;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Canal de la primera versión: no manda el mensaje solo. Deja listo un link wa.me para que una
 * persona (el barbero o el admin, desde el panel) lo abra y toque "enviar" en WhatsApp.
 */
@Component
public class WhatsAppLinkCanal implements CanalMensajeria {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    @Override
    public TipoCanal tipo() {
        return TipoCanal.WHATSAPP_LINK;
    }

    @Override
    public boolean esAutomatico() {
        return false;
    }

    @Override
    public ResultadoEnvio validar(Notificacion notificacion) {
        if (!E164.matcher(notificacion.getDestinatario()).matches()) {
            return ResultadoEnvio.fallo("El teléfono no está en formato internacional (+549...)");
        }
        return ResultadoEnvio.ok();
    }
}
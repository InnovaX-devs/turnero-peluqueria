package com.innovax.barberia.notificacion;

import lombok.extern.slf4j.Slf4j;

/**
 * Para desarrollo: no manda nada, escribe el email en el log (incluido el link de recuperación).
 * Se usa cuando no hay RESEND_API_KEY. No usar en producción: cualquiera con acceso al log
 * podría cambiar contraseñas.
 */
@Slf4j
public class LogEmailSender implements EmailSender {

    @Override
    public void enviar(Email email) {
        log.info("""

                ===== EMAIL (no enviado: falta RESEND_API_KEY) =====
                Para:   {}
                Asunto: {}

                {}
                ====================================================""",
                email.para(), email.asunto(), email.texto());
    }
}
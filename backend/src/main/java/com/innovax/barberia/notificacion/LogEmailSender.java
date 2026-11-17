package com.innovax.barberia.notificacion;

import lombok.extern.slf4j.Slf4j;

/**
 * Para desarrollo: no manda nada, lo deja en el log. Se usa cuando no hay RESEND_API_KEY.
 *
 * <p>Los emails tienen nombres de pacientes, datos de turnos y links de recuperación de contraseña (issue #73:
 * nada de eso tiene que quedar en los logs de producción). Por defecto solo se loguea el destinatario
 * enmascarado y el asunto; el cuerpo completo se imprime únicamente con {@code app.mail.log-contenido=true},
 * que el perfil "local" activa para poder copiar el link de recuperación mientras se desarrolla.
 */
@Slf4j
public class LogEmailSender implements EmailSender {

    private final boolean mostrarContenido;

    public LogEmailSender(boolean mostrarContenido) {
        this.mostrarContenido = mostrarContenido;
    }

    @Override
    public void enviar(Email email) {
        if (!mostrarContenido) {
            log.info("Email NO enviado (falta RESEND_API_KEY). Para: {} · Asunto: {}",
                    enmascarar(email.para()), email.asunto());
            return;
        }
        log.info("""

                ===== EMAIL (no enviado: falta RESEND_API_KEY) =====
                Para:   {}
                Asunto: {}

                {}
                ====================================================""",
                email.para(), email.asunto(), email.texto());
    }

    /** "juan.perez@gmail.com" → "j***@gmail.com". */
    static String enmascarar(String direccion) {
        if (direccion == null || direccion.isBlank()) {
            return "(sin destinatario)";
        }
        int arroba = direccion.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return direccion.charAt(0) + "***" + direccion.substring(arroba);
    }
}

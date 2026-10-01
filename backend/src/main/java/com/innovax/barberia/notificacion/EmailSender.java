package com.innovax.barberia.notificacion;

/** Envía un email. La implementación se elige en {@link EmailConfig} según las variables de entorno. */
public interface EmailSender {

    void enviar(Email email);

    record Email(String para, String asunto, String texto, String html) {
    }
}
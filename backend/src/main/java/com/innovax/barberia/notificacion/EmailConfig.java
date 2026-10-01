package com.innovax.barberia.notificacion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Con RESEND_API_KEY se mandan mails de verdad; sin ella, se escriben en el log. */
@Slf4j
@Configuration
@EnableAsync
public class EmailConfig {

    @Bean
    public EmailSender emailSender(
            @Value("${app.mail.resend-api-key:}") String apiKey,
            @Value("${app.mail.remitente:}") String remitente) {
        if (apiKey.isBlank()) {
            log.warn("RESEND_API_KEY no está configurada: los emails se van a mostrar en el log en vez de enviarse");
            return new LogEmailSender();
        }
        if (remitente.isBlank()) {
            throw new IllegalStateException("Con RESEND_API_KEY configurada también hace falta MAIL_REMITENTE");
        }
        return new ResendEmailSender(apiKey, remitente);
    }
}
package com.innovax.barberia.notificacion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/** Envía por la API HTTP de Resend (https://resend.com/docs/api-reference/emails/send-email). */
@Slf4j
public class ResendEmailSender implements EmailSender {

    private static final String URL = "https://api.resend.com/emails";

    private final RestClient http = RestClient.create();
    private final String apiKey;
    private final String remitente;

    public ResendEmailSender(String apiKey, String remitente) {
        this.apiKey = apiKey;
        this.remitente = remitente;
    }

    @Override
    public void enviar(Email email) {
        Map<String, Object> body = Map.of(
                "from", remitente,
                "to", List.of(email.para()),
                "subject", email.asunto(),
                "text", email.texto(),
                "html", email.html());
        try {
            http.post()
                    .uri(URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // No se loguea el contenido: el link de recuperación es una credencial
            log.error("No se pudo enviar el email «{}» (revisá RESEND_API_KEY y MAIL_REMITENTE)", email.asunto(), e);
            throw new IllegalStateException("No se pudo enviar el email", e);
        }
    }
}
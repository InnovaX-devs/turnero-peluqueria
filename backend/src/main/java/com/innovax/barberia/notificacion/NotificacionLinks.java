package com.innovax.barberia.notificacion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Arma los links que van dentro de los mensajes y el link wa.me para mandarlos. */
@Component
public class NotificacionLinks {

    private final String urlFrontend;

    public NotificacionLinks(@Value("${app.frontend-url:http://localhost:5173}") String urlFrontend) {
        this.urlFrontend = urlFrontend.endsWith("/")
                ? urlFrontend.substring(0, urlFrontend.length() - 1)
                : urlFrontend;
    }

    /** Link para que el cliente cancele o reprograme su turno sin login. */
    public String gestionTurno(UUID tokenGestion) {
        return urlFrontend + "/turno/" + tokenGestion;
    }

    /** Link wa.me con el mensaje ya escrito. El teléfono viene en E.164 (+5493584111111). */
    public String whatsApp(String telefono, String mensaje) {
        String digitos = telefono.replaceAll("\\D", "");
        String texto = URLEncoder.encode(mensaje, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/" + digitos + "?text=" + texto;
    }
}
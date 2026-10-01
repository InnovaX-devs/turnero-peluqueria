package com.innovax.barberia.auth;

import com.innovax.barberia.notificacion.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

/**
 * Manda el email de recuperación después de que se guardó el token (AFTER_COMMIT) y en otro hilo:
 * así el endpoint tarda lo mismo exista o no la cuenta, y si el proveedor de mails falla
 * no se rompe la respuesta.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRecuperacionListener {

    private final EmailSender emailSender;

    @Async
    @TransactionalEventListener
    public void enviar(RecuperacionSolicitada evento) {
        String asunto = "Restablecé tu contraseña";
        String texto = """
                Recibimos un pedido para restablecer la contraseña de tu cuenta.

                Entrá a este link para elegir una nueva (vence en 1 hora y sirve una sola vez):
                %s

                Si no lo pediste vos, ignorá este email: tu contraseña no cambia.
                """.formatted(evento.link());
        String link = HtmlUtils.htmlEscape(evento.link());
        String html = """
                <p>Recibimos un pedido para restablecer la contraseña de tu cuenta.</p>
                <p><a href="%s">Elegir una contraseña nueva</a></p>
                <p>El link vence en 1 hora y sirve una sola vez.</p>
                <p style="color:#666">Si no lo pediste vos, ignorá este email: tu contraseña no cambia.</p>
                """.formatted(link);
        try {
            emailSender.enviar(new EmailSender.Email(evento.email(), asunto, texto, html));
        } catch (RuntimeException e) {
            // El EmailSender ya dejó el detalle en el log; el usuario puede pedir otro link
            log.warn("No se envió el email de recuperación");
        }
    }
}
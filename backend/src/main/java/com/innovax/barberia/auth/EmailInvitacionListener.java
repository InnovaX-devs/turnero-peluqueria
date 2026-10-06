package com.innovax.barberia.auth;

import com.innovax.barberia.notificacion.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

/**
 * Manda el email de invitación al panel después de que se guardó el token (AFTER_COMMIT) y en otro hilo,
 * así un fallo del proveedor de mails no rompe el alta del barbero (el admin puede reenviar la invitación).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailInvitacionListener {

    private final EmailSender emailSender;

    @Async
    @TransactionalEventListener
    public void enviar(UsuarioInvitado evento) {
        String asunto = "Te invitaron a entrar al panel";
        String texto = """
                Te crearon un usuario para entrar al panel de la barbería.

                Entrá a este link para elegir tu contraseña (vence en 3 días y sirve una sola vez):
                %s

                Si no esperabas este email, ignoralo.
                """.formatted(evento.link());
        String link = HtmlUtils.htmlEscape(evento.link());
        String html = """
                <p>Te crearon un usuario para entrar al panel de la barbería.</p>
                <p><a href="%s">Elegir mi contraseña</a></p>
                <p>El link vence en 3 días y sirve una sola vez.</p>
                <p style="color:#666">Si no esperabas este email, ignoralo.</p>
                """.formatted(link);
        try {
            emailSender.enviar(new EmailSender.Email(evento.email(), asunto, texto, html));
        } catch (RuntimeException e) {
            // El EmailSender ya dejó el detalle en el log; el admin puede reenviar la invitación
            log.warn("No se envió el email de invitación");
        }
    }
}
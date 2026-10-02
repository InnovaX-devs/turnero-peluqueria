package com.innovax.barberia.notificacion;

/**
 * Por dónde sale un mensaje. Hoy hay un solo canal (link wa.me, que manda una persona); cuando
 * se pase a la API de WhatsApp Business se agrega otra implementación automática sin tocar el
 * servicio ni el job.
 */
public interface CanalMensajeria {

    TipoCanal tipo();

    /** true si el sistema manda el mensaje solo (API); false si lo tiene que mandar una persona. */
    boolean esAutomatico();

    /** Revisa que el mensaje se pueda mandar por este canal (por ejemplo, que el teléfono sea válido). */
    ResultadoEnvio validar(Notificacion notificacion);

    /** Solo para canales automáticos: manda el mensaje. */
    default ResultadoEnvio enviar(Notificacion notificacion) {
        throw new UnsupportedOperationException("El canal " + tipo() + " no envía mensajes solo");
    }
}
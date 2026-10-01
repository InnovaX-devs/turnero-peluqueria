package com.innovax.barberia.notificacion;

/**
 * Por dónde sale un mensaje. Hoy hay un solo canal (link wa.me); cuando se pase a la API de
 * WhatsApp Business se agrega otra implementación sin tocar el servicio ni el job.
 */
public interface CanalMensajeria {

    TipoCanal tipo();

    ResultadoEnvio enviar(Notificacion notificacion);
}
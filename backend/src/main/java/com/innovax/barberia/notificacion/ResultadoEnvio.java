package com.innovax.barberia.notificacion;

/** Qué pasó al intentar mandar una notificación. */
public record ResultadoEnvio(boolean exitoso, String error) {

    public static ResultadoEnvio ok() {
        return new ResultadoEnvio(true, null);
    }

    public static ResultadoEnvio fallo(String error) {
        return new ResultadoEnvio(false, error);
    }
}
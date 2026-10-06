package com.innovax.barberia.alta;

/** Datos del alta que no sirven. El mensaje está pensado para mostrarlo tal cual en la terminal. */
public class AltaInvalidaException extends RuntimeException {

    public AltaInvalidaException(String mensaje) {
        super(mensaje);
    }
}

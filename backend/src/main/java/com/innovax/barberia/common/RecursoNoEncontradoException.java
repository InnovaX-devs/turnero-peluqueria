package com.innovax.barberia.common;

/** Se lanza cuando un id recibido en la request no corresponde a nada en la base. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
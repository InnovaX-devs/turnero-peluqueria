package com.innovax.barberia.common;

import lombok.Getter;

/** Regla de negocio incumplida sobre un campo puntual. Se responde 400 con el mismo formato que la validación de Spring. */
@Getter
public class DatosInvalidosException extends RuntimeException {

    private final String campo;

    public DatosInvalidosException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }
}
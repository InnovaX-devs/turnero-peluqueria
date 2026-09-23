package com.innovax.barberia.common;

import lombok.Getter;

/** El dato choca con uno existente (ej. slug repetido). Se responde 409. */
@Getter
public class ConflictoException extends RuntimeException {

    private final String campo;

    public ConflictoException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }
}
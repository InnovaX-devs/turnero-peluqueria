package com.innovax.barberia.auth;

/** Mismo mensaje si el email no existe o la contraseña está mal, para no revelar qué usuarios existen. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Email o contraseña incorrectos");
    }
}

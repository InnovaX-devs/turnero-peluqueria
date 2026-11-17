package com.innovax.barberia.common;

/**
 * Versión vigente de los términos y la política de privacidad que acepta el paciente al reservar
 * (issue #73). Se guarda junto con la fecha en {@code cliente.consentimiento_version}.
 * Cuando cambie el texto de la página de privacidad del frontend, subí esta versión.
 */
public final class TerminosSalud {

    public static final String VERSION = "2026-11-16";

    private TerminosSalud() {
    }
}

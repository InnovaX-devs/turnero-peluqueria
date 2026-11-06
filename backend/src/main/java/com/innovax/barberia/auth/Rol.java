package com.innovax.barberia.auth;

public enum Rol {
    ADMIN,
    PROFESIONAL;

    /**
     * Lee el rol guardado en un token. Acepta "BARBERO", el nombre de PROFESIONAL antes de la
     * migración V7 (issue #64): los tokens emitidos antes del deploy lo siguen trayendo hasta que
     * vencen. Se puede sacar cuando ya no quede ninguno vivo (vencen a las JWT_EXPIRACION, 8 h).
     */
    public static Rol desdeToken(String valor) {
        return "BARBERO".equals(valor) ? PROFESIONAL : Rol.valueOf(valor);
    }
}

package com.innovax.barberia.auth;

/** Se publica al generar un token de recuperación. El link lleva el token en texto plano. */
public record RecuperacionSolicitada(String email, String link) {
}
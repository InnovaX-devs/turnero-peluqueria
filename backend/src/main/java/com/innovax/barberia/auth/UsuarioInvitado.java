package com.innovax.barberia.auth;

/** Se publica al invitar a un usuario al panel. El link lleva el token en texto plano. */
public record UsuarioInvitado(String email, String link) {
}
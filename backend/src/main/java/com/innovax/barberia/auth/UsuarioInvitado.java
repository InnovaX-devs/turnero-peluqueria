package com.innovax.barberia.auth;

import com.innovax.barberia.negocio.Rubro;

/** Se publica al invitar a un usuario al panel. El link lleva el token en texto plano; el rubro es del negocio al que entra. */
public record UsuarioInvitado(String email, String link, Rubro rubro) {
}
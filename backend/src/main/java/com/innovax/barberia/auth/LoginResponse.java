package com.innovax.barberia.auth;

import java.time.Instant;

public record LoginResponse(String token, Instant expiraEn, UsuarioResponse usuario) {
}

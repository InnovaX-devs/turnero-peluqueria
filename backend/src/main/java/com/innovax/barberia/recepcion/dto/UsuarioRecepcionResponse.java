package com.innovax.barberia.recepcion.dto;

import com.innovax.barberia.auth.Usuario;

import java.time.Instant;
import java.util.UUID;

public record UsuarioRecepcionResponse(UUID id, String email, boolean activo, Instant creadoEn) {

    public static UsuarioRecepcionResponse de(Usuario u) {
        return new UsuarioRecepcionResponse(u.getId(), u.getEmail(), u.isActivo(), u.getCreatedAt());
    }
}

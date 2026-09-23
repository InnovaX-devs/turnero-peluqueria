package com.innovax.barberia.auth;

import java.util.UUID;

/** barberoId es null si el usuario no está vinculado a un barbero (ej. un admin que no corta). */
public record UsuarioResponse(UUID id, String email, Rol rol, UUID barberiaId, UUID barberoId) {
}

package com.innovax.barberia.auth;

import java.util.UUID;

/**
 * barberoId es null si el usuario no está vinculado a un barbero (ej. un admin que no corta).
 * barberiaNombre y barberiaLogoUrl son para la barra del panel: así los barberos también ven el
 * logo sin tener permiso para leer la configuración. barberiaLogoUrl es null si no hay logo.
 */
public record UsuarioResponse(UUID id, String email, Rol rol, UUID barberiaId, UUID barberoId,
                              String barberiaNombre, String barberiaLogoUrl) {
}

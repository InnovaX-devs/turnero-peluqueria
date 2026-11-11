package com.innovax.barberia.auth;

import com.innovax.barberia.negocio.Rubro;

import java.util.UUID;

/**
 * profesionalId es null si el usuario no está vinculado a un barbero (ej. un admin que no corta).
 * negocioNombre y negocioLogoUrl son para la barra del panel: así los barberos también ven el
 * logo sin tener permiso para leer la configuración. negocioLogoUrl es null si no hay logo.
 * rubro define cómo nombra las cosas el panel (barbero / profesional, cliente / paciente...).
 */
public record UsuarioResponse(UUID id, String email, Rol rol, UUID negocioId, UUID profesionalId,
                              String negocioNombre, String negocioLogoUrl, Rubro rubro) {
}

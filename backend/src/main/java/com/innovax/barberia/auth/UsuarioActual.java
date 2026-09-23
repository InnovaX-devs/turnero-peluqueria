package com.innovax.barberia.auth;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Datos del usuario logueado, sacados del token.
 *
 * REGLA DEL EQUIPO: toda consulta del panel filtra por {@link #barberiaId()}.
 * Nunca se toma el barberia_id del request (body, query o path), siempre de acá.
 */
@Component
public class UsuarioActual {

    private Jwt jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        throw new IllegalStateException("No hay un usuario autenticado");
    }

    public UUID id() {
        return UUID.fromString(jwt().getSubject());
    }

    public UUID barberiaId() {
        return UUID.fromString(jwt().getClaimAsString(JwtService.CLAIM_BARBERIA));
    }

    public Rol rol() {
        return Rol.valueOf(jwt().getClaimAsString(JwtService.CLAIM_ROL));
    }

    public Optional<UUID> barberoId() {
        return Optional.ofNullable(jwt().getClaimAsString(JwtService.CLAIM_BARBERO)).map(UUID::fromString);
    }

    public boolean esAdmin() {
        return rol() == Rol.ADMIN;
    }

    /**
     * Un admin puede operar sobre cualquier barbero; un barbero, solo sobre sí mismo.
     * Ojo: igual hay que verificar que el barbero sea de {@link #barberiaId()} al buscarlo en la base.
     */
    public void verificarAccesoABarbero(UUID barberoId) {
        if (esAdmin()) {
            return;
        }
        boolean esElMismo = barberoId().map(barberoId::equals).orElse(false);
        if (!esElMismo) {
            throw new AccessDeniedException("No tenés permiso sobre este barbero");
        }
    }
}

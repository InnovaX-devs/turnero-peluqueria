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
 * REGLA DEL EQUIPO: toda consulta del panel filtra por {@link #negocioId()}.
 * Nunca se toma el negocio_id del request (body, query o path), siempre de acá.
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

    public UUID negocioId() {
        return UUID.fromString(claim(JwtService.CLAIM_NEGOCIO, JwtService.CLAIM_NEGOCIO_ANTERIOR));
    }

    public Rol rol() {
        return Rol.desdeToken(jwt().getClaimAsString(JwtService.CLAIM_ROL));
    }

    public Optional<UUID> profesionalId() {
        return Optional.ofNullable(claim(JwtService.CLAIM_PROFESIONAL, JwtService.CLAIM_PROFESIONAL_ANTERIOR))
                .map(UUID::fromString);
    }

    /** El claim con su nombre actual o, en tokens emitidos antes de V7, con el anterior. */
    private String claim(String nombre, String nombreAnterior) {
        String valor = jwt().getClaimAsString(nombre);
        return valor != null ? valor : jwt().getClaimAsString(nombreAnterior);
    }

    public boolean esAdmin() {
        return rol() == Rol.ADMIN;
    }

    /**
     * Un admin puede operar sobre cualquier barbero; un barbero, solo sobre sí mismo.
     * Ojo: igual hay que verificar que el barbero sea de {@link #negocioId()} al buscarlo en la base.
     */
    public void verificarAccesoAProfesional(UUID profesionalId) {
        if (esAdmin()) {
            return;
        }
        boolean esElMismo = profesionalId().map(profesionalId::equals).orElse(false);
        if (!esElMismo) {
            throw new AccessDeniedException("No tenés permiso sobre este barbero");
        }
    }
}

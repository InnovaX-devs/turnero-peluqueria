package com.innovax.barberia.auth;

import com.innovax.barberia.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    public static final String CLAIM_NEGOCIO = "negocio";
    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_PROFESIONAL = "profesional";

    /**
     * Nombres de los claims antes de la migración V7 (issue #64). Los tokens emitidos antes del
     * deploy los siguen trayendo hasta que vencen; UsuarioActual los lee como alternativa.
     */
    static final String CLAIM_NEGOCIO_ANTERIOR = "barberia";
    static final String CLAIM_PROFESIONAL_ANTERIOR = "barbero";

    private final JwtEncoder encoder;
    private final JwtProperties props;

    public record TokenGenerado(String valor, Instant expiraEn) {
    }

    public TokenGenerado generar(Usuario usuario, UUID profesionalId) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(props.expiracion());

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("barberia")
                .issuedAt(ahora)
                .expiresAt(expira)
                .subject(usuario.getId().toString())
                .claim(CLAIM_NEGOCIO, usuario.getNegocio().getId().toString())
                .claim(CLAIM_ROL, usuario.getRol().name());
        if (profesionalId != null) {
            claims.claim(CLAIM_PROFESIONAL, profesionalId.toString());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new TokenGenerado(token, expira);
    }
}

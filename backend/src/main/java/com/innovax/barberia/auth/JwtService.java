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

    public static final String CLAIM_BARBERIA = "barberia";
    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_BARBERO = "barbero";

    private final JwtEncoder encoder;
    private final JwtProperties props;

    public record TokenGenerado(String valor, Instant expiraEn) {
    }

    public TokenGenerado generar(Usuario usuario, UUID barberoId) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(props.expiracion());

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("barberia")
                .issuedAt(ahora)
                .expiresAt(expira)
                .subject(usuario.getId().toString())
                .claim(CLAIM_BARBERIA, usuario.getBarberia().getId().toString())
                .claim(CLAIM_ROL, usuario.getRol().name());
        if (barberoId != null) {
            claims.claim(CLAIM_BARBERO, barberoId.toString());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new TokenGenerado(token, expira);
    }
}

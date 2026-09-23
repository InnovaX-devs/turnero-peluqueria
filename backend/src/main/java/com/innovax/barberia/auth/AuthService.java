package com.innovax.barberia.auth;

import com.innovax.barberia.barbero.Barbero;
import com.innovax.barberia.barbero.BarberoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarios;
    private final BarberoRepository barberos;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UsuarioActual usuarioActual;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(request.email().trim())
                .filter(Usuario::isActivo)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }

        UUID barberoId = barberoIdDe(usuario);
        JwtService.TokenGenerado token = jwtService.generar(usuario, barberoId);
        return new LoginResponse(token.valor(), token.expiraEn(), aResponse(usuario, barberoId));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse yo() {
        Usuario usuario = usuarios.findById(usuarioActual.id())
                .orElseThrow(CredencialesInvalidasException::new);
        return aResponse(usuario, barberoIdDe(usuario));
    }

    private UUID barberoIdDe(Usuario usuario) {
        return barberos.findByUsuarioId(usuario.getId()).map(Barbero::getId).orElse(null);
    }

    private UsuarioResponse aResponse(Usuario u, UUID barberoId) {
        return new UsuarioResponse(u.getId(), u.getEmail(), u.getRol(), u.getBarberia().getId(), barberoId);
    }
}

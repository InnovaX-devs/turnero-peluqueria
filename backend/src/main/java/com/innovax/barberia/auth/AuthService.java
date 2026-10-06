package com.innovax.barberia.auth;

import com.innovax.barberia.barbero.Barbero;
import com.innovax.barberia.barbero.BarberoRepository;
import com.innovax.barberia.common.DatosInvalidosException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
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

    /**
     * El usuario logueado cambia su contraseña. Pide la actual: si alguien deja la sesión abierta
     * en la compu de la barbería, otro no puede cambiársela. El token de la sesión sigue valiendo.
     */
    @Transactional
    public void cambiarPassword(String actual, String nueva) {
        Usuario usuario = usuarios.findById(usuarioActual.id())
                .filter(Usuario::isActivo)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordEncoder.matches(actual, usuario.getPasswordHash())) {
            throw new DatosInvalidosException("actual", "La contraseña actual no es correcta");
        }
        // BCrypt solo usa los primeros 72 bytes (y Spring tira error si hay más)
        if (nueva.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new DatosInvalidosException("nueva", "La contraseña es demasiado larga");
        }
        if (passwordEncoder.matches(nueva, usuario.getPasswordHash())) {
            throw new DatosInvalidosException("nueva", "Tiene que ser distinta de la actual");
        }
        usuario.setPasswordHash(passwordEncoder.encode(nueva));
    }

    private UUID barberoIdDe(Usuario usuario) {
        return barberos.findByUsuarioId(usuario.getId()).map(Barbero::getId).orElse(null);
    }

    private UsuarioResponse aResponse(Usuario u, UUID barberoId) {
        return new UsuarioResponse(u.getId(), u.getEmail(), u.getRol(), u.getBarberia().getId(), barberoId);
    }
}

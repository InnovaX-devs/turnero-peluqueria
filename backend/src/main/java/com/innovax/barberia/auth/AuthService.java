package com.innovax.barberia.auth;

import com.innovax.barberia.profesional.Profesional;
import com.innovax.barberia.profesional.ProfesionalRepository;
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
    private final ProfesionalRepository profesionales;
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

        UUID profesionalId = profesionalIdDe(usuario);
        JwtService.TokenGenerado token = jwtService.generar(usuario, profesionalId);
        return new LoginResponse(token.valor(), token.expiraEn(), aResponse(usuario, profesionalId));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse yo() {
        Usuario usuario = usuarios.findById(usuarioActual.id())
                .orElseThrow(CredencialesInvalidasException::new);
        return aResponse(usuario, profesionalIdDe(usuario));
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

    private UUID profesionalIdDe(Usuario usuario) {
        return profesionales.findByUsuarioId(usuario.getId()).map(Profesional::getId).orElse(null);
    }

    private UsuarioResponse aResponse(Usuario u, UUID profesionalId) {
        var negocio = u.getNegocio();
        return new UsuarioResponse(u.getId(), u.getEmail(), u.getRol(), negocio.getId(), profesionalId,
                negocio.getNombre(), negocio.getLogoUrl());
    }
}

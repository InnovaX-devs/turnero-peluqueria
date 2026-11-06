package com.innovax.barberia.recepcion;

import com.innovax.barberia.auth.RecuperacionService;
import com.innovax.barberia.auth.Rol;
import com.innovax.barberia.auth.Usuario;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.auth.UsuarioRepository;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.recepcion.dto.RecepcionRequests;
import com.innovax.barberia.recepcion.dto.UsuarioRecepcionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Usuarios de recepción / secretaría (issue #70). Los da de alta el admin; no están vinculados a
 * un profesional. Usan la misma invitación por email que los profesionales para elegir su contraseña.
 */
@Service
@RequiredArgsConstructor
public class RecepcionService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final NegocioRepository negocios;
    private final PasswordEncoder passwordEncoder;
    private final RecuperacionService recuperacion;
    private final UsuarioActual usuarioActual;

    @Transactional(readOnly = true)
    public List<UsuarioRecepcionResponse> listar() {
        return usuarios.findByNegocioIdAndRolOrderByEmailAsc(usuarioActual.negocioId(), Rol.RECEPCION).stream()
                .map(UsuarioRecepcionResponse::de)
                .toList();
    }

    /** Crea el usuario con una contraseña que nadie conoce y le manda la invitación para que elija la suya. */
    @Transactional
    public UsuarioRecepcionResponse crear(RecepcionRequests.Crear request) {
        Negocio negocio = negocios.findById(usuarioActual.negocioId())
                .orElseThrow(() -> new IllegalStateException("El negocio del token no existe"));
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarios.findByEmailIgnoreCase(email).isPresent()) {
            throw emailEnUso();
        }
        Usuario usuario = new Usuario();
        usuario.setNegocio(negocio);
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(passwordInutilizable()));
        usuario.setRol(Rol.RECEPCION);
        usuario.setActivo(true);
        try {
            usuario = usuarios.saveAndFlush(usuario); // el UNIQUE de la base cubre la carrera entre dos altas
        } catch (DataIntegrityViolationException e) {
            throw emailEnUso();
        }
        recuperacion.invitar(usuario);
        return UsuarioRecepcionResponse.de(usuario);
    }

    /** Manda un link nuevo (invalida los anteriores). Sirve si la invitación venció, no llegó o se olvidó la contraseña. */
    @Transactional
    public void reenviarInvitacion(UUID id) {
        Usuario usuario = buscar(id);
        if (!usuario.isActivo()) {
            throw new DatosInvalidosException("usuario", "El usuario está inactivo. Reactivalo para reenviar la invitación");
        }
        recuperacion.invitar(usuario);
    }

    /** Baja lógica: el login ya filtra por usuario activo. */
    @Transactional
    public UsuarioRecepcionResponse cambiarActivo(UUID id, boolean activo) {
        Usuario usuario = buscar(id);
        usuario.setActivo(activo);
        return UsuarioRecepcionResponse.de(usuario);
    }

    /** Solo usuarios de recepción del negocio del admin: cualquier otro id responde 404. */
    private Usuario buscar(UUID id) {
        return usuarios.findById(id)
                .filter(u -> u.getRol() == Rol.RECEPCION)
                .filter(u -> u.getNegocio().getId().equals(usuarioActual.negocioId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private static String passwordInutilizable() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); // 43 chars, entra en los 72 de BCrypt
    }

    private static ConflictoException emailEnUso() {
        return new ConflictoException("email", "Ese email ya está en uso");
    }
}

package com.innovax.barberia.auth;

import com.innovax.barberia.common.DatosInvalidosException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Recuperación de contraseña por email e invitación al panel (mismo mecanismo, distinta vigencia).
 *
 * El token viaja solo en el link del email; en la base se guarda su SHA-256. Como el token
 * es aleatorio de 256 bits no hace falta un hash lento (BCrypt), y SHA-256 permite buscarlo.
 */
@Service
public class RecuperacionService {

    static final Duration VIGENCIA = Duration.ofHours(1);
    /** La invitación vence más tarde que un reset: el barbero puede no mirar el mail enseguida. */
    static final Duration VIGENCIA_INVITACION = Duration.ofHours(72);
    /** Si ya se pidió un link hace menos de esto, no se manda otro (evita llenar la casilla de alguien). */
    static final Duration ESPERA_ENTRE_PEDIDOS = Duration.ofMinutes(1);

    static final String LINK_INVALIDO = "El link no es válido. Pedí uno nuevo.";
    static final String LINK_USADO = "Este link ya fue usado. Si necesitás, pedí uno nuevo.";
    static final String LINK_VENCIDO = "Este link venció. Pedí uno nuevo.";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final TokenRecuperacionRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventos;
    private final String frontendUrl;

    public RecuperacionService(UsuarioRepository usuarios,
                               TokenRecuperacionRepository tokens,
                               PasswordEncoder passwordEncoder,
                               ApplicationEventPublisher eventos,
                               @Value("${app.frontend-url}") String frontendUrl) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.eventos = eventos;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    /** No devuelve nada a propósito: el controller responde igual exista o no la cuenta. */
    @Transactional
    public void solicitar(String email) {
        Optional<Usuario> encontrado = usuarios.findByEmailIgnoreCase(email.trim()).filter(Usuario::isActivo);
        if (encontrado.isEmpty()) {
            return;
        }
        Usuario usuario = encontrado.get();
        Instant ahora = Instant.now();

        if (tokens.existsByUsuarioIdAndCreatedAtAfter(usuario.getId(), ahora.minus(ESPERA_ENTRE_PEDIDOS))) {
            return;
        }

        // Solo vale el último link pedido
        tokens.invalidarPendientes(usuario.getId(), ahora);

        String token = generarToken();
        tokens.save(new TokenRecuperacion(usuario, hashear(token), ahora.plus(VIGENCIA)));

        // El email se manda después del commit y en otro hilo (ver EmailRecuperacionListener)
        String link = frontendUrl + "/panel/restablecer?token=" + token;
        eventos.publishEvent(new RecuperacionSolicitada(usuario.getEmail(), link));
    }

    /**
     * Invita a un usuario ya creado a elegir su contraseña (la usa el ABM de barberos).
     * Reutiliza el token y la pantalla de restablecer; invalida los links anteriores.
     * A diferencia de {@link #solicitar}, acá sí se avisa si se pide demasiado seguido: lo dispara un admin.
     */
    @Transactional
    public void invitar(Usuario usuario) {
        Instant ahora = Instant.now();
        if (tokens.existsByUsuarioIdAndCreatedAtAfter(usuario.getId(), ahora.minus(ESPERA_ENTRE_PEDIDOS))) {
            throw new DatosInvalidosException("usuario",
                    "Ya se mandó un link hace instantes. Esperá un minuto y probá de nuevo.");
        }

        tokens.invalidarPendientes(usuario.getId(), ahora);

        String token = generarToken();
        tokens.save(new TokenRecuperacion(usuario, hashear(token), ahora.plus(VIGENCIA_INVITACION)));

        String link = frontendUrl + "/panel/restablecer?token=" + token;
        eventos.publishEvent(new UsuarioInvitado(usuario.getEmail(), link));
    }

    @Transactional
    public void restablecer(String token, String password) {
        // BCrypt solo usa los primeros 72 bytes (y Spring tira error si hay más)
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new DatosInvalidosException("password", "La contraseña es demasiado larga");
        }

        Instant ahora = Instant.now();
        TokenRecuperacion recuperacion = tokens.findByTokenHash(hashear(token.trim()))
                .orElseThrow(() -> new DatosInvalidosException("token", LINK_INVALIDO));

        if (recuperacion.estaUsado()) {
            throw new DatosInvalidosException("token", LINK_USADO);
        }
        if (recuperacion.estaVencido(ahora)) {
            throw new DatosInvalidosException("token", LINK_VENCIDO);
        }
        Usuario usuario = recuperacion.getUsuario();
        if (!usuario.isActivo()) {
            throw new DatosInvalidosException("token", LINK_INVALIDO);
        }

        usuario.setPasswordHash(passwordEncoder.encode(password));
        recuperacion.marcarUsado(ahora);
        tokens.invalidarPendientes(usuario.getId(), ahora);
    }

    static String generarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashear(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
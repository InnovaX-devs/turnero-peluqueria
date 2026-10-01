package com.innovax.barberia.auth;

import com.innovax.barberia.barberia.BarberiaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recuperar y restablecer la contraseña. Usa un usuario propio (creado y borrado en cada test)
 * para no cambiarle la contraseña a los usuarios del seed que usan los demás tests.
 * Al borrar el usuario, la base borra sus tokens (ON DELETE CASCADE).
 */
@SpringBootTest
class RecuperacionIntegrationTest {

    private static final UUID JADE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final String EMAIL = "recupero.test@jade.com";
    private static final String PASSWORD_VIEJA = "vieja1234";
    private static final String PASSWORD_NUEVA = "nueva5678";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private BarberiaRepository barberias;
    @Autowired
    private TokenRecuperacionRepository tokens;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        usuarios.findByEmailIgnoreCase(EMAIL).ifPresent(usuarios::delete);   // por si quedó de una corrida cortada

        Usuario u = new Usuario();
        u.setBarberia(barberias.findById(JADE).orElseThrow());
        u.setEmail(EMAIL);
        u.setPasswordHash(passwordEncoder.encode(PASSWORD_VIEJA));
        u.setRol(Rol.BARBERO);
        usuario = usuarios.save(u);
    }

    @AfterEach
    void limpiar() {
        usuarios.deleteById(usuario.getId());
    }

    // ---------- helpers ----------

    private String crearToken(Instant expiraEn) {
        String token = RecuperacionService.generarToken();
        tokens.save(new TokenRecuperacion(usuario, RecuperacionService.hashear(token), expiraEn));
        return token;
    }

    private ResultActions recuperar(String email) throws Exception {
        return mvc.perform(post("/api/auth/recuperar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)));
    }

    private ResultActions restablecer(String token, String password) throws Exception {
        return mvc.perform(post("/api/auth/restablecer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"password\":\"%s\"}".formatted(token, password)));
    }

    private ResultActions login(String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, password)));
    }

    private int tokensDelUsuario() {
        Integer n = jdbc.queryForObject(
                "select count(*) from token_recuperacion where usuario_id = ?", Integer.class, usuario.getId());
        return n == null ? 0 : n;
    }

    private boolean estaUsado(String token) {
        Timestamp usadoEn = jdbc.queryForObject(
                "select usado_en from token_recuperacion where token_hash = ?",
                Timestamp.class, RecuperacionService.hashear(token));
        return usadoEn != null;
    }

    // ---------- Pedir el link ----------

    @Test
    void recuperarRespondeLoMismoExistaONoLaCuenta() throws Exception {
        String conCuenta = recuperar(EMAIL.toUpperCase())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(AuthController.RESPUESTA_RECUPERAR))
                .andReturn().getResponse().getContentAsString();

        String sinCuenta = recuperar("nadie.registrado@jade.com")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(sinCuenta).isEqualTo(conCuenta);
    }

    @Test
    void recuperarGeneraUnTokenHasheadoQueVenceEnUnaHora() throws Exception {
        recuperar(EMAIL).andExpect(status().isOk());

        assertThat(tokensDelUsuario()).isEqualTo(1);
        String hash = jdbc.queryForObject(
                "select token_hash from token_recuperacion where usuario_id = ?", String.class, usuario.getId());
        assertThat(hash).matches("[0-9a-f]{64}");

        Timestamp expira = jdbc.queryForObject(
                "select expira_en from token_recuperacion where usuario_id = ?", Timestamp.class, usuario.getId());
        assertThat(expira.toInstant())
                .isCloseTo(Instant.now().plus(Duration.ofHours(1)), within(Duration.ofMinutes(1)));
    }

    @Test
    void recuperarDosVecesSeguidasNoGeneraOtroToken() throws Exception {
        recuperar(EMAIL).andExpect(status().isOk());
        recuperar(EMAIL).andExpect(status().isOk());

        assertThat(tokensDelUsuario()).isEqualTo(1);
    }

    @Test
    void recuperarConEmailInvalidoDevuelve400() throws Exception {
        recuperar("no-es-un-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").exists());
    }

    // ---------- Restablecer ----------

    @Test
    void tokenValidoCambiaLaContrasenaYQuedaUsado() throws Exception {
        String token = crearToken(Instant.now().plus(Duration.ofHours(1)));

        restablecer(token, PASSWORD_NUEVA).andExpect(status().isNoContent());

        login(PASSWORD_NUEVA).andExpect(status().isOk());
        login(PASSWORD_VIEJA).andExpect(status().isUnauthorized());
        assertThat(estaUsado(token)).isTrue();
    }

    @Test
    void tokenVencidoDevuelve400YNoCambiaLaContrasena() throws Exception {
        String token = crearToken(Instant.now().minus(Duration.ofMinutes(1)));

        restablecer(token, PASSWORD_NUEVA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.token").value(RecuperacionService.LINK_VENCIDO));

        login(PASSWORD_VIEJA).andExpect(status().isOk());
    }

    @Test
    void tokenYaUsadoDevuelve400() throws Exception {
        String token = crearToken(Instant.now().plus(Duration.ofHours(1)));
        restablecer(token, PASSWORD_NUEVA).andExpect(status().isNoContent());

        restablecer(token, "otra-clave-999")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.token").value(RecuperacionService.LINK_USADO));

        login(PASSWORD_NUEVA).andExpect(status().isOk());
    }

    @Test
    void tokenInexistenteDevuelve400() throws Exception {
        restablecer(RecuperacionService.generarToken(), PASSWORD_NUEVA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.token").value(RecuperacionService.LINK_INVALIDO));

        login(PASSWORD_VIEJA).andExpect(status().isOk());
    }

    @Test
    void alRestablecerSeInvalidanLosOtrosLinksPendientes() throws Exception {
        String viejo = crearToken(Instant.now().plus(Duration.ofHours(1)));
        String nuevo = crearToken(Instant.now().plus(Duration.ofHours(1)));

        restablecer(nuevo, PASSWORD_NUEVA).andExpect(status().isNoContent());

        restablecer(viejo, "otra-clave-999")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.token").value(RecuperacionService.LINK_USADO));
    }

    @Test
    void contrasenaDeMenosDe8CaracteresDevuelve400YNoGastaElToken() throws Exception {
        String token = crearToken(Instant.now().plus(Duration.ofHours(1)));

        restablecer(token, "corta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.password").exists());

        assertThat(estaUsado(token)).isFalse();
        login(PASSWORD_VIEJA).andExpect(status().isOk());
    }
}
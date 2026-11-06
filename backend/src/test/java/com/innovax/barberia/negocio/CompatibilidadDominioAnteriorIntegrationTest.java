package com.innovax.barberia.negocio;

import com.innovax.barberia.auth.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue #64 (barbería/barbero -> negocio/profesional): lo que ya está en circulación tiene que
 * seguir andando igual. Rutas públicas viejas, campos y parámetros viejos, tokens emitidos antes
 * del deploy y la migración de datos.
 * Usa los datos del seed de dev (necesita la base levantada). @Transactional: no deja cambios.
 */
@SpringBootTest
@Transactional
class CompatibilidadDominioAnteriorIntegrationTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    private static final String NEGOCIO_JADE = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String SUCURSAL = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final String LUCAS = "cccccccc-0000-0000-0000-000000000001";
    private static final String SERVICIO_CORTE = "dddddddd-0000-0000-0000-000000000001";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private LocalDate lunes;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        lunes = LocalDate.now(ZONA).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private String cuerpo(String url) throws Exception {
        return mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    // ---------- Rutas públicas viejas (links y páginas de reserva en circulación) ----------

    @Test
    void laRutaPublicaViejaDelNegocioDevuelveLoMismoQueLaNueva() throws Exception {
        assertThat(cuerpo("/api/public/barberias/jade")).isEqualTo(cuerpo("/api/public/negocios/jade"));
    }

    @Test
    void laRutaViejaDeBarberosDevuelveLoMismoQueLaDeProfesionales() throws Exception {
        String params = "?sucursal=" + SUCURSAL + "&servicio=" + SERVICIO_CORTE;
        assertThat(cuerpo("/api/public/barberias/jade/barberos" + params))
                .isEqualTo(cuerpo("/api/public/negocios/jade/profesionales" + params));
    }

    @Test
    void laRutaViejaDeServiciosDevuelveLoMismoQueLaNueva() throws Exception {
        assertThat(cuerpo("/api/public/barberias/jade/servicios"))
                .isEqualTo(cuerpo("/api/public/negocios/jade/servicios"));
    }

    @Test
    void laRutaViejaConUnSlugInexistenteSigueDevolviendo404() throws Exception {
        mvc.perform(get("/api/public/barberias/no-existe")).andExpect(status().isNotFound());
    }

    @Test
    void disponibilidadAceptaElParametroViejoBarbero() throws Exception {
        String base = "/api/public/disponibilidad?sucursal=" + SUCURSAL + "&servicio=" + SERVICIO_CORTE
                + "&fecha=" + lunes;
        assertThat(cuerpo(base + "&barbero=" + LUCAS)).isEqualTo(cuerpo(base + "&profesional=" + LUCAS));
    }

    @Test
    void reservarAceptaElCampoViejoBarberoId() throws Exception {
        mvc.perform(post("/api/public/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sucursalId":"%s","servicioId":"%s","barberoId":"%s","fecha":"%s","hora":"10:00",
                                 "clienteNombre":"Cliente Viejo","clienteTelefono":"+5493584990064"}
                                """.formatted(SUCURSAL, SERVICIO_CORTE, LUCAS, lunes)))
                .andExpect(status().isCreated());
    }

    // ---------- Tokens emitidos antes del deploy ----------

    /** Un token como los que se emitían antes de V7: claims "barberia" y "barbero", rol BARBERO. */
    private String tokenViejoDeLucas() {
        UUID usuarioLucas = usuarios.findByEmailIgnoreCase("lucas@jade.com").orElseThrow().getId();
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("barberia")
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(3600))
                .subject(usuarioLucas.toString())
                .claim("barberia", NEGOCIO_JADE)
                .claim("rol", "BARBERO")
                .claim("barbero", LUCAS)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String tokenNuevoDeLucas() throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"lucas@jade.com\",\"password\":\"barbero123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    @Test
    void unTokenViejoSigueAndandoYQuedaComoProfesional() throws Exception {
        mvc.perform(get("/api/auth/yo").header("Authorization", tokenViejoDeLucas()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("PROFESIONAL"))
                .andExpect(jsonPath("$.profesionalId").value(LUCAS));
    }

    @Test
    void conUnTokenViejoLosPermisosYElFiltroPorProfesionalSonLosMismos() throws Exception {
        // Reportes: pide rol ADMIN o PROFESIONAL y, para un profesional, filtra por el claim del token
        String url = "/api/reportes?desde=" + lunes.minusDays(30) + "&hasta=" + lunes;
        String conTokenViejo = mvc.perform(get(url).header("Authorization", tokenViejoDeLucas()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String conTokenNuevo = mvc.perform(get(url).header("Authorization", tokenNuevoDeLucas()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(conTokenViejo).isEqualTo(conTokenNuevo);
    }

    @Test
    void unTokenNuevoUsaLosClaimsNuevos() throws Exception {
        String token = tokenNuevoDeLucas().substring("Bearer ".length());
        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        assertThat(payload)
                .contains("\"negocio\"", "\"profesional\"", "\"PROFESIONAL\"")
                .doesNotContain("\"barbero\"", "\"BARBERO\"");
    }

    // ---------- Migración V7 ----------

    @Test
    void noQuedanUsuariosConElRolViejo() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE rol = 'BARBERO'", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE rol = 'PROFESIONAL'", Long.class))
                .isPositive();
    }

    @Test
    void laBaseYaNoAceptaElRolViejo() {
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE usuario SET rol = 'BARBERO' WHERE email = 'lucas@jade.com'"))
                .hasMessageContaining("chk_usuario_rol");
    }

    @Test
    void noQuedanTablasNiColumnasConNombresViejos() {
        Long viejos = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND (table_name LIKE '%barber%' OR column_name LIKE '%barber%')
                """, Long.class);
        assertThat(viejos).isZero();
    }
}

package com.innovax.barberia.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Usa los usuarios del seed de dev (necesita la base levantada). */
@SpringBootTest
class AuthIntegrationTest {

    private static final String BARBERIA_JADE = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String LUCAS = "cccccccc-0000-0000-0000-000000000001";
    private static final String ANDRES = "cccccccc-0000-0000-0000-000000000002";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- Login ----------

    @Test
    void loginCorrectoDevuelveTokenYDatos() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"LUCAS@jade.com\",\"password\":\"barbero123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.rol").value("BARBERO"))
                .andExpect(jsonPath("$.usuario.barberiaId").value(BARBERIA_JADE))
                .andExpect(jsonPath("$.usuario.barberoId").value(LUCAS));
    }

    @Test
    void contrasenaIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@jade.com\",\"password\":\"mal\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Email o contraseña incorrectos"));
    }

    @Test
    void emailInexistenteDevuelveElMismo401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@jade.com\",\"password\":\"admin123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Email o contraseña incorrectos"));
    }

    @Test
    void loginSinDatosDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.password").exists());
    }

    // ---------- Token ----------

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/auth/yo")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        mvc.perform(get("/api/auth/yo").header("Authorization", "Bearer esto-no-es-un-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenDevuelveElUsuario() throws Exception {
        String token = login("admin@jade.com", "admin123");
        mvc.perform(get("/api/auth/yo").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@jade.com"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    // ---------- Roles ----------

    @Test
    void adminPuedeEntrarAEndpointDeAdmin() throws Exception {
        String token = login("admin@jade.com", "admin123");
        mvc.perform(get("/api/test-permisos/solo-admin").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void barberoNoPuedeEntrarAEndpointDeAdmin() throws Exception {
        String token = login("lucas@jade.com", "barbero123");
        mvc.perform(get("/api/test-permisos/solo-admin").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    // ---------- Barbero: solo lo suyo ----------

    @Test
    void barberoPuedeOperarSobreSiMismo() throws Exception {
        String token = login("lucas@jade.com", "barbero123");
        mvc.perform(get("/api/test-permisos/barbero/" + LUCAS).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void barberoNoPuedeOperarSobreOtroBarbero() throws Exception {
        String token = login("lucas@jade.com", "barbero123");
        mvc.perform(get("/api/test-permisos/barbero/" + ANDRES).header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeOperarSobreCualquierBarbero() throws Exception {
        String token = login("admin@jade.com", "admin123");
        mvc.perform(get("/api/test-permisos/barbero/" + ANDRES).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    // ---------- Aislamiento por barbería ----------

    @Test
    void laBarberiaSaleDelToken() throws Exception {
        String token = login("lucas@jade.com", "barbero123");
        mvc.perform(get("/api/test-permisos/mi-barberia").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(content().string(BARBERIA_JADE));
    }
}

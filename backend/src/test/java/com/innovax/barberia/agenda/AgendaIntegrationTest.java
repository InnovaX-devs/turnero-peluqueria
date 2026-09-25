package com.innovax.barberia.agenda;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Usa los usuarios del seed de dev (necesita la base levantada). */
@SpringBootTest
class AgendaIntegrationTest {

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

    @Test
    void sinTokenDevuelve401() throws Exception {
        LocalDate hoy = LocalDate.now();
        mvc.perform(get("/api/agenda").param("desde", hoy.toString()).param("hasta", hoy.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminVeLaAgendaSinFiltrarPorBarbero() throws Exception {
        String token = login("admin@jade.com", "admin123");
        LocalDate hoy = LocalDate.now();
        mvc.perform(get("/api/agenda")
                        .param("desde", hoy.toString())
                        .param("hasta", hoy.toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos").isArray())
                .andExpect(jsonPath("$.bloqueos").isArray())
                .andExpect(jsonPath("$.barberos", org.hamcrest.Matchers.hasSize(3)));
    }

    @Test
    void adminPuedeFiltrarPorUnBarberoPuntual() throws Exception {
        String token = login("admin@jade.com", "admin123");
        LocalDate hoy = LocalDate.now();
        mvc.perform(get("/api/agenda")
                        .param("desde", hoy.toString())
                        .param("hasta", hoy.toString())
                        .param("barberoId", ANDRES)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void barberoVeSuAgendaAunquePidaOtroBarberoEnElFiltro() throws Exception {
        String token = login("lucas@jade.com", "barbero123");
        LocalDate hoy = LocalDate.now();
        // Lucas pide el barberoId de Andrés: el backend lo ignora y devuelve solo lo de Lucas.
        mvc.perform(get("/api/agenda")
                        .param("desde", hoy.toString())
                        .param("hasta", hoy.toString())
                        .param("barberoId", ANDRES)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos[*].barberoId").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.equalTo(LUCAS))));
    }

    @Test
    void hastaAnteriorADesdeDevuelve400() throws Exception {
        String token = login("admin@jade.com", "admin123");
        LocalDate hoy = LocalDate.now();
        mvc.perform(get("/api/agenda")
                        .param("desde", hoy.toString())
                        .param("hasta", hoy.minusDays(1).toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.hasta").exists());
    }

    @Test
    void faltanParametrosObligatoriosDevuelve400() throws Exception {
        String token = login("admin@jade.com", "admin123");
        mvc.perform(get("/api/agenda").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }
}
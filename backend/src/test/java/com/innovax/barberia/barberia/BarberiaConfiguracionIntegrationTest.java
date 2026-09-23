package com.innovax.barberia.barberia;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Usa los datos del seed de dev (necesita la base levantada).
 * Es @Transactional: cada test se revierte al terminar y no deja cambios en la base.
 */
@SpringBootTest
@Transactional
class BarberiaConfiguracionIntegrationTest {

    private static final String BARBERIA_JADE = "aaaaaaaa-0000-0000-0000-000000000001";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private BarberiaRepository barberias;
    @PersistenceContext
    private EntityManager em;

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
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private String admin() throws Exception {
        return login("admin@jade.com", "admin123");
    }

    /** Body válido; cada test pisa lo que le interesa. */
    private static String body(String slug, boolean reservasOnline, int minMin, int maxDias, int intervalo) {
        return """
                {"nombre":"Jade","slug":"%s","logoUrl":null,"reservasOnline":%s,
                 "anticipacionMinimaMin":%d,"anticipacionMaximaDias":%d,"intervaloSlotsMin":%d}
                """.formatted(slug, reservasOnline, minMin, maxDias, intervalo);
    }

    // ---------- Permisos ----------

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/barberia")).andExpect(status().isUnauthorized());
    }

    @Test
    void barberoNoPuedeVerNiEditarLaConfiguracion() throws Exception {
        String token = login("lucas@jade.com", "barbero123");
        mvc.perform(get("/api/barberia").header("Authorization", token))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/barberia").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("jade", true, 60, 30, 30)))
                .andExpect(status().isForbidden());
    }

    // ---------- Lectura / edición ----------

    @Test
    void adminVeLaConfiguracionDeSuBarberia() throws Exception {
        mvc.perform(get("/api/barberia").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BARBERIA_JADE))
                .andExpect(jsonPath("$.slug").value("jade"))
                .andExpect(jsonPath("$.reservasOnline").value(true))
                .andExpect(jsonPath("$.intervaloSlotsMin").value(30));
    }

    @Test
    void adminEditaLaConfiguracion() throws Exception {
        String token = admin();
        String nuevo = """
                {"nombre":"  Jade Barber Club ","slug":"jade-club","logoUrl":"https://ejemplo.com/logo.png",
                 "reservasOnline":false,"anticipacionMinimaMin":120,"anticipacionMaximaDias":45,"intervaloSlotsMin":15}
                """;
        mvc.perform(put("/api/barberia").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(nuevo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Jade Barber Club"))   // se recorta
                .andExpect(jsonPath("$.slug").value("jade-club"))
                .andExpect(jsonPath("$.logoUrl").value("https://ejemplo.com/logo.png"))
                .andExpect(jsonPath("$.reservasOnline").value(false))
                .andExpect(jsonPath("$.anticipacionMinimaMin").value(120))
                .andExpect(jsonPath("$.anticipacionMaximaDias").value(45))
                .andExpect(jsonPath("$.intervaloSlotsMin").value(15));

        mvc.perform(get("/api/barberia").header("Authorization", token))
                .andExpect(jsonPath("$.slug").value("jade-club"))
                .andExpect(jsonPath("$.reservasOnline").value(false));
    }

    @Test
    void puedeGuardarConSuPropioSlugSinCambiarlo() throws Exception {
        mvc.perform(put("/api/barberia").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(body("jade", true, 60, 30, 30)))
                .andExpect(status().isOk());
    }

    // ---------- Slug ----------

    @Test
    void slugDeOtraBarberiaDevuelve409() throws Exception {
        Barberia otra = new Barberia();
        otra.setNombre("Otra");
        otra.setSlug("otra-barberia");
        barberias.saveAndFlush(otra);

        mvc.perform(put("/api/barberia").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(body("otra-barberia", true, 60, 30, 30)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.slug").exists());
    }

    @Test
    void slugNoAptoParaUrlDevuelve400() throws Exception {
        for (String malo : new String[]{"Mi Barberia", "mi_barberia", "mi--barberia", "-jade", "jade-", "barbería", "ab"}) {
            mvc.perform(put("/api/barberia").header("Authorization", admin())
                            .contentType(MediaType.APPLICATION_JSON).content(body(malo, true, 60, 30, 30)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.slug").exists());
        }
    }

    // ---------- Otras validaciones ----------

    @Test
    void intervaloQueNoEs15o30Devuelve400() throws Exception {
        for (int malo : new int[]{0, 10, 20, 60}) {
            mvc.perform(put("/api/barberia").header("Authorization", admin())
                            .contentType(MediaType.APPLICATION_JSON).content(body("jade", true, 60, 30, malo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.intervaloSlotsMin").exists());
        }
    }

    @Test
    void anticipacionMinimaTieneQueSerMenorQueLaMaxima() throws Exception {
        // máxima = 1 día = 1440 min; mínima de 1440 ya no deja ningún horario reservable
        mvc.perform(put("/api/barberia").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(body("jade", true, 1440, 1, 30)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.anticipacionMinimaMin").exists());
    }

    @Test
    void valoresFueraDeRangoDevuelven400() throws Exception {
        mvc.perform(put("/api/barberia").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(body("jade", true, -5, 0, 30)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.anticipacionMinimaMin").exists())
                .andExpect(jsonPath("$.errores.anticipacionMaximaDias").exists());
    }

    @Test
    void faltarElFlagNoLoInterpretaComoFalse() throws Exception {
        String sinFlag = """
                {"nombre":"Jade","slug":"jade","anticipacionMinimaMin":60,"anticipacionMaximaDias":30,"intervaloSlotsMin":30}
                """;
        mvc.perform(put("/api/barberia").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(sinFlag))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.reservasOnline").exists());
    }

    // ---------- Al desactivar, los turnos existentes se mantienen ----------

    @Test
    void desactivarReservasOnlineNoTocaLosTurnosYaReservados() throws Exception {
        em.createNativeQuery("""
                INSERT INTO atencion (barberia_id, sucursal_id, barbero_id, servicio_id,
                                      inicio, fin, estado, origen, precio_final)
                VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000001',
                        'cccccccc-0000-0000-0000-000000000003', 'dddddddd-0000-0000-0000-000000000001',
                        now() + interval '400 days', now() + interval '400 days 30 minutes',
                        'RESERVADA', 'ONLINE', 1500000)
                """).executeUpdate();

        mvc.perform(put("/api/barberia").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(body("jade", false, 60, 30, 30)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservasOnline").value(false));

        Object estado = em.createNativeQuery("""
                SELECT estado FROM atencion
                WHERE barberia_id = 'aaaaaaaa-0000-0000-0000-000000000001'
                  AND inicio > now() + interval '399 days'
                """).getSingleResult();
        assertThat(estado).isEqualTo("RESERVADA");
    }
}
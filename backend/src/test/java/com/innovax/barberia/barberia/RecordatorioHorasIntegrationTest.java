package com.innovax.barberia.barberia;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Las horas de anticipación del recordatorio en la configuración de la barbería. */
@SpringBootTest
@Transactional
class RecordatorioHorasIntegrationTest {

    private static final String BASE = """
            "nombre":"Jade","slug":"jade","logoUrl":null,"reservasOnline":true,
            "anticipacionMinimaMin":60,"anticipacionMaximaDias":30,"intervaloSlotsMin":30""";

    @Autowired private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String admin() throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@jade.com\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    @Test
    void porDefectoSonVeinticuatroHoras() throws Exception {
        mvc.perform(get("/api/barberia").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordatorioHorasAntes").value(24));
    }

    @Test
    void elAdminLaCambia() throws Exception {
        String token = admin();
        mvc.perform(put("/api/barberia").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + BASE + ",\"recordatorioHorasAntes\":48}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordatorioHorasAntes").value(48));
        mvc.perform(get("/api/barberia").header("Authorization", token))
                .andExpect(jsonPath("$.recordatorioHorasAntes").value(48));
    }

    @Test
    void siNoVieneSeMantieneLaQueEstaba() throws Exception {
        String token = admin();
        mvc.perform(put("/api/barberia").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{" + BASE + ",\"recordatorioHorasAntes\":12}")).andExpect(status().isOk());

        mvc.perform(put("/api/barberia").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + BASE + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordatorioHorasAntes").value(12));
    }

    @Test
    void fueraDeRangoDevuelve400() throws Exception {
        String token = admin();
        mvc.perform(put("/api/barberia").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + BASE + ",\"recordatorioHorasAntes\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.recordatorioHorasAntes").exists());
        mvc.perform(put("/api/barberia").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + BASE + ",\"recordatorioHorasAntes\":169}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.recordatorioHorasAntes").exists());
    }
}
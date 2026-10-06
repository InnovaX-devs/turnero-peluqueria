package com.innovax.barberia.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** "Mi cuenta": cada usuario cambia su propia contraseña. @Transactional: los cambios se revierten. */
@SpringBootTest
@Transactional
class CambiarPasswordIntegrationTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private String token(String email, String password) throws Exception {
        String body = login(email, password).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private ResultActions cambiar(String token, String actual, String nueva) throws Exception {
        return mvc.perform(put("/api/auth/password").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actual\":\"%s\",\"nueva\":\"%s\"}".formatted(actual, nueva)));
    }

    @Test
    void elAdminCambiaSuContrasenaYEntraConLaNueva() throws Exception {
        cambiar(token("admin@jade.com", "admin123"), "admin123", "otraClave2026")
                .andExpect(status().isNoContent());

        login("admin@jade.com", "admin123").andExpect(status().isUnauthorized());
        login("admin@jade.com", "otraClave2026").andExpect(status().isOk());
    }

    @Test
    void unBarberoTambienPuedeCambiarLaSuya() throws Exception {
        cambiar(token("lucas@jade.com", "barbero123"), "barbero123", "lucasNueva1")
                .andExpect(status().isNoContent());
        login("lucas@jade.com", "lucasNueva1").andExpect(status().isOk());
    }

    @Test
    void conLaContrasenaActualEquivocadaNoLaCambia() throws Exception {
        cambiar(token("admin@jade.com", "admin123"), "noEsEsta", "otraClave2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.actual").value("La contraseña actual no es correcta"));
        login("admin@jade.com", "admin123").andExpect(status().isOk());
    }

    @Test
    void laNuevaTieneQueTenerAlMenos8Caracteres() throws Exception {
        cambiar(token("admin@jade.com", "admin123"), "admin123", "corta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nueva").exists());
    }

    @Test
    void laNuevaTieneQueSerDistintaDeLaActual() throws Exception {
        cambiar(token("admin@jade.com", "admin123"), "admin123", "admin123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nueva").value("Tiene que ser distinta de la actual"));
    }

    @Test
    void sinSesionDevuelve401() throws Exception {
        mvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"admin123\",\"nueva\":\"otraClave2026\"}"))
                .andExpect(status().isUnauthorized());
    }
}

package com.innovax.barberia.barberia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Página pública de reserva: GET /api/public/barberias/{slug}.
 * Usa los datos del seed de dev (necesita la base levantada). @Transactional: no deja cambios.
 */
@SpringBootTest
@Transactional
class BarberiaPublicaIntegrationTest {

    private static final UUID BARBERIA_JADE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private BarberiaRepository barberias;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void devuelveLaBarberiaSinLogin() throws Exception {
        mvc.perform(get("/api/public/barberias/jade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Jade"))
                .andExpect(jsonPath("$.reservasOnline").value(true))
                .andExpect(jsonPath("$.sucursales", hasSize(1)))
                .andExpect(jsonPath("$.sucursales[0].telefono").value("+5493584000000"))
                // no expone configuración interna
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.anticipacionMinimaMin").doesNotExist());
    }

    @Test
    void ignoraMayusculasEnElSlug() throws Exception {
        mvc.perform(get("/api/public/barberias/JADE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("jade"));
    }

    @Test
    void conReservasOnlineApagadasIgualDevuelveElContacto() throws Exception {
        Barberia jade = barberias.findById(BARBERIA_JADE).orElseThrow();
        jade.setReservasOnline(false);
        barberias.saveAndFlush(jade);

        mvc.perform(get("/api/public/barberias/jade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservasOnline").value(false))
                .andExpect(jsonPath("$.sucursales[0].direccion").value("Sobremonte 240"));
    }

    @Test
    void slugInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/public/barberias/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Barbería no encontrada"));
    }
}
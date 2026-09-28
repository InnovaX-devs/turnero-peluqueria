package com.innovax.barberia.barbero;

import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.barberia.BarberiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Barberos de la reserva pública: GET /api/public/barberias/{slug}/barberos.
 * Usa los datos del seed de dev (necesita la base levantada). @Transactional: no deja cambios.
 */
@SpringBootTest
@Transactional
class BarberoPublicoIntegrationTest {

    private static final UUID BARBERIA_JADE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final String SUCURSAL = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final UUID SERVICIO_BARBA = UUID.fromString("dddddddd-0000-0000-0000-000000000002");
    private static final UUID LUCAS = UUID.fromString("cccccccc-0000-0000-0000-000000000001");

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private BarberoRepository barberos;
    @Autowired
    private BarberiaRepository barberias;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private ResultActions listar(String servicio) throws Exception {
        return mvc.perform(get("/api/public/barberias/jade/barberos")
                .param("sucursal", SUCURSAL)
                .param("servicio", servicio));
    }

    @Test
    void devuelveLosBarberosOrdenadosConSuProximoDiaSinLogin() throws Exception {
        listar(SERVICIO_BARBA.toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].nombre").value("Andres"))
                .andExpect(jsonPath("$[1].nombre").value("Chapa"))
                .andExpect(jsonPath("$[2].nombre").value("Lucas"))
                // el seed tiene horarios de lunes a sábado: siempre hay un día libre en la próxima semana
                .andExpect(jsonPath("$[0].proximoDiaDisponible", notNullValue()));
    }

    @Test
    void noMuestraBarberosQueNoHacenEseServicio() throws Exception {
        Barbero lucas = barberos.findById(LUCAS).orElseThrow();
        lucas.getServicios().removeIf(s -> s.getId().equals(SERVICIO_BARBA));
        barberos.saveAndFlush(lucas);

        listar(SERVICIO_BARBA.toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void noMuestraBarberosInactivos() throws Exception {
        Barbero lucas = barberos.findById(LUCAS).orElseThrow();
        lucas.setActivo(false);
        barberos.saveAndFlush(lucas);

        listar(SERVICIO_BARBA.toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void servicioInexistenteDevuelve404() throws Exception {
        listar(UUID.randomUUID().toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Servicio no encontrado"));
    }

    @Test
    void conReservasOnlineApagadasDevuelve400() throws Exception {
        Barberia jade = barberias.findById(BARBERIA_JADE).orElseThrow();
        jade.setReservasOnline(false);
        barberias.saveAndFlush(jade);

        listar(SERVICIO_BARBA.toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.barberia").exists());
    }
}

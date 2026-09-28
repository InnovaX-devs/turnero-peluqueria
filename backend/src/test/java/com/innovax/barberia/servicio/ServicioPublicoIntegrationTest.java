package com.innovax.barberia.servicio;

import com.innovax.barberia.barbero.Barbero;
import com.innovax.barberia.barbero.BarberoRepository;
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
 * Servicios de la reserva pública: GET /api/public/barberias/{slug}/servicios.
 * Usa los datos del seed de dev (necesita la base levantada). @Transactional: no deja cambios.
 */
@SpringBootTest
@Transactional
class ServicioPublicoIntegrationTest {

    private static final String SUCURSAL = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final UUID SERVICIO_BARBA = UUID.fromString("dddddddd-0000-0000-0000-000000000002");

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private ServicioRepository servicios;
    @Autowired
    private BarberoRepository barberos;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void devuelveLosServiciosOrdenadosSinLogin() throws Exception {
        mvc.perform(get("/api/public/barberias/jade/servicios").param("sucursal", SUCURSAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].nombre").value("Corte"))
                .andExpect(jsonPath("$[0].duracionMin").value(30))
                .andExpect(jsonPath("$[0].precio").value(1500000))
                .andExpect(jsonPath("$[1].nombre").value("Barba"))
                .andExpect(jsonPath("$[2].nombre").value("Corte y Barba"));
    }

    @Test
    void sinSucursalDevuelveTodosLosActivos() throws Exception {
        mvc.perform(get("/api/public/barberias/jade/servicios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void noMuestraServiciosDesactivados() throws Exception {
        Servicio barba = servicios.findById(SERVICIO_BARBA).orElseThrow();
        barba.setActivo(false);
        servicios.saveAndFlush(barba);

        mvc.perform(get("/api/public/barberias/jade/servicios").param("sucursal", SUCURSAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].nombre").value("Corte y Barba"));
    }

    @Test
    void noMuestraServiciosQueNingunBarberoHaceEnEsaSucursal() throws Exception {
        for (Barbero b : barberos.findAll()) {
            b.getServicios().removeIf(s -> s.getId().equals(SERVICIO_BARBA));
            barberos.save(b);
        }
        barberos.flush();

        mvc.perform(get("/api/public/barberias/jade/servicios").param("sucursal", SUCURSAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void sucursalDeOtraBarberiaOInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/public/barberias/jade/servicios").param("sucursal", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Sucursal no encontrada"));
    }

    @Test
    void slugInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/public/barberias/no-existe/servicios"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Barbería no encontrada"));
    }
}

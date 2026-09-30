package com.innovax.barberia.agenda;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.OrigenAtencion;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.barbero.BarberoRepository;
import com.innovax.barberia.servicio.ServicioRepository;
import com.innovax.barberia.sucursal.SucursalRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

/** Usa los datos del seed de dev (necesita la base levantada). */
@SpringBootTest
class CambiarEstadoTurnoIntegrationTest {

    private static final String BARBERIA = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String SUCURSAL = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final String BARBERO_LUCAS = "cccccccc-0000-0000-0000-000000000001";
    private static final String BARBERO_ANDRES = "cccccccc-0000-0000-0000-000000000002";
    private static final String SERVICIO_CORTE = "dddddddd-0000-0000-0000-000000000001"; // precio 1500000

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private AtencionRepository atenciones;
    @Autowired
    private BarberiaRepository barberias;
    @Autowired
    private SucursalRepository sucursales;
    @Autowired
    private ServicioRepository servicios;
    @Autowired
    private BarberoRepository barberos;

    private MockMvc mvc;
    private UUID turnoId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void tearDown() {
        if (turnoId != null) {
            atenciones.deleteById(turnoId);
        }
    }

    private UUID crearTurnoReservado(String barberoId) {
        Atencion atencion = new Atencion();
        atencion.setBarberia(barberias.getReferenceById(UUID.fromString(BARBERIA)));
        atencion.setSucursal(sucursales.getReferenceById(UUID.fromString(SUCURSAL)));
        atencion.setBarbero(barberos.getReferenceById(UUID.fromString(barberoId)));
        atencion.setServicio(servicios.getReferenceById(UUID.fromString(SERVICIO_CORTE)));
        Instant inicio = Instant.now().plus(2, ChronoUnit.DAYS);
        atencion.setInicio(inicio);
        atencion.setFin(inicio.plus(30, ChronoUnit.MINUTES));
        atencion.setEstado(EstadoAtencion.RESERVADA);
        atencion.setOrigen(OrigenAtencion.MANUAL);
        atencion.setPrecioFinal(1_500_000);
        Atencion guardada = atenciones.saveAndFlush(atencion);
        turnoId = guardada.getId();
        return turnoId;
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
        UUID id = crearTurnoReservado(BARBERO_LUCAS);
        mvc.perform(patch("/api/agenda/turnos/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminPuedeCompletarYAjustarPrecioYMetodoPago() throws Exception {
        UUID id = crearTurnoReservado(BARBERO_LUCAS);
        String token = login("admin@jade.com", "admin123");

        mvc.perform(patch("/api/agenda/turnos/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content("{\"estado\":\"COMPLETADA\",\"precioFinal\":1600000,\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.metodoPago").value("EFECTIVO"))
                .andExpect(jsonPath("$.precioFinal").value(1600000))
                .andExpect(jsonPath("$.estadoCambiadoPorEmail").value("admin@jade.com"))
                .andExpect(jsonPath("$.estadoCambiadoEn").exists());
    }

    @Test
    void completarSinMetodoPagoDevuelve400() throws Exception {
        UUID id = crearTurnoReservado(BARBERO_LUCAS);
        String token = login("admin@jade.com", "admin123");

        mvc.perform(patch("/api/agenda/turnos/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content("{\"estado\":\"COMPLETADA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.metodoPago").exists());
    }

    @Test
    void marcarAusenteNoRequiereMetodoPago() throws Exception {
        UUID id = crearTurnoReservado(BARBERO_LUCAS);
        String token = login("admin@jade.com", "admin123");

        mvc.perform(patch("/api/agenda/turnos/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content("{\"estado\":\"AUSENTE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("AUSENTE"));
    }

    @Test
    void turnoYaActualizadoDevuelve409() throws Exception {
        UUID id = crearTurnoReservado(BARBERO_LUCAS);
        String token = login("admin@jade.com", "admin123");

        mvc.perform(patch("/api/agenda/turnos/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/agenda/turnos/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content("{\"estado\":\"AUSENTE\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void barberoNoPuedeCambiarEstadoDeOtroBarbero() throws Exception {
        UUID id = crearTurnoReservado(BARBERO_ANDRES);
        String token = login("lucas@jade.com", "barbero123");

        mvc.perform(patch("/api/agenda/turnos/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void barberoPuedeCambiarEstadoDeSuPropioTurno() throws Exception {
        UUID id = crearTurnoReservado(BARBERO_LUCAS);
        String token = login("lucas@jade.com", "barbero123");

        mvc.perform(patch("/api/agenda/turnos/{id}/estado", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }
}
package com.innovax.barberia.agenda;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.OrigenAtencion;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.profesional.ProfesionalRepository;
import com.innovax.barberia.servicio.ServicioRepository;
import com.innovax.barberia.sucursal.SucursalRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sala de espera (issue #71): marcar que el cliente llegó. Usa el seed de dev; cada test borra su turno.
 */
@SpringBootTest
class SalaDeEsperaIntegrationTest {

    private static final String NEGOCIO = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String SUCURSAL = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final String LUCAS = "cccccccc-0000-0000-0000-000000000001";
    private static final String ANDRES = "cccccccc-0000-0000-0000-000000000002";
    private static final String CORTE = "dddddddd-0000-0000-0000-000000000001";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private AtencionRepository atenciones;
    @Autowired
    private NegocioRepository negocios;
    @Autowired
    private SucursalRepository sucursales;
    @Autowired
    private ServicioRepository servicios;
    @Autowired
    private ProfesionalRepository profesionales;

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

    private UUID crearTurno(String profesionalId, EstadoAtencion estado) {
        Atencion atencion = new Atencion();
        atencion.setNegocio(negocios.getReferenceById(UUID.fromString(NEGOCIO)));
        atencion.setSucursal(sucursales.getReferenceById(UUID.fromString(SUCURSAL)));
        atencion.setProfesional(profesionales.getReferenceById(UUID.fromString(profesionalId)));
        atencion.setServicio(servicios.getReferenceById(UUID.fromString(CORTE)));
        Instant inicio = Instant.now().plus(3, ChronoUnit.DAYS);
        atencion.setInicio(inicio);
        atencion.setFin(inicio.plus(30, ChronoUnit.MINUTES));
        atencion.setEstado(estado);
        atencion.setOrigen(OrigenAtencion.MANUAL);
        atencion.setPrecioFinal(1_500_000);
        turnoId = atenciones.saveAndFlush(atencion).getId();
        return turnoId;
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private ResultActions marcar(String token, UUID id, boolean llego) throws Exception {
        return mvc.perform(patch("/api/agenda/turnos/{id}/llegada", id)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"llego\":" + llego + "}"));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        UUID id = crearTurno(LUCAS, EstadoAtencion.RESERVADA);
        mvc.perform(patch("/api/agenda/turnos/{id}/llegada", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"llego\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminMarcaYDesmarcaLaLlegadaSinCambiarElEstado() throws Exception {
        UUID id = crearTurno(LUCAS, EstadoAtencion.RESERVADA);
        String admin = login("admin@jade.com", "admin123");

        marcar(admin, id, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESERVADA"))
                .andExpect(jsonPath("$.llegadaEn").exists());
        assertThat(atenciones.findById(id).orElseThrow().getLlegadaEn()).isNotNull();

        marcar(admin, id, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESERVADA"))
                .andExpect(jsonPath("$.llegadaEn").value(nullValue()));
        assertThat(atenciones.findById(id).orElseThrow().getLlegadaEn()).isNull();
    }

    @Test
    void marcarDosVecesRespetaLaHoraDeLlegadaOriginal() throws Exception {
        UUID id = crearTurno(LUCAS, EstadoAtencion.RESERVADA);
        String admin = login("admin@jade.com", "admin123");

        marcar(admin, id, true).andExpect(status().isOk());
        Instant primera = atenciones.findById(id).orElseThrow().getLlegadaEn();
        marcar(admin, id, true).andExpect(status().isOk());

        assertThat(atenciones.findById(id).orElseThrow().getLlegadaEn()).isEqualTo(primera);
    }

    @Test
    void elProfesionalMarcaSusTurnosPeroNoLosDeOtro() throws Exception {
        String lucas = login("lucas@jade.com", "barbero123");

        UUID propio = crearTurno(LUCAS, EstadoAtencion.RESERVADA);
        marcar(lucas, propio, true).andExpect(status().isOk());
        atenciones.deleteById(propio);

        UUID ajeno = crearTurno(ANDRES, EstadoAtencion.RESERVADA);
        marcar(lucas, ajeno, true).andExpect(status().isForbidden());
    }

    @Test
    void noSeMarcaUnTurnoQueYaNoEstaReservado() throws Exception {
        UUID id = crearTurno(LUCAS, EstadoAtencion.CANCELADA);
        marcar(login("admin@jade.com", "admin123"), id, true).andExpect(status().isConflict());
    }

    @Test
    void faltaElCampoLlegoDevuelve400() throws Exception {
        UUID id = crearTurno(LUCAS, EstadoAtencion.RESERVADA);
        mvc.perform(patch("/api/agenda/turnos/{id}/llegada", id)
                        .header("Authorization", login("admin@jade.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}

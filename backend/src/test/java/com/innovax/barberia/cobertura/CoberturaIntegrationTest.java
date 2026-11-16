package com.innovax.barberia.cobertura;

import com.innovax.barberia.profesional.Profesional;
import com.innovax.barberia.profesional.ProfesionalRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Obras sociales y prepagas (issue #67): ABM, profesionales que las atienden, reserva pública filtrada,
 * cobro con copago, caja y reporte de facturación. Usa los datos del seed de dev; @Transactional: no deja cambios.
 */
@SpringBootTest
@Transactional
class CoberturaIntegrationTest {

    private static final String AUTH = "Authorization";
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    private static final String SUCURSAL = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final String CORTE = "dddddddd-0000-0000-0000-000000000001";
    private static final String LUCAS = "cccccccc-0000-0000-0000-000000000001";
    private static final String ANDRES = "cccccccc-0000-0000-0000-000000000002";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private ProfesionalRepository profesionales;
    @Autowired
    private CoberturaRepository coberturas;

    private MockMvc mvc;
    private String admin;
    private LocalDate martes;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        admin = login("admin@jade.com", "admin123");
        martes = LocalDate.now(ZONA).with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
    }

    // ---------- Helpers ----------

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private ResultActions crear(String token, String nombre) throws Exception {
        return mvc.perform(post("/api/coberturas").header(AUTH, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"%s\"}".formatted(nombre)));
    }

    private static String idDe(ResultActions r) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id");
    }

    /** PAMI creada por el admin y asignada solo a Andrés. */
    private String pamiSoloAndres() throws Exception {
        String pami = idDe(crear(admin, "PAMI").andExpect(status().isCreated()));
        Profesional andres = profesionales.findById(UUID.fromString(ANDRES)).orElseThrow();
        andres.getCoberturas().add(coberturas.findById(UUID.fromString(pami)).orElseThrow());
        profesionales.saveAndFlush(andres);
        return pami;
    }

    private ResultActions reservar(String profesionalId, String coberturaId, String telefono) throws Exception {
        return mvc.perform(post("/api/public/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sucursalId":"%s","servicioId":"%s","profesionalId":"%s","fecha":"%s","hora":"10:00",
                         "clienteNombre":"Paciente PAMI","clienteTelefono":"%s",
                         "coberturaId":%s,"numeroAfiliado":"15-123456-7","plan":"Único"}
                        """.formatted(SUCURSAL, CORTE, profesionalId, martes, telefono,
                        coberturaId == null ? "null" : "\"" + coberturaId + "\"")));
    }

    // ---------- ABM ----------

    @Test
    void elAdminCreaRenombraYDesactivaCoberturas() throws Exception {
        String id = idDe(crear(admin, "  Swiss   Medical ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Swiss Medical"))
                .andExpect(jsonPath("$.activa").value(true)));

        crear(admin, "SWISS MEDICAL")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.nombre").exists());

        mvc.perform(put("/api/coberturas/" + id).header(AUTH, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Swiss Medical SMG\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Swiss Medical SMG"));

        mvc.perform(patch("/api/coberturas/" + id + "/activa").header(AUTH, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activa\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));

        // Desactivada: no se ofrece en la página pública, pero sigue en el ABM
        mvc.perform(get("/api/public/negocios/jade/coberturas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/coberturas").header(AUTH, admin))
                .andExpect(jsonPath("$[*].nombre", contains("Swiss Medical SMG")));
    }

    @Test
    void cargarLasComunesNoDuplica() throws Exception {
        crear(admin, "pami").andExpect(status().isCreated());
        mvc.perform(post("/api/coberturas/comunes").header(AUTH, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(CoberturaService.COMUNES.size())))
                .andExpect(jsonPath("$[*].nombre", hasItem("OSDE")))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("PAMI"))));
        mvc.perform(post("/api/coberturas/comunes").header(AUTH, admin))
                .andExpect(jsonPath("$", hasSize(CoberturaService.COMUNES.size())));
    }

    @Test
    void unProfesionalPuedeVerlasPeroNoEditarlas() throws Exception {
        String lucas = login("lucas@jade.com", "barbero123");
        mvc.perform(get("/api/coberturas").header(AUTH, lucas)).andExpect(status().isOk());
        crear(lucas, "OSDE").andExpect(status().isForbidden());
        mvc.perform(get("/api/coberturas/reporte").header(AUTH, lucas)
                        .param("desde", martes.toString()).param("hasta", martes.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void elAdminAsignaCoberturasAlProfesional() throws Exception {
        String osde = idDe(crear(admin, "OSDE").andExpect(status().isCreated()));
        mvc.perform(put("/api/profesionales/" + LUCAS).header(AUTH, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Lucas","apellido":"Sanchez","sucursalIds":["%s"],"servicioIds":["%s"],
                                 "coberturaIds":["%s"]}
                                """.formatted(SUCURSAL, CORTE, osde)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coberturaIds", contains(osde)));

        mvc.perform(get("/api/profesionales/opciones").header(AUTH, admin))
                .andExpect(jsonPath("$.coberturas[*].nombre", contains("OSDE")));
    }

    // ---------- Reserva pública ----------

    @Test
    void conCoberturaSoloSeOfrecenLosProfesionalesQueLaAtienden() throws Exception {
        String pami = pamiSoloAndres();
        mvc.perform(get("/api/public/negocios/jade/profesionales")
                        .param("sucursal", SUCURSAL).param("servicio", CORTE).param("cobertura", pami))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(ANDRES)));

        // "Cualquier profesional": los horarios son solo de Andrés
        mvc.perform(get("/api/public/disponibilidad")
                        .param("sucursal", SUCURSAL).param("servicio", CORTE)
                        .param("cobertura", pami).param("fecha", martes.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[0].profesionales", contains(ANDRES)));

        // Lucas puntual con PAMI: sin horarios
        mvc.perform(get("/api/public/disponibilidad")
                        .param("sucursal", SUCURSAL).param("servicio", CORTE).param("profesional", LUCAS)
                        .param("cobertura", pami).param("fecha", martes.toString()))
                .andExpect(jsonPath("$.slots", hasSize(0)));
    }

    @Test
    void unPacienteConPamiNoPuedeReservarConUnProfesionalQueNoAtiendePami() throws Exception {
        String pami = pamiSoloAndres();
        reservar(LUCAS, pami, "+5493584990001")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.coberturaId").value("Lucas Sanchez no atiende PAMI"));

        // Como particular con Lucas sí puede
        reservar(LUCAS, null, "+5493584990002").andExpect(status().isCreated());
    }

    @Test
    void reservaConPamiCopagoCajaYReporte() throws Exception {
        String pami = pamiSoloAndres();
        String turno = idDe(reservar(ANDRES, pami, "+5493584990003").andExpect(status().isCreated()));

        mvc.perform(get("/api/agenda").header(AUTH, admin)
                        .param("desde", martes.toString()).param("hasta", martes.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos[?(@.id == '%s')].tipoPago".formatted(turno), contains("COBERTURA")))
                .andExpect(jsonPath("$.turnos[?(@.id == '%s')].coberturaNombre".formatted(turno), contains("PAMI")))
                .andExpect(jsonPath("$.turnos[?(@.id == '%s')].clienteNumeroAfiliado".formatted(turno), contains("15-123456-7")));

        // Con copago sin indicar el monto: error
        mvc.perform(patch("/api/agenda/turnos/" + turno + "/estado").header(AUTH, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"COMPLETADA\",\"tipoPago\":\"COBERTURA_CON_COPAGO\",\"metodoPago\":\"EFECTIVO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.copago").exists());

        mvc.perform(patch("/api/agenda/turnos/" + turno + "/estado").header(AUTH, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"COMPLETADA","tipoPago":"COBERTURA_CON_COPAGO","copago":250000,
                                 "metodoPago":"EFECTIVO","numeroAutorizacion":"A-778"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPago").value("COBERTURA_CON_COPAGO"))
                .andExpect(jsonPath("$.copago").value(250000))
                .andExpect(jsonPath("$.estadoPago").value("PAGADO"));

        // A la caja entra solo el copago
        mvc.perform(get("/api/caja").header(AUTH, admin)
                        .param("desde", martes.toString()).param("hasta", martes.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(250000));

        mvc.perform(get("/api/coberturas/reporte").header(AUTH, admin)
                        .param("desde", martes.toString()).param("hasta", martes.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(1))
                .andExpect(jsonPath("$.totalCopagos").value(250000))
                .andExpect(jsonPath("$.porCobertura[0].nombre").value("PAMI"))
                .andExpect(jsonPath("$.detalle[0].numeroAfiliado").value("15-123456-7"))
                .andExpect(jsonPath("$.detalle[0].numeroAutorizacion").value("A-778"));
    }
}

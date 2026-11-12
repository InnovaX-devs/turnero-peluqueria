package com.innovax.barberia.especialidad;

import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.negocio.Rubro;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matrícula y especialidades del profesional (issue #68): ABM en el panel y la reserva pública
 * "elegir especialidad → profesional (o cualquiera) → prestación → horario".
 * Usa los datos del seed de dev. @Transactional: no deja cambios (incluido el rubro de Jade).
 */
@SpringBootTest
@Transactional
class EspecialidadIntegrationTest {

    private static final String AUTH = "Authorization";
    private static final UUID NEGOCIO_JADE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final String SUCURSAL = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final String CORTE = "dddddddd-0000-0000-0000-000000000001";
    private static final String BARBA = "dddddddd-0000-0000-0000-000000000002";
    private static final String CORTE_Y_BARBA = "dddddddd-0000-0000-0000-000000000003";
    private static final String LUCAS = "cccccccc-0000-0000-0000-000000000001";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private NegocioRepository negocios;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
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

    private String admin() throws Exception {
        return login("admin@jade.com", "admin123");
    }

    private ResultActions crearEspecialidad(String token, String nombre) throws Exception {
        return mvc.perform(post("/api/especialidades").header(AUTH, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"%s\"}".formatted(nombre)));
    }

    private String idDe(ResultActions r) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id");
    }

    /** Edita a Lucas con los servicios, matrícula y especialidades dados. */
    private ResultActions editarLucas(String token, String servicios, String extra) throws Exception {
        return mvc.perform(put("/api/profesionales/" + LUCAS).header(AUTH, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Lucas","apellido":"Sanchez","sucursalIds":["%s"],"servicioIds":[%s]%s}
                        """.formatted(SUCURSAL, servicios, extra)));
    }

    private void conRubro(Rubro rubro) {
        Negocio jade = negocios.findById(NEGOCIO_JADE).orElseThrow();
        jade.setRubro(rubro);
        negocios.saveAndFlush(jade);
    }

    private static String q(String id) {
        return "\"" + id + "\"";
    }

    // ---------- ABM (panel) ----------

    @Test
    void elAdminCreaRenombraYBorraEspecialidades() throws Exception {
        String token = admin();
        String id = idDe(crearEspecialidad(token, "  Kinesiología   deportiva ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Kinesiología deportiva")));

        mvc.perform(get("/api/especialidades").header(AUTH, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", contains("Kinesiología deportiva")));

        mvc.perform(put("/api/especialidades/" + id).header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Kinesiología\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Kinesiología"));

        mvc.perform(delete("/api/especialidades/" + id).header(AUTH, token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/especialidades").header(AUTH, token))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void noPermiteNombresRepetidosSinImportarMayusculas() throws Exception {
        String token = admin();
        crearEspecialidad(token, "Kinesiología").andExpect(status().isCreated());
        crearEspecialidad(token, "KINESIOLOGÍA")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.nombre").exists());
    }

    @Test
    void unProfesionalNoPuedeAdministrarEspecialidades() throws Exception {
        String token = login("lucas@jade.com", "barbero123");
        mvc.perform(get("/api/especialidades").header(AUTH, token)).andExpect(status().isForbidden());
        crearEspecialidad(token, "Kinesiología").andExpect(status().isForbidden());
    }

    @Test
    void seLeAsignanMatriculaYEspecialidadesAlProfesional() throws Exception {
        String token = admin();
        String kine = idDe(crearEspecialidad(token, "Kinesiología"));

        editarLucas(token, q(CORTE), ",\"matricula\":\"  MP 1234 \",\"especialidadIds\":[%s]".formatted(q(kine)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricula").value("MP 1234"))
                .andExpect(jsonPath("$.especialidadIds", contains(kine)));

        // Un panel viejo que no manda los campos no borra lo cargado
        editarLucas(token, q(CORTE), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricula").value("MP 1234"))
                .andExpect(jsonPath("$.especialidadIds", contains(kine)));

        // Vacíos sí: se le quitan
        editarLucas(token, q(CORTE), ",\"matricula\":\"\",\"especialidadIds\":[]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricula", nullValue()))
                .andExpect(jsonPath("$.especialidadIds", hasSize(0)));

        mvc.perform(get("/api/profesionales/opciones").header(AUTH, token))
                .andExpect(jsonPath("$.especialidades[*].nombre", contains("Kinesiología")));
    }

    @Test
    void noAceptaEspecialidadesDeOtroNegocioNiInexistentes() throws Exception {
        editarLucas(admin(), q(CORTE), ",\"especialidadIds\":[%s]".formatted(q(UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.especialidadIds").exists());
    }

    // ---------- Reserva pública ----------

    @Test
    void elNegocioIndicaSiLaReservaArrancaPorEspecialidad() throws Exception {
        crearEspecialidad(admin(), "Kinesiología").andExpect(status().isCreated());

        mvc.perform(get("/api/public/negocios/jade"))
                .andExpect(jsonPath("$.reservaPorEspecialidad").value(false)); // es BARBERIA

        conRubro(Rubro.SALUD);
        mvc.perform(get("/api/public/negocios/jade"))
                .andExpect(jsonPath("$.reservaPorEspecialidad").value(true));
    }

    /** Criterio del issue: el paciente reserva buscando "Kinesiología" sin conocer el nombre del profesional. */
    @Test
    void elPacienteEncuentraProfesionalesYPrestacionesPorEspecialidad() throws Exception {
        conRubro(Rubro.SALUD);
        String token = admin();
        String kine = idDe(crearEspecialidad(token, "Kinesiología"));
        crearEspecialidad(token, "Traumatología"); // nadie la tiene: no se ofrece
        editarLucas(token, q(CORTE) + "," + q(CORTE_Y_BARBA),
                ",\"matricula\":\"MP 1234\",\"especialidadIds\":[%s]".formatted(q(kine)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/public/negocios/jade/especialidades").param("sucursal", SUCURSAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", contains("Kinesiología")));

        mvc.perform(get("/api/public/negocios/jade/profesionales")
                        .param("sucursal", SUCURSAL).param("especialidad", kine))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Lucas"))
                .andExpect(jsonPath("$[0].matricula").value("MP 1234"))
                .andExpect(jsonPath("$[0].especialidades", contains("Kinesiología")))
                .andExpect(jsonPath("$[0].proximoDiaDisponible", notNullValue()));

        // Prestaciones del profesional elegido, o de cualquiera de la especialidad
        mvc.perform(get("/api/public/negocios/jade/servicios")
                        .param("sucursal", SUCURSAL).param("profesional", LUCAS))
                .andExpect(jsonPath("$[*].nombre", contains("Corte", "Corte y Barba")));
        mvc.perform(get("/api/public/negocios/jade/servicios")
                        .param("sucursal", SUCURSAL).param("especialidad", kine))
                .andExpect(jsonPath("$[*].nombre", contains("Corte", "Corte y Barba")));
    }

    @Test
    void cualquierProfesionalSoloCuentaALosDeLaEspecialidad() throws Exception {
        conRubro(Rubro.SALUD);
        String token = admin();
        String kine = idDe(crearEspecialidad(token, "Kinesiología"));
        // Lucas es el único kinesiólogo y no hace Barba; Andrés y Chapa sí la hacen
        editarLucas(token, q(CORTE), ",\"especialidadIds\":[%s]".formatted(q(kine))).andExpect(status().isOk());

        LocalDate hoy = LocalDate.now(ZoneId.of("America/Argentina/Cordoba"));
        mvc.perform(get("/api/public/disponibilidad/dias")
                        .param("sucursal", SUCURSAL).param("servicio", BARBA)
                        .param("desde", hoy.toString()).param("hasta", hoy.plusDays(13).toString()))
                .andExpect(jsonPath("$.dias", not(empty())));
        mvc.perform(get("/api/public/disponibilidad/dias")
                        .param("sucursal", SUCURSAL).param("servicio", BARBA).param("especialidad", kine)
                        .param("desde", hoy.toString()).param("hasta", hoy.plusDays(13).toString()))
                .andExpect(jsonPath("$.dias", empty()));
    }

    @Test
    void profesionalesSinServicioNiEspecialidadDevuelve400() throws Exception {
        mvc.perform(get("/api/public/negocios/jade/profesionales").param("sucursal", SUCURSAL))
                .andExpect(status().isBadRequest());
    }
}

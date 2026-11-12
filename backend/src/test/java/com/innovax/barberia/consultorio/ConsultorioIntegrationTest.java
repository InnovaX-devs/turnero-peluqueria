package com.innovax.barberia.consultorio;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.profesional.ProfesionalRepository;
import com.innovax.barberia.sucursal.Sucursal;
import com.innovax.barberia.sucursal.SucursalRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Consultorios como recurso compartido (issue #69): ABM, validación de horarios entre profesionales,
 * disponibilidad con consultorio compartido y la regla "no se pueden generar dos turnos simultáneos en el
 * mismo consultorio" en todos los caminos (reserva online, panel, sobreturno, reprogramación y la base).
 *
 * <p>Usa los datos del seed de dev (lunes a sábado de 10 a 13 y de 16 a 21; turnos de 30 min).
 * @Transactional: no deja cambios.
 */
@SpringBootTest
@Transactional
class ConsultorioIntegrationTest {

    private static final String AUTH = "Authorization";
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    private static final UUID NEGOCIO_JADE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final String SUCURSAL = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final String CORTE = "dddddddd-0000-0000-0000-000000000001";
    private static final String CLIENTE = "eeeeeeee-0000-0000-0000-000000000001";
    private static final String LUCAS = "cccccccc-0000-0000-0000-000000000001";
    private static final String ANDRES = "cccccccc-0000-0000-0000-000000000002";
    private static final String CHAPA = "cccccccc-0000-0000-0000-000000000003";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private NegocioRepository negocios;
    @Autowired
    private SucursalRepository sucursales;
    @Autowired
    private ConsultorioRepository consultorios;
    @Autowired
    private AtencionRepository atenciones;
    @Autowired
    private ProfesionalRepository profesionales;

    private MockMvc mvc;
    private String admin;
    /** Siempre en el futuro y dentro de la anticipación máxima (30 días), y con más de 2 h para reprogramar. */
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

    private ResultActions crearConsultorio(String sucursalId, String nombre) throws Exception {
        return mvc.perform(post("/api/consultorios").header(AUTH, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sucursalId\":\"%s\",\"nombre\":\"%s\"}".formatted(sucursalId, nombre)));
    }

    private String consultorio(String nombre) throws Exception {
        return idDe(crearConsultorio(SUCURSAL, nombre).andExpect(status().isCreated()));
    }

    private static String idDe(ResultActions r) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id");
    }

    /** Un tramo de horario en JSON. {@code consultorioId} null = sin consultorio. */
    private static String tramo(int dia, String desde, String hasta, String consultorioId) {
        return """
                {"sucursalId":"%s","diaSemana":%d,"horaInicio":"%s","horaFin":"%s","consultorioId":%s}"""
                .formatted(SUCURSAL, dia, desde, hasta, consultorioId == null ? "null" : "\"" + consultorioId + "\"");
    }

    private ResultActions guardarHorarios(String profesionalId, String... tramos) throws Exception {
        return mvc.perform(put("/api/profesionales/" + profesionalId + "/horarios").header(AUTH, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"horarios\":[" + String.join(",", tramos) + "]}"));
    }

    private ResultActions disponibilidad(String profesionalId) throws Exception {
        return mvc.perform(get("/api/public/disponibilidad")
                .param("sucursal", SUCURSAL)
                .param("servicio", CORTE)
                .param("profesional", profesionalId)
                .param("fecha", martes.toString()));
    }

    private ResultActions reservarOnline(String profesionalId, String hora, String telefono) throws Exception {
        return mvc.perform(post("/api/public/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sucursalId":"%s","servicioId":"%s","profesionalId":"%s",
                         "fecha":"%s","hora":"%s","clienteNombre":"Paciente Test","clienteTelefono":"%s"}
                        """.formatted(SUCURSAL, CORTE, profesionalId, martes, hora, telefono)));
    }

    private ResultActions turnoPanel(String profesionalId, String hora, boolean sobreturno) throws Exception {
        return mvc.perform(post("/api/agenda/turnos").header(AUTH, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"clienteId":"%s","servicioId":"%s","profesionalId":"%s","fecha":"%s","hora":"%s","sobreturno":%s}
                        """.formatted(CLIENTE, CORTE, profesionalId, martes, hora, sobreturno)));
    }

    private Instant martesA(int hora, int minuto) {
        return ZonedDateTime.of(martes, LocalTime.of(hora, minuto), ZONA).toInstant();
    }

    /**
     * Escenario base del consultorio compartido: Lucas tenía el Consultorio 2 los martes de 10 a 13 y le dieron
     * un turno a las 10:00. Después el admin le pasa ese tramo del Consultorio 2 a Andrés (a Lucas lo deja solo
     * a la tarde). El turno de Lucas sigue en pie y sigue ocupando el Consultorio 2 de 10:00 a 10:30.
     *
     * @return el id del turno de Lucas
     */
    private String turnoDeLucasEnElConsultorioYTramoPasadoAAndres(String c2) throws Exception {
        guardarHorarios(LUCAS, tramo(2, "10:00", "13:00", c2), tramo(2, "16:00", "21:00", null))
                .andExpect(status().isOk());
        String turnoLucas = idDe(reservarOnline(LUCAS, "10:00", "+5493584770001").andExpect(status().isCreated()));

        guardarHorarios(LUCAS, tramo(2, "16:00", "21:00", null)).andExpect(status().isOk());
        guardarHorarios(ANDRES, tramo(2, "10:00", "13:00", c2), tramo(2, "16:00", "21:00", null))
                .andExpect(status().isOk());
        return turnoLucas;
    }

    // ---------- ABM ----------

    @Test
    void elAdminCreaRenombraDesactivaYReactivaConsultorios() throws Exception {
        String id = idDe(crearConsultorio(SUCURSAL, "  Consultorio   2 ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Consultorio 2"))
                .andExpect(jsonPath("$.sucursalId").value(SUCURSAL))
                .andExpect(jsonPath("$.activo").value(true)));

        // Mismo nombre en la misma sucursal, sin importar mayúsculas
        crearConsultorio(SUCURSAL, "consultorio 2")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.nombre").exists());

        mvc.perform(put("/api/consultorios/" + id).header(AUTH, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Box A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Box A"));

        mvc.perform(patch("/api/consultorios/" + id + "/activo").header(AUTH, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mvc.perform(get("/api/consultorios").param("incluirInactivos", "false").header(AUTH, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(id))));

        // Inactivo no aparece entre las opciones para asignar en los horarios
        mvc.perform(get("/api/profesionales/opciones").header(AUTH, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultorios[*].id", not(hasItem(id))));

        mvc.perform(patch("/api/consultorios/" + id + "/activo").header(AUTH, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":true}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/profesionales/opciones").header(AUTH, admin))
                .andExpect(jsonPath("$.consultorios[*].id", hasItem(id)));
    }

    @Test
    void unProfesionalNoPuedeAdministrarConsultorios() throws Exception {
        String lucas = login("lucas@jade.com", "barbero123");
        mvc.perform(get("/api/consultorios").header(AUTH, lucas))
                .andExpect(status().isForbidden());
    }

    @Test
    void noSePuedeDesactivarUnConsultorioQueEstaEnUnHorario() throws Exception {
        String c2 = consultorio("Consultorio 2");
        guardarHorarios(LUCAS, tramo(2, "10:00", "13:00", c2)).andExpect(status().isOk());

        mvc.perform(patch("/api/consultorios/" + c2 + "/activo").header(AUTH, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("Lucas Sanchez")));
    }

    // ---------- Horarios ----------

    @Test
    void elHorarioGuardaYDevuelveElConsultorio() throws Exception {
        String c2 = consultorio("Consultorio 2");
        guardarHorarios(LUCAS, tramo(2, "10:00", "13:00", c2), tramo(2, "16:00", "21:00", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].consultorioId").value(c2))
                .andExpect(jsonPath("$[1].consultorioId").value(nullValue()));

        mvc.perform(get("/api/profesionales/" + LUCAS + "/horarios").header(AUTH, admin))
                .andExpect(jsonPath("$[0].consultorioId").value(c2));

        // Volver a guardar el mismo horario no choca consigo mismo
        guardarHorarios(LUCAS, tramo(2, "10:00", "13:00", c2), tramo(2, "16:00", "21:00", null))
                .andExpect(status().isOk());
    }

    @Test
    void dosProfesionalesNoPuedenTenerElMismoConsultorioEnHorariosSuperpuestos() throws Exception {
        String c2 = consultorio("Consultorio 2");
        guardarHorarios(LUCAS, tramo(2, "10:00", "13:00", c2)).andExpect(status().isOk());

        guardarHorarios(ANDRES, tramo(2, "12:00", "14:00", c2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.horarios")
                        .value("Consultorio 2 ya está ocupado el martes de 10:00 a 13:00 por Lucas Sanchez"));

        // Pegado (empieza cuando el otro termina), otro día u otro consultorio: no hay problema
        String c3 = consultorio("Consultorio 3");
        guardarHorarios(ANDRES,
                tramo(2, "13:00", "15:00", c2),
                tramo(3, "10:00", "13:00", c2),
                tramo(2, "10:00", "12:00", c3))
                .andExpect(status().isOk());

        // Sin consultorio, se superponen como siempre (cada uno en lo suyo)
        guardarHorarios(CHAPA, tramo(2, "10:00", "13:00", null)).andExpect(status().isOk());
    }

    @Test
    void elConsultorioTieneQueSerDeLaSucursalDelTramoYEstarActivo() throws Exception {
        Sucursal otra = new Sucursal();
        otra.setNegocio(negocios.findById(NEGOCIO_JADE).orElseThrow());
        otra.setNombre("Jade Norte");
        otra.setDireccion("Alvear 100");
        otra.setCiudad("Río Cuarto");
        sucursales.saveAndFlush(otra);
        String deLaOtra = idDe(crearConsultorio(otra.getId().toString(), "Consultorio 1").andExpect(status().isCreated()));

        guardarHorarios(LUCAS, tramo(2, "10:00", "13:00", deLaOtra))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.horarios", containsString("no es de la sucursal")));

        String inactivo = consultorio("Consultorio viejo");
        mvc.perform(patch("/api/consultorios/" + inactivo + "/activo").header(AUTH, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk());
        guardarHorarios(LUCAS, tramo(2, "10:00", "13:00", inactivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.horarios").exists());

        guardarHorarios(LUCAS, tramo(2, "10:00", "13:00", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());
    }

    // ---------- Disponibilidad con consultorio compartido ----------

    @Test
    void elTurnoQueSeDaEnUnTramoConConsultorioQuedaEnEseConsultorio() throws Exception {
        String c2 = consultorio("Consultorio 2");
        guardarHorarios(LUCAS, tramo(2, "10:00", "13:00", c2), tramo(2, "16:00", "21:00", null))
                .andExpect(status().isOk());

        String manana = idDe(reservarOnline(LUCAS, "10:00", "+5493584770011").andExpect(status().isCreated()));
        String tarde = idDe(reservarOnline(LUCAS, "16:00", "+5493584770012").andExpect(status().isCreated()));

        Atencion turnoManana = atenciones.findById(UUID.fromString(manana)).orElseThrow();
        assertThat(turnoManana.getConsultorio()).isNotNull();
        assertThat(turnoManana.getConsultorio().getId()).isEqualTo(UUID.fromString(c2));
        assertThat(atenciones.findById(UUID.fromString(tarde)).orElseThrow().getConsultorio()).isNull();

        // El panel muestra en qué consultorio es
        mvc.perform(get("/api/agenda").header(AUTH, admin)
                        .param("desde", martes.toString())
                        .param("hasta", martes.toString())
                        .param("profesionalId", LUCAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos[?(@.id == '%s')].consultorioNombre".formatted(manana), hasItem("Consultorio 2")));
    }

    @Test
    void laDisponibilidadDescartaLosSlotsConElConsultorioOcupadoPorOtroProfesional() throws Exception {
        String c2 = consultorio("Consultorio 2");
        turnoDeLucasEnElConsultorioYTramoPasadoAAndres(c2);

        // Andrés no tiene turnos propios, pero a las 10:00 el Consultorio 2 lo ocupa Lucas
        disponibilidad(ANDRES)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[*].hora", not(hasItem(startsWith("10:00")))))
                .andExpect(jsonPath("$.slots[*].hora", hasItem(startsWith("10:30"))))
                .andExpect(jsonPath("$.slots[*].hora", hasItem(startsWith("16:00"))));

        // Chapa no usa el consultorio: a las 10:00 está libre
        disponibilidad(CHAPA)
                .andExpect(jsonPath("$.slots[*].hora", hasItem(startsWith("10:00"))));
    }

    @Test
    void unTurnoCanceladoLiberaElConsultorio() throws Exception {
        String c2 = consultorio("Consultorio 2");
        String turnoLucas = turnoDeLucasEnElConsultorioYTramoPasadoAAndres(c2);

        mvc.perform(patch("/api/agenda/turnos/" + turnoLucas + "/estado").header(AUTH, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isOk());

        disponibilidad(ANDRES)
                .andExpect(jsonPath("$.slots[*].hora", hasItem(startsWith("10:00"))));
    }

    // ---------- Criterio: no se pueden generar dos turnos simultáneos en el mismo consultorio ----------

    @Test
    void laReservaOnlineNoDaUnTurnoConElConsultorioOcupado() throws Exception {
        String c2 = consultorio("Consultorio 2");
        turnoDeLucasEnElConsultorioYTramoPasadoAAndres(c2);

        reservarOnline(ANDRES, "10:00", "+5493584770021")
                .andExpect(status().isConflict());
        // Media hora después el consultorio ya está libre
        reservarOnline(ANDRES, "10:30", "+5493584770022")
                .andExpect(status().isCreated());
    }

    @Test
    void elPanelNoDaTurnoNiSobreturnoConElConsultorioOcupadoPorOtro() throws Exception {
        String c2 = consultorio("Consultorio 2");
        turnoDeLucasEnElConsultorioYTramoPasadoAAndres(c2);

        turnoPanel(ANDRES, "10:00", false)
                .andExpect(status().isConflict());
        turnoPanel(ANDRES, "10:00", true)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Consultorio 2 está ocupado por otro profesional en ese horario"));
    }

    @Test
    void unSobreturnoDelMismoProfesionalEnSuConsultorioSiguePermitido() throws Exception {
        String c2 = consultorio("Consultorio 2");
        guardarHorarios(LUCAS, tramo(2, "10:00", "13:00", c2)).andExpect(status().isOk());
        reservarOnline(LUCAS, "10:00", "+5493584770031").andExpect(status().isCreated());

        // El consultorio lo está usando él mismo: el sobreturno va como siempre
        turnoPanel(LUCAS, "10:00", true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consultorioNombre").value("Consultorio 2"));
    }

    @Test
    void noSePuedeReprogramarAUnHorarioConElConsultorioOcupado() throws Exception {
        String c2 = consultorio("Consultorio 2");
        turnoDeLucasEnElConsultorioYTramoPasadoAAndres(c2);
        String body = reservarOnline(ANDRES, "11:00", "+5493584770041")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.tokenGestion");

        mvc.perform(put("/api/public/turnos/" + token + "/reprogramar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"%s\",\"hora\":\"10:00\"}".formatted(martes)))
                .andExpect(status().isConflict());
    }

    @Test
    void alReprogramarElTurnoTomaElConsultorioDelTramoNuevo() throws Exception {
        String c2 = consultorio("Consultorio 2");
        turnoDeLucasEnElConsultorioYTramoPasadoAAndres(c2);
        String body = reservarOnline(ANDRES, "11:00", "+5493584770042")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.tokenGestion");
        String turnoId = JsonPath.read(body, "$.id");
        assertThat(atenciones.findById(UUID.fromString(turnoId)).orElseThrow().getConsultorio()).isNotNull();

        // A la tarde el tramo no tiene consultorio: el turno deja de ocupar el Consultorio 2
        mvc.perform(put("/api/public/turnos/" + token + "/reprogramar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"%s\",\"hora\":\"16:00\"}".formatted(martes)))
                .andExpect(status().isOk());
        assertThat(atenciones.findById(UUID.fromString(turnoId)).orElseThrow().getConsultorio()).isNull();
    }

    /**
     * La última palabra la tiene la base: aunque dos requests pasen los chequeos al mismo tiempo,
     * {@code excl_atencion_consultorio} rechaza el segundo turno.
     */
    @Test
    void laBaseRechazaDosTurnosSimultaneosDeDistintosProfesionalesEnElMismoConsultorio() throws Exception {
        String c2 = consultorio("Consultorio 2");
        String turnoLucas = turnoDeLucasEnElConsultorioYTramoPasadoAAndres(c2);
        Atencion deLucas = atenciones.findById(UUID.fromString(turnoLucas)).orElseThrow();

        Atencion deAndres = new Atencion();
        deAndres.setNegocio(deLucas.getNegocio());
        deAndres.setSucursal(deLucas.getSucursal());
        deAndres.setProfesional(profesionales.findById(UUID.fromString(ANDRES)).orElseThrow());
        deAndres.setServicio(deLucas.getServicio());
        deAndres.setInicio(martesA(10, 15));
        deAndres.setFin(martesA(10, 45));
        deAndres.setEstado(EstadoAtencion.RESERVADA);
        deAndres.setOrigen(deLucas.getOrigen());
        deAndres.setPrecioFinal(0);
        deAndres.setConsultorio(consultorios.findById(UUID.fromString(c2)).orElseThrow());

        // Después de esto la transacción de Postgres queda abortada: no se hace nada más en el test
        assertThatThrownBy(() -> atenciones.saveAndFlush(deAndres))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(Consultorio.RESTRICCION_TURNOS);
    }

    @Test
    void laConsultaDeOcupacionTraeSoloLosTurnosDeOtrosProfesionales() throws Exception {
        String c2 = consultorio("Consultorio 2");
        String turnoLucas = turnoDeLucasEnElConsultorioYTramoPasadoAAndres(c2);

        assertThat(atenciones.findOcupadasEnConsultoriosPorOtros(
                List.of(UUID.fromString(c2)), UUID.fromString(ANDRES),
                Set.of(EstadoAtencion.RESERVADA), martesA(10, 0), martesA(10, 30)))
                .extracting(Atencion::getId)
                .containsExactly(UUID.fromString(turnoLucas));
        assertThat(atenciones.findOcupadasEnConsultoriosPorOtros(
                List.of(UUID.fromString(c2)), UUID.fromString(LUCAS),
                Set.of(EstadoAtencion.RESERVADA), martesA(10, 0), martesA(10, 30)))
                .isEmpty();
    }
}

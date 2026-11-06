package com.innovax.barberia.agenda;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.auth.Rol;
import com.innovax.barberia.auth.Usuario;
import com.innovax.barberia.auth.UsuarioRepository;
import com.innovax.barberia.cliente.ClienteRepository;
import com.innovax.barberia.listaespera.ListaEsperaRepository;
import com.innovax.barberia.negocio.NegocioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue #71: dar turno desde el panel, sobreturnos y lista de espera.
 *
 * <p>No es {@code @Transactional}: la regla de turnos superpuestos la aplica la base al guardar, y hay
 * que probarla con transacciones reales. Cada test borra lo que crea (turnos de Chapa y Lucas ese día).
 */
@SpringBootTest
class TurnoPanelIntegrationTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    private static final String NEGOCIO = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String CORTE = "dddddddd-0000-0000-0000-000000000001"; // 30 min
    private static final String SUCURSAL = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final String LUCAS = "cccccccc-0000-0000-0000-000000000001";
    private static final String CHAPA = "cccccccc-0000-0000-0000-000000000003";
    private static final String CLIENTE = "eeeeeeee-0000-0000-0000-000000000001";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private AtencionRepository atenciones;
    @Autowired
    private ListaEsperaRepository listaEspera;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private ClienteRepository clientes;
    @Autowired
    private NegocioRepository negocios;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mvc;
    /** Próximo martes: futuro, laborable en el seed (lun-sáb 10-13 y 16-21) y dentro de la anticipación online. */
    private LocalDate fecha;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        fecha = LocalDate.now(ZONA).with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
    }

    @AfterEach
    void tearDown() {
        var inicioDia = ZonedDateTime.of(fecha, LocalTime.MIN, ZONA).toInstant();
        var finDia = ZonedDateTime.of(fecha.plusDays(1), LocalTime.MIN, ZONA).toInstant();
        atenciones.findAll().stream()
                .filter(a -> List.of(UUID.fromString(LUCAS), UUID.fromString(CHAPA)).contains(a.getProfesional().getId()))
                .filter(a -> !a.getInicio().isBefore(inicioDia) && a.getInicio().isBefore(finDia))
                .forEach(atenciones::delete);
        listaEspera.findAll().stream()
                .filter(l -> l.getFecha().equals(fecha))
                .forEach(listaEspera::delete);
        usuarios.findByEmailIgnoreCase("recepcion-turnos@jade.com").ifPresent(usuarios::delete);
        clientes.findByNegocioIdAndTelefono(UUID.fromString(NEGOCIO), "+5493584997101").ifPresent(clientes::delete);
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

    private String lucas() throws Exception {
        return login("lucas@jade.com", "barbero123");
    }

    private String recepcion() throws Exception {
        Usuario u = new Usuario();
        u.setNegocio(negocios.findById(UUID.fromString(NEGOCIO)).orElseThrow());
        u.setEmail("recepcion-turnos@jade.com");
        u.setPasswordHash(passwordEncoder.encode("recepcion123"));
        u.setRol(Rol.RECEPCION);
        u.setActivo(true);
        usuarios.saveAndFlush(u);
        return login("recepcion-turnos@jade.com", "recepcion123");
    }

    private ResultActions darTurno(String token, String profesional, String hora, boolean sobreturno) throws Exception {
        return mvc.perform(post("/api/agenda/turnos").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"clienteId":"%s","servicioId":"%s","profesionalId":"%s","fecha":"%s","hora":"%s","sobreturno":%s}
                        """.formatted(CLIENTE, CORTE, profesional, fecha, hora, sobreturno)));
    }

    private UUID idDe(ResultActions r) throws Exception {
        return UUID.fromString(JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id"));
    }

    // ---------- Turno normal ----------

    @Test
    void adminDaUnTurnoNormalEnUnHorarioLibre() throws Exception {
        UUID id = idDe(darTurno(admin(), CHAPA, "11:00", false)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RESERVADA"))
                .andExpect(jsonPath("$.origen").value("MANUAL"))
                .andExpect(jsonPath("$.sobreturno").value(false)));

        Atencion a = atenciones.findById(id).orElseThrow();
        assertThat(a.getInicio()).isEqualTo(ZonedDateTime.of(fecha, LocalTime.of(11, 0), ZONA).toInstant());
    }

    /** Las horas vienen como "11:00" o "11:00:00" según la configuración de Jackson: se compara el comienzo. */
    private static boolean incluye(List<String> horas, String hora) {
        return horas.stream().anyMatch(h -> h.startsWith(hora));
    }

    private List<String> horasLibres(String token) throws Exception {
        String body = mvc.perform(get("/api/agenda/horarios-libres").header("Authorization", token)
                        .param("profesionalId", CHAPA).param("servicioId", CORTE).param("fecha", fecha.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.horas");
    }

    @Test
    void losHorariosLibresNoIncluyenLosOcupados() throws Exception {
        String admin = admin();
        assertThat(incluye(horasLibres(admin), "11:00")).isTrue();

        darTurno(admin, CHAPA, "11:00", false).andExpect(status().isCreated());

        assertThat(incluye(horasLibres(admin), "11:00")).isFalse();
    }

    @Test
    void unTurnoNormalNoPuedeIrEnUnHorarioOcupado() throws Exception {
        String admin = admin();
        darTurno(admin, CHAPA, "11:00", false).andExpect(status().isCreated());
        darTurno(admin, CHAPA, "11:00", false).andExpect(status().isConflict());
    }

    @Test
    void unTurnoNormalNoPuedeIrFueraDelHorario() throws Exception {
        darTurno(admin(), CHAPA, "22:00", false).andExpect(status().isConflict());
    }

    @Test
    void elProfesionalDaTurnosSoloParaEl() throws Exception {
        String lucas = lucas();
        darTurno(lucas, LUCAS, "17:00", false).andExpect(status().isCreated());
        darTurno(lucas, CHAPA, "17:00", false).andExpect(status().isForbidden());
    }

    // ---------- Sobreturnos ----------

    @Test
    void sobreturnoEncimaDeOtroTurno() throws Exception {
        String admin = admin();
        darTurno(admin, CHAPA, "11:00", false).andExpect(status().isCreated());
        darTurno(admin, CHAPA, "11:00", true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sobreturno").value(true));

        var inicio = ZonedDateTime.of(fecha, LocalTime.of(11, 0), ZONA).toInstant();
        assertThat(atenciones.findAll().stream()
                .filter(a -> a.getProfesional().getId().equals(UUID.fromString(CHAPA)) && a.getInicio().equals(inicio)))
                .hasSize(2);
    }

    @Test
    void recepcionTambienDaSobreturnos() throws Exception {
        String recepcion = recepcion();
        darTurno(recepcion, CHAPA, "11:00", false).andExpect(status().isCreated());
        darTurno(recepcion, CHAPA, "11:00", true).andExpect(status().isCreated());
    }

    @Test
    void sobreturnoFueraDelHorario() throws Exception {
        darTurno(admin(), CHAPA, "21:30", true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sobreturno").value(true));
    }

    @Test
    void unSobreturnoNoPuedeOcuparUnHorarioLibre() throws Exception {
        darTurno(admin(), CHAPA, "11:00", true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void elProfesionalNoPuedeDarSobreturnos() throws Exception {
        String lucas = lucas();
        darTurno(lucas, LUCAS, "17:00", false).andExpect(status().isCreated());
        darTurno(lucas, LUCAS, "17:00", true).andExpect(status().isForbidden());
    }

    @Test
    void dosTurnosNormalesSiguenSinPoderPisarseAunqueHayaUnSobreturno() throws Exception {
        String admin = admin();
        darTurno(admin, CHAPA, "11:00", false).andExpect(status().isCreated());
        darTurno(admin, CHAPA, "11:00", true).andExpect(status().isCreated());
        // El sobreturno no "abre" el horario para un tercer turno normal
        darTurno(admin, CHAPA, "11:00", false).andExpect(status().isConflict());
    }

    @Test
    void laReservaOnlineNoPuedeCrearSobreturnos() throws Exception {
        mvc.perform(post("/api/public/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sucursalId":"%s","servicioId":"%s","profesionalId":"%s","fecha":"%s","hora":"12:00",
                                 "clienteNombre":"Cliente Test","clienteTelefono":"+5493584997101","sobreturno":true}
                                """.formatted(SUCURSAL, CORTE, CHAPA, fecha)))
                .andExpect(status().isCreated());

        var inicio = ZonedDateTime.of(fecha, LocalTime.of(12, 0), ZONA).toInstant();
        Atencion creada = atenciones.findAll().stream()
                .filter(a -> a.getProfesional().getId().equals(UUID.fromString(CHAPA)) && a.getInicio().equals(inicio))
                .findFirst().orElseThrow();
        assertThat(creada.isSobreturno()).isFalse();
    }

    @Test
    void laReservaOnlineNoOfreceElHorarioDeUnSobreturno() throws Exception {
        String admin = admin();
        darTurno(admin, CHAPA, "11:00", false).andExpect(status().isCreated());
        darTurno(admin, CHAPA, "11:00", true).andExpect(status().isCreated());

        String body = mvc.perform(get("/api/public/disponibilidad")
                        .param("sucursal", SUCURSAL).param("servicio", CORTE).param("profesional", CHAPA)
                        .param("fecha", fecha.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> horas = JsonPath.read(body, "$.slots[*].hora");
        assertThat(incluye(horas, "11:00")).isFalse();
        assertThat(incluye(horas, "12:00")).isTrue(); // el resto del día sigue libre
    }

    @Test
    void alReprogramarDesdeElLinkUnSobreturnoPasaASerNormal() throws Exception {
        UUID id = idDe(darTurno(admin(), CHAPA, "21:30", true).andExpect(status().isCreated()));
        UUID token = atenciones.findById(id).orElseThrow().getTokenGestion();

        mvc.perform(put("/api/public/turnos/{token}/reprogramar", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"%s\",\"hora\":\"12:30\"}".formatted(fecha)))
                .andExpect(status().isOk());

        Atencion a = atenciones.findById(id).orElseThrow();
        assertThat(a.isSobreturno()).isFalse();
        assertThat(a.getInicio()).isEqualTo(ZonedDateTime.of(fecha, LocalTime.of(12, 30), ZONA).toInstant());
    }

    // ---------- Lista de espera ----------

    private ResultActions anotar(String token, String profesional) throws Exception {
        String prof = profesional == null ? "null" : "\"" + profesional + "\"";
        return mvc.perform(post("/api/lista-espera").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"clienteId":"%s","profesionalId":%s,"fecha":"%s","nota":"Cualquier horario a la tarde"}
                        """.formatted(CLIENTE, prof, fecha)));
    }

    private ResultActions lista(String token) throws Exception {
        return mvc.perform(get("/api/lista-espera").header("Authorization", token).param("fecha", fecha.toString()));
    }

    @Test
    void alCancelarUnTurnoSeAvisaALaListaDeEspera() throws Exception {
        String admin = admin();
        UUID turno = idDe(darTurno(admin, CHAPA, "11:00", false).andExpect(status().isCreated()));
        anotar(admin, CHAPA).andExpect(status().isCreated())
                .andExpect(jsonPath("$.profesionalNombre").exists())
                .andExpect(jsonPath("$.avisoEn").value(nullValue()));

        mvc.perform(patch("/api/agenda/turnos/{id}/estado", turno).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isOk());

        lista(admin).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].avisoEn").exists())
                .andExpect(jsonPath("$[0].avisoDetalle", containsString("11:00")));
    }

    @Test
    void cancelarElTurnoDeOtroProfesionalNoAvisa() throws Exception {
        String admin = admin();
        UUID turno = idDe(darTurno(admin, LUCAS, "11:00", false).andExpect(status().isCreated()));
        anotar(admin, CHAPA).andExpect(status().isCreated());

        mvc.perform(patch("/api/agenda/turnos/{id}/estado", turno).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isOk());

        lista(admin).andExpect(jsonPath("$[0].avisoEn").value(nullValue()));
    }

    @Test
    void resolverLoSacaDeLaLista() throws Exception {
        String admin = admin();
        String body = anotar(admin, null).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(patch("/api/lista-espera/{id}/resuelto", id).header("Authorization", admin))
                .andExpect(status().isNoContent());
        lista(admin).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void elProfesionalVeLosSuyosYLosDeCualquiera() throws Exception {
        String admin = admin();
        anotar(admin, CHAPA).andExpect(status().isCreated());
        anotar(admin, null).andExpect(status().isCreated());

        lista(admin).andExpect(jsonPath("$", hasSize(2)));
        lista(lucas()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].profesionalId").value(nullValue()));
    }

    @Test
    void elProfesionalAnotaSoloParaEl() throws Exception {
        anotar(lucas(), CHAPA).andExpect(status().isCreated())
                .andExpect(jsonPath("$.profesionalId").value(LUCAS));
    }
}

package com.innovax.barberia.atencion;

import com.innovax.barberia.auth.Rol;
import com.innovax.barberia.auth.Usuario;
import com.innovax.barberia.auth.UsuarioRepository;
import com.innovax.barberia.cliente.ClienteRepository;
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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue #72: notas de consulta (no historia clínica). Las leen y editan solo el profesional que atendió y el
 * admin; recepción nunca. Quedan registrados quién y cuándo las editó, y no salen por ningún endpoint
 * público ni por los mensajes al cliente.
 *
 * <p>No es {@code @Transactional}: cada test borra lo que crea (turnos de Lucas y Chapa ese día).
 */
@SpringBootTest
class NotasConsultaIntegrationTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    private static final String NEGOCIO = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String CORTE = "dddddddd-0000-0000-0000-000000000001";
    private static final String SUCURSAL = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final String LUCAS = "cccccccc-0000-0000-0000-000000000001";
    private static final String CHAPA = "cccccccc-0000-0000-0000-000000000003";
    private static final String CLIENTE = "eeeeeeee-0000-0000-0000-000000000001";
    private static final String TELEFONO_ONLINE = "+5493584997201";
    private static final String EMAIL_RECEPCION = "recepcion-notas@jade.com";
    private static final String NOTA = "Dolor lumbar hace dos semanas, volver en 15 días";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private AtencionRepository atenciones;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private ClienteRepository clientes;
    @Autowired
    private NegocioRepository negocios;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mvc;
    private LocalDate fecha;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        fecha = LocalDate.now(ZONA).with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
    }

    @AfterEach
    void tearDown() {
        var inicioDia = ZonedDateTime.of(fecha, LocalTime.MIN, ZONA).toInstant();
        var finDia = ZonedDateTime.of(fecha.plusDays(1), LocalTime.MIN, ZONA).toInstant();
        atenciones.findAll().stream()
                .filter(a -> List.of(UUID.fromString(LUCAS), UUID.fromString(CHAPA)).contains(a.getProfesional().getId()))
                .filter(a -> !a.getInicio().isBefore(inicioDia) && a.getInicio().isBefore(finDia))
                .forEach(atenciones::delete);
        usuarios.findByEmailIgnoreCase(EMAIL_RECEPCION).ifPresent(usuarios::delete);
        clientes.findByNegocioIdAndTelefono(UUID.fromString(NEGOCIO), TELEFONO_ONLINE).ifPresent(clientes::delete);
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
        u.setEmail(EMAIL_RECEPCION);
        u.setPasswordHash(passwordEncoder.encode("recepcion123"));
        u.setRol(Rol.RECEPCION);
        u.setActivo(true);
        usuarios.saveAndFlush(u);
        return login(EMAIL_RECEPCION, "recepcion123");
    }

    private ResultActions darTurno(String token, String profesional, String hora, String notas) throws Exception {
        String campoNotas = notas == null ? "" : ",\"notas\":\"%s\"".formatted(notas);
        return mvc.perform(post("/api/agenda/turnos").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"clienteId":"%s","servicioId":"%s","profesionalId":"%s","fecha":"%s","hora":"%s","sobreturno":false%s}
                        """.formatted(CLIENTE, CORTE, profesional, fecha, hora, campoNotas)));
    }

    private UUID idDe(ResultActions r) throws Exception {
        return UUID.fromString(JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id"));
    }

    private ResultActions editarNotas(String token, UUID turno, String notas) throws Exception {
        String valor = notas == null ? "null" : "\"%s\"".formatted(notas);
        return mvc.perform(patch("/api/agenda/turnos/{id}/notas", turno).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notas\":%s}".formatted(valor)));
    }

    private String agendaComo(String token) throws Exception {
        return mvc.perform(get("/api/agenda").header("Authorization", token)
                        .param("desde", fecha.toString()).param("hasta", fecha.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // ---------- Quién puede leer ----------

    @Test
    void recepcionNoPuedeLeerLasNotasDeConsulta() throws Exception {
        UUID turno = idDe(darTurno(admin(), LUCAS, "11:00", null).andExpect(status().isCreated()));
        editarNotas(admin(), turno, NOTA).andExpect(status().isOk());

        String agenda = agendaComo(recepcion());

        // El turno está en la agenda de recepción, pero sin la nota ni quién la editó
        assertThat(agenda).contains(turno.toString());
        assertThat(agenda).doesNotContain(NOTA);
        assertThat((List<String>) JsonPath.read(agenda, "$.turnos[?(@.id=='%s')].notas".formatted(turno)))
                .containsExactly((String) null);
        assertThat((List<String>) JsonPath.read(agenda, "$.turnos[?(@.id=='%s')].notasEditadasPorEmail".formatted(turno)))
                .containsExactly((String) null);
    }

    @Test
    void recepcionNoPuedeEditarLasNotasDeConsulta() throws Exception {
        UUID turno = idDe(darTurno(admin(), LUCAS, "11:00", null).andExpect(status().isCreated()));

        editarNotas(recepcion(), turno, NOTA).andExpect(status().isForbidden());

        assertThat(atenciones.findById(turno).orElseThrow().getNotas()).isNull();
    }

    @Test
    void recepcionNoVeLaNotaEnLaRespuestaDeLosTurnosQueDa() throws Exception {
        darTurno(recepcion(), LUCAS, "11:00", NOTA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notas").value(nullValue()))
                .andExpect(jsonPath("$.notasEditadasPorEmail").value(nullValue()));
    }

    @Test
    void recepcionNoVeLaNotaAlCambiarElEstadoNiAlMarcarLaLlegada() throws Exception {
        String recepcion = recepcion();
        UUID turno = idDe(darTurno(admin(), LUCAS, "11:00", null).andExpect(status().isCreated()));
        editarNotas(admin(), turno, NOTA).andExpect(status().isOk());

        mvc.perform(patch("/api/agenda/turnos/{id}/llegada", turno).header("Authorization", recepcion)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"llego\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notas").value(nullValue()));
        mvc.perform(patch("/api/agenda/turnos/{id}/estado", turno).header("Authorization", recepcion)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"AUSENTE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notas").value(nullValue()));
    }

    @Test
    void elAdminLeeLasNotas() throws Exception {
        String admin = admin();
        UUID turno = idDe(darTurno(admin, LUCAS, "11:00", null).andExpect(status().isCreated()));
        editarNotas(admin, turno, NOTA).andExpect(status().isOk());

        assertThat((List<String>) JsonPath.read(agendaComo(admin), "$.turnos[?(@.id=='%s')].notas".formatted(turno)))
                .containsExactly(NOTA);
    }

    @Test
    void elProfesionalQueAtendioLasLeeYLasEdita() throws Exception {
        String lucas = lucas();
        UUID turno = idDe(darTurno(admin(), LUCAS, "11:00", null).andExpect(status().isCreated()));

        editarNotas(lucas, turno, NOTA).andExpect(status().isOk())
                .andExpect(jsonPath("$.notas").value(NOTA))
                .andExpect(jsonPath("$.notasEditadasPorEmail").value("lucas@jade.com"));

        assertThat((List<String>) JsonPath.read(agendaComo(lucas), "$.turnos[?(@.id=='%s')].notas".formatted(turno)))
                .containsExactly(NOTA);
    }

    @Test
    void unProfesionalNoEditaLasNotasDeOtro() throws Exception {
        UUID turnoDeChapa = idDe(darTurno(admin(), CHAPA, "11:00", null).andExpect(status().isCreated()));

        editarNotas(lucas(), turnoDeChapa, NOTA).andExpect(status().isForbidden());

        assertThat(atenciones.findById(turnoDeChapa).orElseThrow().getNotas()).isNull();
    }

    @Test
    void unTurnoInexistenteDaNotFound() throws Exception {
        editarNotas(admin(), UUID.randomUUID(), NOTA).andExpect(status().isNotFound());
    }

    @Test
    void sinSesionNoSePuedenEditarLasNotas() throws Exception {
        mvc.perform(patch("/api/agenda/turnos/{id}/notas", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notas\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Quién y cuándo editó ----------

    @Test
    void quedaRegistradoQuienYCuandoEditoLaNota() throws Exception {
        UUID turno = idDe(darTurno(admin(), LUCAS, "11:00", null).andExpect(status().isCreated()));

        editarNotas(lucas(), turno, NOTA).andExpect(status().isOk())
                .andExpect(jsonPath("$.notasEditadasPorEmail").value("lucas@jade.com"))
                .andExpect(jsonPath("$.notasEditadasEn").exists());
        // Se lee por HTTP y no desde la entidad: el test no es @Transactional y la relación es lazy
        assertThat(atenciones.findById(turno).orElseThrow().getNotasEditadasEn()).isNotNull();

        editarNotas(admin(), turno, NOTA + " (corregido)").andExpect(status().isOk())
                .andExpect(jsonPath("$.notasEditadasPorEmail").value("admin@jade.com"))
                .andExpect(jsonPath("$.notasEditadasEn").exists());
    }

    @Test
    void reenviarLaMismaNotaNoPisaLaAuditoria() throws Exception {
        UUID turno = idDe(darTurno(admin(), LUCAS, "11:00", null).andExpect(status().isCreated()));
        editarNotas(lucas(), turno, NOTA).andExpect(status().isOk());

        editarNotas(admin(), turno, NOTA).andExpect(status().isOk())
                .andExpect(jsonPath("$.notasEditadasPorEmail").value("lucas@jade.com"));
    }

    @Test
    void unaNotaEnBlancoLaBorra() throws Exception {
        String admin = admin();
        UUID turno = idDe(darTurno(admin, LUCAS, "11:00", null).andExpect(status().isCreated()));
        editarNotas(admin, turno, NOTA).andExpect(status().isOk());

        editarNotas(admin, turno, "   ").andExpect(status().isOk())
                .andExpect(jsonPath("$.notas").value(nullValue()))
                .andExpect(jsonPath("$.notasEditadasPorEmail").value(nullValue()));
        assertThat(atenciones.findById(turno).orElseThrow().getNotas()).isNull();
    }

    @Test
    void laNotaCargadaAlDarElTurnoTambienQuedaRegistrada() throws Exception {
        darTurno(admin(), LUCAS, "11:00", NOTA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notas").value(NOTA))
                .andExpect(jsonPath("$.notasEditadasPorEmail").value("admin@jade.com"));
    }

    @Test
    void lasNotasTienenUnMaximoDe2000Caracteres() throws Exception {
        UUID turno = idDe(darTurno(admin(), LUCAS, "11:00", null).andExpect(status().isCreated()));

        editarNotas(admin(), turno, "a".repeat(2001)).andExpect(status().isBadRequest());
    }

    // ---------- Nunca salen por afuera del panel ----------

    @Test
    void laNotaNoSaleEnLaReservaPublicaNiEnLaGestionDelTurnoNiEnLosMensajes() throws Exception {
        String notaDelCliente = "NOTA-PRIVADA-72";
        String reserva = mvc.perform(post("/api/public/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sucursalId":"%s","servicioId":"%s","profesionalId":"%s","fecha":"%s","hora":"10:00",
                                 "clienteNombre":"Cliente Notas","clienteTelefono":"%s","notas":"%s"}
                                """.formatted(SUCURSAL, CORTE, LUCAS, fecha, TELEFONO_ONLINE, notaDelCliente)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(reserva).doesNotContain(notaDelCliente);

        String token = JsonPath.read(reserva, "$.tokenGestion");
        String gestion = mvc.perform(get("/api/public/turnos/{token}", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(gestion).doesNotContain(notaDelCliente);

        // Mensajes al cliente (confirmación, recordatorio) que ve el panel, pendientes o ya enviados: no llevan la nota
        String admin = admin();
        for (String estado : List.of("PENDIENTE", "ENVIADA")) {
            String mensajes = mvc.perform(get("/api/notificaciones").header("Authorization", admin).param("estado", estado))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(mensajes).doesNotContain(notaDelCliente);
        }

        // El admin sí la lee en la agenda: está guardada, solo que no sale para el público
        assertThat(agendaComo(admin)).contains(notaDelCliente);
    }
}

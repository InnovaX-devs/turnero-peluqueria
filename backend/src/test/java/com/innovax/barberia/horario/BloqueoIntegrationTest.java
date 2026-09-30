package com.innovax.barberia.horario;

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Usa los datos del seed de dev (necesita la base levantada).
 * Es @Transactional: todo lo que crea se revierte al terminar.
 */
@SpringBootTest
@Transactional
class BloqueoIntegrationTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    private static final String SUCURSAL = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final String SERVICIO_CORTE = "dddddddd-0000-0000-0000-000000000001";
    private static final String LUCAS = "cccccccc-0000-0000-0000-000000000001";
    private static final String ANDRES = "cccccccc-0000-0000-0000-000000000002";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private BloqueoRepository bloqueos;

    private MockMvc mvc;
    /** Próximo lunes: siempre futuro, dentro del horario del seed (lun-sáb, 10-13 y 16-21) y de la anticipación máxima. */
    private LocalDate lunes;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        lunes = LocalDate.now(ZONA).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private String lucas() throws Exception {
        return login("lucas@jade.com", "barbero123");
    }

    private String admin() throws Exception {
        return login("admin@jade.com", "admin123");
    }

    private Instant a(LocalDate fecha, int hora, int minuto) {
        return ZonedDateTime.of(fecha, LocalTime.of(hora, minuto), ZONA).toInstant();
    }

        /** ["id1","id2"], o null si no se indica ninguno (= el barbero logueado). */
    private static String lista(String... ids) {
        if (ids.length == 0 || ids[0] == null) {
            return "null";
        }
        return Arrays.stream(ids).map(id -> "\"" + id + "\"").collect(Collectors.joining(",", "[", "]"));
    }

    /** Body de un bloqueo por rango horario, para un barbero (null = el logueado). */
    private String rango(String barberoId, int desdeHora, int hastaHora, String motivo) {
        return rangoVarios(new String[]{barberoId}, desdeHora, hastaHora, motivo);
    }

    /** Body de un bloqueo por rango horario, para varios barberos. */
    private String rangoVarios(String[] barberoIds, int desdeHora, int hastaHora, String motivo) {
        return """
                {"barberoIds":%s,"fecha":"%s","horaDesde":"%02d:00","horaHasta":"%02d:00","motivo":%s}
                """.formatted(lista(barberoIds), lunes, desdeHora, hastaHora,
                motivo == null ? "null" : "\"" + motivo + "\"");
    }

    private String diaCompleto(String barberoId) {
        return """
                {"barberoIds":%s,"fecha":"%s"}
                """.formatted(lista(barberoId), lunes);
    }

    private String crear(String token, String json, int statusEsperado) throws Exception {
        return mvc.perform(post("/api/bloqueos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(statusEsperado))
                .andReturn().getResponse().getContentAsString();
    }

    private Bloqueo bloqueoDe(String respuesta) {
        return bloqueos.findById(UUID.fromString(JsonPath.read(respuesta, "$[0].id"))).orElseThrow();
    }

    // ---------- Crear ----------

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(post("/api/bloqueos").contentType(MediaType.APPLICATION_JSON).content(diaCompleto(null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void barberoCreaUnBloqueoPorRangoHorario() throws Exception {
        Bloqueo b = bloqueoDe(crear(lucas(), rango(null, 13, 14, "Almuerzo"), 201));

        assertThat(b.getBarbero().getId()).isEqualTo(UUID.fromString(LUCAS)); // el logueado
        assertThat(b.getInicio()).isEqualTo(a(lunes, 13, 0));
        assertThat(b.getFin()).isEqualTo(a(lunes, 14, 0));
        assertThat(b.getMotivo()).isEqualTo("Almuerzo");
    }

    @Test
    void sinHorasBloqueaElDiaCompleto() throws Exception {
        Bloqueo b = bloqueoDe(crear(lucas(), diaCompleto(null), 201));

        assertThat(b.getInicio()).isEqualTo(a(lunes, 0, 0));
        assertThat(b.getFin()).isEqualTo(a(lunes.plusDays(1), 0, 0));
    }

    @Test
    void elMotivoEsOpcional() throws Exception {
        assertThat(bloqueoDe(crear(lucas(), rango(null, 13, 14, null), 201)).getMotivo()).isNull();
        assertThat(bloqueoDe(crear(lucas(), rango(null, 15, 16, "   "), 201)).getMotivo()).isNull();
    }

    @Test
    void rechazaHorariosInvalidos() throws Exception {
        String token = lucas();
        crear(token, rango(null, 14, 13, null), 400);   // hasta antes que desde
        crear(token, rango(null, 13, 13, null), 400);   // rango vacío
        crear(token, """
                {"fecha":"%s","horaDesde":"13:00"}
                """.formatted(lunes), 400);              // solo una de las dos horas
        crear(token, """
                {"horaDesde":"13:00","horaHasta":"14:00"}
                """, 400);                               // sin fecha
    }

    // ---------- Permisos ----------

    @Test
    void barberoNoPuedeBloquearAOtroBarbero() throws Exception {
        crear(lucas(), rango(ANDRES, 13, 14, null), 403);
    }

    @Test
    void adminTieneQueElegirBarberoYPuedeBloquearACualquiera() throws Exception {
        String token = admin(); // el admin del seed no está vinculado a un barbero
        crear(token, rango(null, 13, 14, null), 400);
        Bloqueo b = bloqueoDe(crear(token, rango(ANDRES, 13, 14, null), 201));
        assertThat(b.getBarbero().getId()).isEqualTo(UUID.fromString(ANDRES));
    }

    // ---------- Turnos en conflicto ----------

    private void reservar(String barberoId, int hora, String telefono) throws Exception {
        mvc.perform(post("/api/public/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sucursalId":"%s","servicioId":"%s","barberoId":"%s","fecha":"%s","hora":"%02d:00",
                                 "clienteNombre":"Cliente Test","clienteTelefono":"%s"}
                                """.formatted(SUCURSAL, SERVICIO_CORTE, barberoId, lunes, hora, telefono)))
                .andExpect(status().isCreated());
    }

    @Test
    void avisaQueTurnosReservadosQuedanTapadosPorElBloqueo() throws Exception {
        reservar(LUCAS, 10, "+5493584990010"); // Corte de 30 min: 10:00-10:30
        String token = lucas();
        long bloqueosAntes = bloqueos.count();

        mvc.perform(post("/api/bloqueos/conflictos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(rango(null, 9, 11, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].clienteNombre").value("Cliente Test"))
                .andExpect(jsonPath("$[0].clienteTelefono").value("+5493584990010"))
                .andExpect(jsonPath("$[0].servicioNombre").value("Corte"));

        // Un bloqueo que solo roza el final del turno (10:30) no lo tapa
        mvc.perform(post("/api/bloqueos/conflictos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"fecha":"%s","horaDesde":"10:30","horaHasta":"12:00"}
                                """.formatted(lunes)))
                .andExpect(jsonPath("$", hasSize(0)));

        // Consultar conflictos no guarda nada
        assertThat(bloqueos.count()).isEqualTo(bloqueosAntes);
    }

    @Test
    void elDiaCompletoTambienDetectaConflictos() throws Exception {
        reservar(LUCAS, 17, "+5493584990011");
        mvc.perform(post("/api/bloqueos/conflictos").header("Authorization", lucas())
                        .contentType(MediaType.APPLICATION_JSON).content(diaCompleto(null)))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void losTurnosDeOtroBarberoNoSonConflicto() throws Exception {
        reservar(ANDRES, 10, "+5493584990012");
        mvc.perform(post("/api/bloqueos/conflictos").header("Authorization", lucas())
                        .contentType(MediaType.APPLICATION_JSON).content(rango(null, 9, 11, null)))
                .andExpect(jsonPath("$", hasSize(0)));
    }

        // ---------- Varios barberos a la vez ----------

    private long bloqueosDe(String barberoId) {
        return bloqueos.findSolapados(UUID.fromString(barberoId), a(lunes, 0, 0), a(lunes.plusDays(1), 0, 0)).size();
    }

    @Test
    void adminBloqueaVariosBarberosDeUnaVez() throws Exception {
        String respuesta = crear(admin(), rangoVarios(new String[]{LUCAS, ANDRES}, 13, 14, "Feriado"), 201);

        assertThat((List<?>) JsonPath.read(respuesta, "$")).hasSize(2); // uno por barbero
        assertThat(bloqueosDe(LUCAS)).isEqualTo(1);
        assertThat(bloqueosDe(ANDRES)).isEqualTo(1);
    }

    @Test
    void unBarberoRepetidoEnLaListaSeBloqueaUnaSolaVez() throws Exception {
        String respuesta = crear(admin(), rangoVarios(new String[]{LUCAS, LUCAS}, 13, 14, null), 201);
        assertThat((List<?>) JsonPath.read(respuesta, "$")).hasSize(1);
    }

    @Test
    void siUnBarberoDeLaListaNoExisteNoSeCreaNingunBloqueo() throws Exception {
        String token = admin();
        crear(token, rangoVarios(new String[]{LUCAS, UUID.randomUUID().toString()}, 13, 14, null), 404);
        assertThat(bloqueosDe(LUCAS)).isZero();
    }

    @Test
    void barberoNoPuedeIncluirAOtroBarberoEnLaLista() throws Exception {
        crear(lucas(), rangoVarios(new String[]{LUCAS, ANDRES}, 13, 14, null), 403);
        assertThat(bloqueosDe(LUCAS)).isZero();
        assertThat(bloqueosDe(ANDRES)).isZero();
    }

    @Test
    void losConflictosSeBuscanEnTodosLosBarberosElegidos() throws Exception {
        reservar(LUCAS, 10, "+5493584990014");
        reservar(ANDRES, 10, "+5493584990015");

        mvc.perform(post("/api/bloqueos/conflictos").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rangoVarios(new String[]{LUCAS, ANDRES}, 9, 11, null)))
                .andExpect(jsonPath("$", hasSize(2)));

        // Solo con Lucas tildado, el turno de Andrés no cuenta
        mvc.perform(post("/api/bloqueos/conflictos").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rangoVarios(new String[]{LUCAS}, 9, 11, null)))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void alEditarSoloSePuedeElegirUnBarbero() throws Exception {
        String token = admin();
        String id = JsonPath.read(crear(token, rango(LUCAS, 13, 14, null), 201), "$[0].id");

        mvc.perform(put("/api/bloqueos/" + id).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rangoVarios(new String[]{LUCAS, ANDRES}, 13, 14, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.barberoIds").exists());
    }

    // ---------- Editar y borrar ----------

    @Test
    void editaUnBloqueo() throws Exception {
        String token = lucas();
        String creado = crear(token, rango(null, 13, 14, "Almuerzo"), 201);
        String id = JsonPath.read(creado, "$[0].id");

        mvc.perform(put("/api/bloqueos/" + id).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(rango(null, 12, 15, "Trámite")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivo").value("Trámite"));

        Bloqueo b = bloqueos.findById(UUID.fromString(id)).orElseThrow();
        assertThat(b.getInicio()).isEqualTo(a(lunes, 12, 0));
        assertThat(b.getFin()).isEqualTo(a(lunes, 15, 0));
    }

    @Test
    void borraUnBloqueo() throws Exception {
        String token = lucas();
        String id = JsonPath.read(crear(token, rango(null, 13, 14, null), 201), "$[0].id");

        mvc.perform(delete("/api/bloqueos/" + id).header("Authorization", token))
                .andExpect(status().isNoContent());
        assertThat(bloqueos.findById(UUID.fromString(id))).isEmpty();

        mvc.perform(delete("/api/bloqueos/" + id).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void barberoNoPuedeEditarNiBorrarBloqueosDeOtro() throws Exception {
        String id = JsonPath.read(crear(admin(), rango(ANDRES, 13, 14, null), 201), "$[0].id");
        String token = lucas();

        mvc.perform(put("/api/bloqueos/" + id).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(rango(null, 12, 15, null)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/bloqueos/" + id).header("Authorization", token))
                .andExpect(status().isForbidden());
        assertThat(bloqueos.findById(UUID.fromString(id))).isPresent();
    }

    // ---------- Impacto en la disponibilidad (issue #9) ----------

    private ResultActions horariosLibresDeLucas() throws Exception {
        return mvc.perform(get("/api/public/disponibilidad")
                .param("sucursal", SUCURSAL).param("servicio", SERVICIO_CORTE)
                .param("barbero", LUCAS).param("fecha", lunes.toString()));
    }

    @Test
    void unBloqueoSacaLosHorariosDeLaGrillaPublicaYImpideReservarlos() throws Exception {
        horariosLibresDeLucas().andExpect(jsonPath("$.slots[*].hora", hasItem(startsWith("11:00"))));

        crear(lucas(), rango(null, 10, 13, "Trámite"), 201);

        horariosLibresDeLucas()
                .andExpect(jsonPath("$.slots[*].hora", not(hasItem(startsWith("10:00")))))
                .andExpect(jsonPath("$.slots[*].hora", not(hasItem(startsWith("11:00")))))
                .andExpect(jsonPath("$.slots[*].hora", not(hasItem(startsWith("12:30")))))
                .andExpect(jsonPath("$.slots[*].hora", hasItem(startsWith("16:00")))); // la tarde sigue libre

        // Y aunque alguien intente reservar a mano un horario bloqueado, se rechaza
        mvc.perform(post("/api/public/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sucursalId":"%s","servicioId":"%s","barberoId":"%s","fecha":"%s","hora":"11:00",
                                 "clienteNombre":"Cliente Test","clienteTelefono":"+5493584990013"}
                                """.formatted(SUCURSAL, SERVICIO_CORTE, LUCAS, lunes)))
                .andExpect(status().isConflict());
    }

    @Test
    void unBloqueoDeDiaCompletoQuitaEseDiaDelCalendario() throws Exception {
        String diasLibres = "/api/public/disponibilidad/dias";
        mvc.perform(get(diasLibres).param("sucursal", SUCURSAL).param("servicio", SERVICIO_CORTE)
                        .param("barbero", LUCAS).param("desde", lunes.toString()).param("hasta", lunes.toString()))
                .andExpect(jsonPath("$.dias", hasItem(lunes.toString())));

        crear(lucas(), diaCompleto(null), 201);

        mvc.perform(get(diasLibres).param("sucursal", SUCURSAL).param("servicio", SERVICIO_CORTE)
                        .param("barbero", LUCAS).param("desde", lunes.toString()).param("hasta", lunes.toString()))
                .andExpect(jsonPath("$.dias", not(hasItem(lunes.toString()))));
    }

    @Test
    void alBorrarElBloqueoLosHorariosVuelven() throws Exception {
        String token = lucas();
        String id = JsonPath.read(crear(token, rango(null, 10, 13, null), 201), "$[0].id");
        horariosLibresDeLucas().andExpect(jsonPath("$.slots[*].hora", not(hasItem(startsWith("10:00")))));

        mvc.perform(delete("/api/bloqueos/" + id).header("Authorization", token)).andExpect(status().isNoContent());

        horariosLibresDeLucas().andExpect(jsonPath("$.slots[*].hora", hasItem(startsWith("10:00"))));
    }
}
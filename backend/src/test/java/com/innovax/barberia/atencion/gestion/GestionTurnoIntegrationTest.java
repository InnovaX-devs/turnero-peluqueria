package com.innovax.barberia.atencion.gestion;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.OrigenAtencion;
import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.barbero.BarberoRepository;
import com.innovax.barberia.cliente.ClienteRepository;
import com.innovax.barberia.disponibilidad.DisponibilidadService;
import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.servicio.ServicioRepository;
import com.innovax.barberia.sucursal.SucursalRepository;
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

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cancelar y reprogramar un turno desde el link del cliente (/api/public/turnos/{token}).
 * NO es @Transactional: así se puede comprobar que, si reprogramar falla, el turno queda
 * como estaba (el servicio revierte su propia transacción). Lo creado se borra en @AfterEach.
 */
@SpringBootTest
class GestionTurnoIntegrationTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    private static final UUID JADE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID SUCURSAL = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private static final UUID LUCAS = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID CHAPA = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    private static final UUID CORTE = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
    private static final UUID CORTE_Y_BARBA = UUID.fromString("dddddddd-0000-0000-0000-000000000003");
    private static final UUID JUAN = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001");

    @Autowired private WebApplicationContext context;
    @Autowired private AtencionRepository atenciones;
    @Autowired private BarberiaRepository barberias;
    @Autowired private SucursalRepository sucursales;
    @Autowired private BarberoRepository barberos;
    @Autowired private ServicioRepository servicios;
    @Autowired private ClienteRepository clientes;
    @Autowired private DisponibilidadService disponibilidad;

    private MockMvc mvc;
    private final List<UUID> creadas = new ArrayList<>();

    /** Un lunes al menos 10 días adelante: Lucas trabaja de 10 a 13 y de 16 a 21. */
    private LocalDate lunes;
    /** Corte y Barba (45 min) con Lucas, el lunes a las 10. */
    private Atencion turno;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        lunes = LocalDate.now(ZONA).plusDays(10); // lejos del lunes que usa ReservaOnlineIntegrationTest
        while (lunes.getDayOfWeek() != DayOfWeek.MONDAY) {
            lunes = lunes.plusDays(1);
        }
        turno = crear(LUCAS, CORTE_Y_BARBA, en(lunes, 10, 0), 45);
    }

    @AfterEach
    void limpiar() {
        creadas.forEach(atenciones::deleteById);
        Barberia jade = barberias.findById(JADE).orElseThrow();
        jade.setAnticipacionCancelacionMin(120);
        jade.setReservasOnline(true);
        barberias.save(jade);
    }

    private static Instant en(LocalDate dia, int hora, int minuto) {
        return ZonedDateTime.of(dia, LocalTime.of(hora, minuto), ZONA).toInstant();
    }

    private Atencion crear(UUID barbero, UUID servicio, Instant inicio, int duracion) {
        Atencion a = new Atencion();
        a.setBarberia(barberias.findById(JADE).orElseThrow());
        a.setSucursal(sucursales.findById(SUCURSAL).orElseThrow());
        a.setBarbero(barberos.findById(barbero).orElseThrow());
        a.setServicio(servicios.findById(servicio).orElseThrow());
        a.setCliente(clientes.findById(JUAN).orElseThrow());
        a.setInicio(inicio);
        a.setFin(inicio.plus(Duration.ofMinutes(duracion)));
        a.setEstado(EstadoAtencion.RESERVADA);
        a.setOrigen(OrigenAtencion.ONLINE);
        a.setPrecioFinal(1700000);
        Atencion guardada = atenciones.save(a);
        creadas.add(guardada.getId());
        return guardada;
    }

    private void configurar(int anticipacionCancelacionMin, boolean reservasOnline) {
        Barberia jade = barberias.findById(JADE).orElseThrow();
        jade.setAnticipacionCancelacionMin(anticipacionCancelacionMin);
        jade.setReservasOnline(reservasOnline);
        barberias.save(jade);
    }

    private ResultActions ver(UUID token) throws Exception {
        return mvc.perform(get("/api/public/turnos/{token}", token));
    }

    private ResultActions cancelar(UUID token) throws Exception {
        return mvc.perform(post("/api/public/turnos/{token}/cancelar", token));
    }

    private ResultActions reprogramar(UUID token, LocalDate fecha, String hora) throws Exception {
        return mvc.perform(put("/api/public/turnos/{token}/reprogramar", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fecha\":\"%s\",\"hora\":\"%s\"}".formatted(fecha, hora)));
    }

    private Atencion recargar(Atencion a) {
        return atenciones.findById(a.getId()).orElseThrow();
    }

    // ---------- Ver ----------

    @Test
    void muestraElTurnoSinLogin() throws Exception {
        ver(turno.getTokenGestion())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESERVADA"))
                .andExpect(jsonPath("$.servicio.nombre").value("Corte y Barba"))
                .andExpect(jsonPath("$.barbero.nombre").value("Lucas Sanchez"))
                .andExpect(jsonPath("$.clienteNombre").value("Juan Gomez"))
                .andExpect(jsonPath("$.barberia.slug").value("jade"))
                .andExpect(jsonPath("$.anticipacionCancelacionMin").value(120))
                .andExpect(jsonPath("$.puedeCancelar").value(true))
                .andExpect(jsonPath("$.puedeReprogramar").value(true));
    }

    @Test
    void tokenInexistenteDevuelve404() throws Exception {
        ver(UUID.randomUUID()).andExpect(status().isNotFound());
        cancelar(UUID.randomUUID()).andExpect(status().isNotFound());
    }

    // ---------- Cancelar ----------

    @Test
    void cancelarLiberaElHorario() throws Exception {
        assertThat(disponibilidad.estaDisponible(SUCURSAL, CORTE_Y_BARBA, LUCAS, lunes, LocalTime.of(10, 0)))
                .isFalse();

        cancelar(turno.getTokenGestion())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.puedeCancelar").value(false));

        Atencion cancelada = recargar(turno);
        assertThat(cancelada.getEstado()).isEqualTo(EstadoAtencion.CANCELADA);
        assertThat(cancelada.getEstadoCambiadoEn()).isNotNull();
        assertThat(disponibilidad.estaDisponible(SUCURSAL, CORTE_Y_BARBA, LUCAS, lunes, LocalTime.of(10, 0)))
                .isTrue();
    }

    @Test
    void noSePuedeCancelarDosVeces() throws Exception {
        cancelar(turno.getTokenGestion()).andExpect(status().isOk());
        cancelar(turno.getTokenGestion())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.estado").value("Este turno ya estaba cancelado"));
    }

    @Test
    void conMenosDeLaAnticipacionNoSePuedeCancelar() throws Exception {
        // Falta 1 hora y la barbería pide 2
        Atencion pronto = crear(CHAPA, CORTE, Instant.now().plus(Duration.ofHours(1)), 30);

        ver(pronto.getTokenGestion())
                .andExpect(jsonPath("$.puedeCancelar").value(false))
                .andExpect(jsonPath("$.puedeReprogramar").value(false));
        cancelar(pronto.getTokenGestion())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.turno").value(containsString("2 horas")));
        assertThat(recargar(pronto).getEstado()).isEqualTo(EstadoAtencion.RESERVADA);
    }

    @Test
    void laAnticipacionEsConfigurable() throws Exception {
        configurar(30, true);
        Atencion pronto = crear(CHAPA, CORTE, Instant.now().plus(Duration.ofHours(1)), 30);

        cancelar(pronto.getTokenGestion()).andExpect(status().isOk());
    }

    // ---------- Reprogramar ----------

    @Test
    void reprogramaAunqueSeSuperpongaConSuPropioHorarioYMantieneElLink() throws Exception {
        // 10:00-10:45 → 10:30-11:15: se pisa con el turno actual, pero es el mismo
        reprogramar(turno.getTokenGestion(), lunes, "10:30")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(turno.getTokenGestion().toString()))
                .andExpect(jsonPath("$.estado").value("RESERVADA"));

        Atencion movido = recargar(turno);
        assertThat(movido.getEstado()).isEqualTo(EstadoAtencion.RESERVADA);
        assertThat(movido.getInicio()).isEqualTo(en(lunes, 10, 30));
        assertThat(movido.getFin()).isEqualTo(en(lunes, 11, 15));
    }

    @Test
    void siElHorarioNuevoEstaOcupadoDevuelve409YNoTocaElTurno() throws Exception {
        crear(LUCAS, CORTE, en(lunes, 12, 0), 30);

        // 11:30-12:15 se pisa con el de las 12
        reprogramar(turno.getTokenGestion(), lunes, "11:30")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.horario").exists());

        Atencion igual = recargar(turno);
        assertThat(igual.getEstado()).isEqualTo(EstadoAtencion.RESERVADA);
        assertThat(igual.getInicio()).isEqualTo(en(lunes, 10, 0));
    }

    @Test
    void fueraDelHorarioDelBarberoDevuelve409() throws Exception {
        // De 13 a 16 Lucas no trabaja
        reprogramar(turno.getTokenGestion(), lunes, "14:00").andExpect(status().isConflict());
        assertThat(recargar(turno).getInicio()).isEqualTo(en(lunes, 10, 0));
    }

    @Test
    void conReservasOnlineApagadasNoSeReprogramaPeroSeCancela() throws Exception {
        configurar(120, false);

        ver(turno.getTokenGestion())
                .andExpect(jsonPath("$.puedeCancelar").value(true))
                .andExpect(jsonPath("$.puedeReprogramar").value(false));
        reprogramar(turno.getTokenGestion(), lunes, "11:00").andExpect(status().isBadRequest());
        cancelar(turno.getTokenGestion()).andExpect(status().isOk());
    }

    @Test
    void siElServicioSeDesactivoNoSeReprogramaPeroSeCancela() throws Exception {
        Servicio corteYBarba = servicios.findById(CORTE_Y_BARBA).orElseThrow();
        corteYBarba.setActivo(false);
        servicios.save(corteYBarba);
        try {
            ver(turno.getTokenGestion()).andExpect(jsonPath("$.puedeReprogramar").value(false));
            reprogramar(turno.getTokenGestion(), lunes, "11:00").andExpect(status().isBadRequest());
            cancelar(turno.getTokenGestion()).andExpect(status().isOk());
        } finally {
            corteYBarba.setActivo(true);
            servicios.save(corteYBarba);
        }
    }

    @Test
    void unTurnoCanceladoNoSePuedeReprogramar() throws Exception {
        cancelar(turno.getTokenGestion()).andExpect(status().isOk());
        reprogramar(turno.getTokenGestion(), lunes, "11:00").andExpect(status().isConflict());
    }

    @Test
    void reprogramarSinFechaDevuelve400() throws Exception {
        mvc.perform(put("/api/public/turnos/{token}/reprogramar", turno.getTokenGestion())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"hora\":\"11:00\"}"))
                .andExpect(status().isBadRequest());
    }
}

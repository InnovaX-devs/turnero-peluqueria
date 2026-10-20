package com.innovax.barberia.reporte;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.MetodoPago;
import com.innovax.barberia.atencion.OrigenAtencion;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.barbero.BarberoRepository;
import com.innovax.barberia.cliente.ClienteRepository;
import com.innovax.barberia.servicio.ServicioRepository;
import com.innovax.barberia.sucursal.SucursalRepository;
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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reportes: GET /api/reportes y /api/reportes/clientes-inactivos.
 * El admin ve toda la barbería; un barbero, solo lo suyo.
 * Crea atenciones en marzo de 2020 para no cruzarse con otros tests. @Transactional: todo se revierte.
 */
@SpringBootTest
@Transactional
class ReportesIntegrationTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    private static final LocalDate MARTES_10 = LocalDate.of(2020, 3, 10);
    private static final LocalDate MARTES_17 = LocalDate.of(2020, 3, 17);
    private static final LocalDate JUEVES_12 = LocalDate.of(2020, 3, 12);
    private static final LocalDate SABADO_14 = LocalDate.of(2020, 3, 14);
    private static final LocalDate DESDE = LocalDate.of(2020, 3, 1);
    private static final LocalDate HASTA = LocalDate.of(2020, 3, 31);

    private static final UUID JADE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID SUCURSAL = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private static final UUID LUCAS = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID ANDRES = UUID.fromString("cccccccc-0000-0000-0000-000000000002");
    private static final UUID CHAPA = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    private static final UUID CORTE = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
    private static final String JUAN = "eeeeeeee-0000-0000-0000-000000000001";
    private static final String MARTIN = "eeeeeeee-0000-0000-0000-000000000002";

    @Autowired private WebApplicationContext context;
    @Autowired private AtencionRepository atenciones;
    @Autowired private BarberiaRepository barberias;
    @Autowired private SucursalRepository sucursales;
    @Autowired private BarberoRepository barberos;
    @Autowired private ServicioRepository servicios;
    @Autowired private ClienteRepository clientes;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        // Juan vino dos martes a las 10 (reservó online)
        crear(LUCAS, en(MARTES_10, 10), EstadoAtencion.COMPLETADA, OrigenAtencion.ONLINE, JUAN, 1500000);
        crear(LUCAS, en(MARTES_17, 10), EstadoAtencion.COMPLETADA, OrigenAtencion.ONLINE, JUAN, 1500000);
        // Martín reservó y no vino
        crear(ANDRES, en(MARTES_10, 11), EstadoAtencion.AUSENTE, OrigenAtencion.ONLINE, MARTIN, 1500000);
        // Uno avisó que no venía
        crear(CHAPA, en(JUEVES_12, 15), EstadoAtencion.CANCELADA, OrigenAtencion.ONLINE, MARTIN, 1500000);
        // Un walk-in sin cliente: cuenta como corte de Chapa, pero no para el ausentismo
        crear(CHAPA, en(SABADO_14, 10), EstadoAtencion.COMPLETADA, OrigenAtencion.MANUAL, null, 1500000);
        // Un turno que ya pasó y nadie marcó
        crear(ANDRES, en(SABADO_14, 12), EstadoAtencion.RESERVADA, OrigenAtencion.ONLINE, MARTIN, 1500000);
        atenciones.flush();
    }

    private static Instant en(LocalDate dia, int hora) {
        return ZonedDateTime.of(dia, LocalTime.of(hora, 0), ZONA).toInstant();
    }

    private void crear(UUID barbero, Instant inicio, EstadoAtencion estado, OrigenAtencion origen,
                       String cliente, long precio) {
        Atencion a = new Atencion();
        a.setBarberia(barberias.findById(JADE).orElseThrow());
        a.setSucursal(sucursales.findById(SUCURSAL).orElseThrow());
        a.setBarbero(barberos.findById(barbero).orElseThrow());
        a.setServicio(servicios.findById(CORTE).orElseThrow());
        a.setCliente(cliente == null ? null : clientes.findById(UUID.fromString(cliente)).orElseThrow());
        a.setInicio(inicio);
        a.setFin(inicio.plus(Duration.ofMinutes(30)));
        a.setEstado(estado);
        a.setOrigen(origen);
        a.setPrecioFinal(precio);
        a.setMetodoPago(estado == EstadoAtencion.COMPLETADA ? MetodoPago.EFECTIVO : null);
        atenciones.save(a);
    }

    private String admin() throws Exception {
        return login("admin@jade.com", "admin123");
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private ResultActions reportes(String token) throws Exception {
        return mvc.perform(get("/api/reportes")
                .param("desde", DESDE.toString())
                .param("hasta", HASTA.toString())
                .header("Authorization", token));
    }

    @Test
    void cuentaLosCortesDeCadaBarberoPorEstado() throws Exception {
        reportes(admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porBarbero", hasSize(3)))
                .andExpect(jsonPath("$.porBarbero[0].nombre").value("Lucas Sanchez"))
                .andExpect(jsonPath("$.porBarbero[0].atendidos").value(2))
                .andExpect(jsonPath("$.porBarbero[0].facturado").value(3000000))
                // Solo suma lo cobrado en cortes atendidos: el cancelado de Chapa no cuenta
                .andExpect(jsonPath("$.porBarbero[?(@.nombre == 'Chapa Perez')].facturado").value(hasItem(1500000)))
                // Andrés no atendió a nadie: su turno ausente y el sin marcar no suman plata
                .andExpect(jsonPath("$.porBarbero[?(@.nombre == 'Andres Biassi')].facturado").value(hasItem(0)))
                .andExpect(jsonPath("$.porBarbero[?(@.nombre == 'Chapa Perez')].atendidos").value(hasItem(1)))
                .andExpect(jsonPath("$.porBarbero[?(@.nombre == 'Chapa Perez')].cancelados").value(hasItem(1)))
                .andExpect(jsonPath("$.porBarbero[?(@.nombre == 'Andres Biassi')].ausentes").value(hasItem(1)));
    }

    @Test
    void losClientesMasFrecuentesSonLosQueMasVinieron() throws Exception {
        reportes(admin())
                .andExpect(status().isOk())
                // Martín no cuenta: nunca fue atendido. El walk-in no tiene cliente.
                .andExpect(jsonPath("$.clientesFrecuentes", hasSize(1)))
                .andExpect(jsonPath("$.clientesFrecuentes[0].nombre").value("Juan Gomez"))
                .andExpect(jsonPath("$.clientesFrecuentes[0].visitas").value(2))
                .andExpect(jsonPath("$.clientesFrecuentes[0].total").value(3000000))
                .andExpect(jsonPath("$.clientesFrecuentes[0].ultimaVisita").value("2020-03-17"));
    }

    @Test
    void elAusentismoSoloMiraTurnosOnline() throws Exception {
        reportes(admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ausentismo.atendidos").value(2))
                .andExpect(jsonPath("$.ausentismo.ausentes").value(1))
                .andExpect(jsonPath("$.ausentismo.cancelados").value(1))
                .andExpect(jsonPath("$.ausentismo.sinMarcar").value(1));
    }

    @Test
    void laDemandaAgrupaPorDiaYHoraSinContarCancelados() throws Exception {
        reportes(admin())
                .andExpect(status().isOk())
                // Martes (2) a las 10: los dos turnos de Juan
                .andExpect(jsonPath("$.demanda[0].diaSemana").value(2))
                .andExpect(jsonPath("$.demanda[0].hora").value(10))
                .andExpect(jsonPath("$.demanda[0].cantidad").value(2))
                // El cancelado del jueves (4) a las 15 no aparece
                .andExpect(jsonPath("$.demanda[?(@.diaSemana == 4)]", hasSize(0)))
                // Martes 11 (ausente), sábado 10 (walk-in) y sábado 12 (sin marcar)
                .andExpect(jsonPath("$.demanda", hasSize(4)));
    }

    @Test
    void listaLosClientesQueNoVuelvenHaceTiempo() throws Exception {
        mvc.perform(get("/api/reportes/clientes-inactivos").param("dias", "30").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barberiaNombre").value("Jade"))
                .andExpect(jsonPath("$.clientes[?(@.nombre == 'Juan Gomez')].visitas").value(hasItem(2)))
                .andExpect(jsonPath("$.clientes[?(@.nombre == 'Juan Gomez')].ultimaVisita").value(hasItem("2020-03-17")))
                // Martín nunca fue atendido: no es un cliente "que dejó de venir"
                .andExpect(jsonPath("$.clientes[*].nombre").value(not(hasItem("Martin Lopez"))));
    }

    @Test
    void noListaAQuienYaTieneUnTurnoReservado() throws Exception {
        crear(LUCAS, Instant.now().plus(Duration.ofDays(3)), EstadoAtencion.RESERVADA, OrigenAtencion.ONLINE,
                JUAN, 1500000);
        atenciones.flush();

        mvc.perform(get("/api/reportes/clientes-inactivos").param("dias", "30").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientes[*].nombre").value(not(hasItem("Juan Gomez"))));
    }

    @Test
    void diasFueraDeRangoDevuelve400() throws Exception {
        mvc.perform(get("/api/reportes/clientes-inactivos").param("dias", "2").header("Authorization", admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hastaAnteriorADesdeDevuelve400() throws Exception {
        mvc.perform(get("/api/reportes")
                        .param("desde", HASTA.toString())
                        .param("hasta", DESDE.toString())
                        .header("Authorization", admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elBarberoVeSoloSusPropiosReportes() throws Exception {
        reportes(login("lucas@jade.com", "barbero123"))
                .andExpect(status().isOk())
                // Solo Lucas: nada de Andrés ni Chapa
                .andExpect(jsonPath("$.porBarbero", hasSize(1)))
                .andExpect(jsonPath("$.porBarbero[0].nombre").value("Lucas Sanchez"))
                .andExpect(jsonPath("$.porBarbero[0].atendidos").value(2))
                // Lo que juntó Lucas: dos cortes de $15.000
                .andExpect(jsonPath("$.porBarbero[0].facturado").value(3000000))
                .andExpect(jsonPath("$.clientesFrecuentes", hasSize(1)))
                .andExpect(jsonPath("$.clientesFrecuentes[0].nombre").value("Juan Gomez"))
                // El ausente, el cancelado y el turno sin marcar son de otros barberos
                .andExpect(jsonPath("$.ausentismo.atendidos").value(2))
                .andExpect(jsonPath("$.ausentismo.ausentes").value(0))
                .andExpect(jsonPath("$.ausentismo.cancelados").value(0))
                .andExpect(jsonPath("$.ausentismo.sinMarcar").value(0))
                // Solo los dos martes a las 10 de Juan
                .andExpect(jsonPath("$.demanda", hasSize(1)))
                .andExpect(jsonPath("$.demanda[0].cantidad").value(2));
    }

    @Test
    void elBarberoNoPuedeElegirOtroBarberoDesdeElRequest() throws Exception {
        // No existe un parámetro para elegir barbero: aunque lo manden, se ignora.
        mvc.perform(get("/api/reportes")
                        .param("desde", DESDE.toString())
                        .param("hasta", HASTA.toString())
                        .param("barberoId", ANDRES.toString())
                        .header("Authorization", login("lucas@jade.com", "barbero123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porBarbero", hasSize(1)))
                .andExpect(jsonPath("$.porBarbero[0].nombre").value("Lucas Sanchez"));
    }

    @Test
    void losClientesQueNoVuelvenDelBarberoSonSoloLosSuyos() throws Exception {
        // Martín se hizo un corte con Andrés: es cliente de Andrés, no de Lucas
        crear(ANDRES, en(MARTES_17, 11), EstadoAtencion.COMPLETADA, OrigenAtencion.ONLINE, MARTIN, 1500000);
        atenciones.flush();

        mvc.perform(get("/api/reportes/clientes-inactivos").param("dias", "30")
                        .header("Authorization", login("lucas@jade.com", "barbero123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientes[*].nombre").value(hasItem("Juan Gomez")))
                .andExpect(jsonPath("$.clientes[*].nombre").value(not(hasItem("Martin Lopez"))));

        mvc.perform(get("/api/reportes/clientes-inactivos").param("dias", "30").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientes[*].nombre").value(hasItem("Martin Lopez")));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/reportes").param("desde", DESDE.toString()).param("hasta", HASTA.toString()))
                .andExpect(status().isUnauthorized());
    }
}
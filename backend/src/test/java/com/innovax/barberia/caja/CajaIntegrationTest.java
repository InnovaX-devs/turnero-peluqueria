package com.innovax.barberia.caja;

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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caja: GET /api/caja y /api/caja/exportar.
 * Usa el seed de dev y crea atenciones en marzo de 2020 para no cruzarse con otros tests.
 * @Transactional: todo lo que crea se revierte.
 */
@SpringBootTest
@Transactional
class CajaIntegrationTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    private static final LocalDate DIA_1 = LocalDate.of(2020, 3, 10);
    private static final LocalDate DIA_2 = LocalDate.of(2020, 3, 11);

    private static final UUID JADE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID SUCURSAL = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private static final UUID LUCAS = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID ANDRES = UUID.fromString("cccccccc-0000-0000-0000-000000000002");
    private static final UUID CHAPA = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    private static final UUID CORTE = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
    private static final UUID BARBA = UUID.fromString("dddddddd-0000-0000-0000-000000000002");
    private static final UUID JUAN = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001");

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

        crear(LUCAS, CORTE, DIA_1, 10, 0, 30, EstadoAtencion.COMPLETADA, 1500000, MetodoPago.EFECTIVO, JUAN);
        crear(ANDRES, BARBA, DIA_1, 10, 0, 15, EstadoAtencion.COMPLETADA, 500000, MetodoPago.MERCADOPAGO, null);
        crear(LUCAS, CORTE, DIA_2, 10, 0, 30, EstadoAtencion.COMPLETADA, 1200000, MetodoPago.EFECTIVO, null);
        // No cuentan: todavía no se atendió o se canceló
        crear(CHAPA, CORTE, DIA_1, 12, 0, 30, EstadoAtencion.RESERVADA, 1500000, null, null);
        crear(CHAPA, CORTE, DIA_1, 14, 0, 30, EstadoAtencion.CANCELADA, 1500000, null, null);
        atenciones.flush();
    }

    private void crear(UUID barbero, UUID servicio, LocalDate dia, int hora, int minuto, int duracion,
                       EstadoAtencion estado, long precio, MetodoPago metodo, UUID cliente) {
        Atencion a = new Atencion();
        a.setBarberia(barberias.findById(JADE).orElseThrow());
        a.setSucursal(sucursales.findById(SUCURSAL).orElseThrow());
        a.setBarbero(barberos.findById(barbero).orElseThrow());
        a.setServicio(servicios.findById(servicio).orElseThrow());
        a.setCliente(cliente == null ? null : clientes.findById(cliente).orElseThrow());
        var inicio = ZonedDateTime.of(dia, LocalTime.of(hora, minuto), ZONA).toInstant();
        a.setInicio(inicio);
        a.setFin(inicio.plus(Duration.ofMinutes(duracion)));
        a.setEstado(estado);
        a.setOrigen(OrigenAtencion.MANUAL);
        a.setPrecioFinal(precio);
        a.setMetodoPago(metodo);
        atenciones.save(a);
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private ResultActions consultar(String token, LocalDate desde, LocalDate hasta) throws Exception {
        return mvc.perform(get("/api/caja")
                .param("desde", desde.toString())
                .param("hasta", hasta.toString())
                .header("Authorization", token));
    }

    @Test
    void sumaSoloLasAtencionesCompletadasDelPeriodo() throws Exception {
        consultar(login("admin@jade.com", "admin123"), DIA_1, DIA_2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3200000))
                .andExpect(jsonPath("$.cantidad").value(3))
                .andExpect(jsonPath("$.ticketPromedio").value(1066666));
    }

    @Test
    void desglosaPorBarberoServicioYMetodoOrdenadoPorTotal() throws Exception {
        consultar(login("admin@jade.com", "admin123"), DIA_1, DIA_2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porBarbero", hasSize(2)))
                .andExpect(jsonPath("$.porBarbero[0].nombre").value("Lucas Sanchez"))
                .andExpect(jsonPath("$.porBarbero[0].cantidad").value(2))
                .andExpect(jsonPath("$.porBarbero[0].total").value(2700000))
                .andExpect(jsonPath("$.porServicio[0].nombre").value("Corte"))
                .andExpect(jsonPath("$.porServicio[0].total").value(2700000))
                .andExpect(jsonPath("$.porMetodoPago[0].metodo").value("EFECTIVO"))
                .andExpect(jsonPath("$.porMetodoPago[0].total").value(2700000))
                .andExpect(jsonPath("$.porMetodoPago[1].metodo").value("MERCADOPAGO"));
    }

    @Test
    void devuelveTodosLosDiasDelPeriodoAunqueNoHayaCortes() throws Exception {
        consultar(login("admin@jade.com", "admin123"), DIA_1, DIA_1.plusDays(4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porDia", hasSize(5)))
                .andExpect(jsonPath("$.porDia[0].total").value(2000000))
                .andExpect(jsonPath("$.porDia[1].total").value(1200000))
                .andExpect(jsonPath("$.porDia[2].total").value(0));
    }

    @Test
    void elDiaSeCuentaEnLaHoraDeLaBarberiaNoEnUtc() throws Exception {
        // 23:30 en Córdoba ya es el día siguiente en UTC: tiene que contar para el día 12
        LocalDate dia3 = LocalDate.of(2020, 3, 12);
        crear(LUCAS, CORTE, dia3, 23, 30, 30, EstadoAtencion.COMPLETADA, 1000000, MetodoPago.TARJETA, null);
        atenciones.flush();

        consultar(login("admin@jade.com", "admin123"), dia3, dia3)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1000000))
                .andExpect(jsonPath("$.cantidad").value(1));
    }

    @Test
    void exportaElDetalleEnCsvParaExcel() throws Exception {
        byte[] csv = mvc.perform(get("/api/caja/exportar")
                        .param("desde", DIA_1.toString())
                        .param("hasta", DIA_2.toString())
                        .header("Authorization", login("admin@jade.com", "admin123")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("text/csv")))
                .andExpect(header().string("Content-Disposition", startsWith("attachment")))
                .andReturn().getResponse().getContentAsByteArray();

        String texto = new String(csv, StandardCharsets.UTF_8);
        assertThat(texto).contains("Fecha;Hora;Barbero;Servicio;Cliente;Método de pago;Precio");
        assertThat(texto).contains("10/03/2020;10:00;Lucas Sanchez;Corte;Juan Gomez;EFECTIVO;15000,00");
        assertThat(texto).contains("Sin cliente");
        assertThat(texto.lines().count()).isEqualTo(4); // encabezado + 3 completadas
    }

    @Test
    void barberoNoPuedeVerLaCaja() throws Exception {
        consultar(login("lucas@jade.com", "barbero123"), DIA_1, DIA_2)
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/caja").param("desde", DIA_1.toString()).param("hasta", DIA_2.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hastaAnteriorADesdeDevuelve400() throws Exception {
        consultar(login("admin@jade.com", "admin123"), DIA_2, DIA_1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.hasta").exists());
    }

    @Test
    void masDeUnAnioDevuelve400() throws Exception {
        consultar(login("admin@jade.com", "admin123"), DIA_1, DIA_1.plusDays(400))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sucursalInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/caja")
                        .param("desde", DIA_1.toString())
                        .param("hasta", DIA_2.toString())
                        .param("sucursal", UUID.randomUUID().toString())
                        .header("Authorization", login("admin@jade.com", "admin123")))
                .andExpect(status().isNotFound());
    }
}

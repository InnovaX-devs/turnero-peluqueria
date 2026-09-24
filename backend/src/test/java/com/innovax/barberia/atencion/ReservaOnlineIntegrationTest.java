package com.innovax.barberia.atencion;

import com.innovax.barberia.cliente.Cliente;
import com.innovax.barberia.cliente.ClienteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Usa los datos del seed de dev (necesita la base levantada).
 *
 * <p>Deliberadamente NO es {@code @Transactional}: el test de concurrencia necesita que cada
 * request corra en su propia transacción real contra la base (si todo corriera en la
 * transacción del test, no habría carrera posible que probar). Por eso cada test limpia a mano
 * lo que crea.
 */
@SpringBootTest
class ReservaOnlineIntegrationTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    private static final String SUCURSAL = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final String SERVICIO_CORTE = "dddddddd-0000-0000-0000-000000000001"; // 30 min
    private static final String BARBERO_LUCAS = "cccccccc-0000-0000-0000-000000000001";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private AtencionRepository atenciones;
    @Autowired
    private ClienteRepository clientes;

    private MockMvc mvc;
    private LocalDate fecha;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // Próximo lunes: siempre futuro, dentro del horario laboral del seed (lun-sáb) y
        // ampliamente dentro de la anticipación mínima/máxima por defecto.
        fecha = LocalDate.now(ZONA).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    @AfterEach
    void tearDown() {
        atenciones.findAll().stream()
                .filter(a -> a.getBarbero().getId().equals(UUID.fromString(BARBERO_LUCAS)))
                .filter(a -> a.getInicio().equals(inicioEsperado(LocalTime.of(10, 0))))
                .forEach(atenciones::delete);
        clientes.findAll().stream()
                .filter(c -> c.getTelefono().startsWith("+549358499"))
                .forEach(clientes::delete);
    }

    private Instant inicioEsperado(LocalTime hora) {
        return ZonedDateTime.of(fecha, hora, ZONA).toInstant();
    }

    private String body(LocalTime hora, String telefono) {
        return """
                {"sucursalId":"%s","servicioId":"%s","barberoId":"%s",
                 "fecha":"%s","hora":"%s","clienteNombre":"Cliente Test","clienteTelefono":"%s"}
                """.formatted(SUCURSAL, SERVICIO_CORTE, BARBERO_LUCAS, fecha, hora, telefono);
    }

    // ---------- Camino feliz ----------

    @Test
    void reservaUnHorarioLibre() throws Exception {
        mvc.perform(post("/api/public/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(LocalTime.of(10, 0), "+5493584990001")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RESERVADA"))
                .andExpect(jsonPath("$.tokenGestion").exists());

        Cliente creado = clientes.findByBarberiaIdAndTelefono(
                        UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"), "+5493584990001")
                .orElseThrow();
        assertThat(creado.getNombre()).isEqualTo("Cliente Test");
    }

    // ---------- El chequeo previo rechaza un horario ya tomado ----------

    @Test
    void siElHorarioYaEstaTomadoDevuelve409() throws Exception {
        mvc.perform(post("/api/public/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(LocalTime.of(10, 0), "+5493584990002")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/public/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(LocalTime.of(10, 0), "+5493584990003")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ese horario se acaba de ocupar"))
                .andExpect(jsonPath("$.errores.horario").exists());
    }

    // ---------- Concurrencia: dos (varias) reservas simultáneas del mismo horario ----------

    @Test
    void dosReservasSimultaneasDelMismoHorarioSoloUnaGana() throws Exception {
        LocalTime hora = LocalTime.of(10, 0);
        int intentos = 8;

        ExecutorService pool = Executors.newFixedThreadPool(intentos);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();

        for (int i = 0; i < intentos; i++) {
            String telefono = "+549358499%04d".formatted(1000 + i);
            resultados.add(pool.submit(() -> {
                largada.await();
                return mvc.perform(post("/api/public/reservas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(hora, telefono)))
                        .andReturn().getResponse().getStatus();
            }));
        }
        largada.countDown(); // todos los hilos disparan la request al mismo tiempo

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> resultado : resultados) {
            statuses.add(resultado.get(15, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(intentos - 1);

        long reservasGuardadas = atenciones.findAll().stream()
                .filter(a -> a.getBarbero().getId().equals(UUID.fromString(BARBERO_LUCAS)))
                .filter(a -> a.getInicio().equals(inicioEsperado(hora)))
                .filter(a -> a.getEstado() == EstadoAtencion.RESERVADA)
                .count();
        assertThat(reservasGuardadas).isEqualTo(1);
    }
}
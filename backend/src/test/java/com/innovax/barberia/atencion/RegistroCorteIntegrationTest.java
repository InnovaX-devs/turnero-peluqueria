package com.innovax.barberia.atencion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Usa los datos del seed de dev (necesita la base levantada).
 * @Transactional: todo lo que crea se revierte. Los cortes "pasados" usan una fecha lejana.
 */
@SpringBootTest
@Transactional
class RegistroCorteIntegrationTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    private static final LocalDate PASADO = LocalDate.of(2020, 3, 2);
    private static final String CORTE = "dddddddd-0000-0000-0000-000000000001"; // 30 min
    private static final String JUAN = "eeeeeeee-0000-0000-0000-000000000001";
    private static final UUID LUCAS = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID SUCURSAL = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private static final String ANDRES = "cccccccc-0000-0000-0000-000000000002";

    
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private AtencionRepository atenciones;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
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

    private String registrar(String token, String json, int statusEsperado) throws Exception {
        return mvc.perform(post("/api/atenciones").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(statusEsperado))
                .andReturn().getResponse().getContentAsString();
    }

    private Atencion atencionDe(String respuesta) {
        return atenciones.findById(UUID.fromString(JsonPath.read(respuesta, "$.id"))).orElseThrow();
    }

    private static String corteEnElPasado(String hora) {
        return """
                {"servicioId":"%s","precioFinal":1500000,"metodoPago":"EFECTIVO","fecha":"%s","hora":"%s"}
                """.formatted(CORTE, PASADO, hora);
    }

    private static Instant enElPasado(int hora, int minuto) {
        return ZonedDateTime.of(PASADO, LocalTime.of(hora, minuto), ZONA).toInstant();
    }

    @Test
    void barberoRegistraUnCorteAnonimoConLoMinimo() throws Exception {
        String respuesta = registrar(lucas(), """
                {"servicioId":"%s","precioFinal":1500000,"metodoPago":"EFECTIVO"}
                """.formatted(CORTE), 201);

        Atencion a = atencionDe(respuesta);
        assertThat(a.getEstado()).isEqualTo(EstadoAtencion.COMPLETADA);
        assertThat(a.getOrigen()).isEqualTo(OrigenAtencion.MANUAL);
        assertThat(a.getMetodoPago()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(a.getCliente()).isNull();
        assertThat(a.getBarbero().getId()).isEqualTo(LUCAS);
        assertThat(a.getSucursal().getId()).isEqualTo(SUCURSAL); // su única sucursal
        assertThat(Duration.between(a.getInicio(), Instant.now()).abs()).isLessThan(Duration.ofMinutes(1));
    }

    @Test
    void registraConClientePrecioEditadoFechaPasadaYNotas() throws Exception {
        String respuesta = registrar(lucas(), """
                {"servicioId":"%s","clienteId":"%s","precioFinal":1200000,"metodoPago":"MERCADOPAGO",
                 "notas":"Degradé en 2","fecha":"%s","hora":"18:30"}
                """.formatted(CORTE, JUAN, PASADO), 201);

        Atencion a = atencionDe(respuesta);
        assertThat(a.getCliente().getNombre()).isEqualTo("Juan Gomez");
        assertThat(a.getPrecioFinal()).isEqualTo(1200000);
        assertThat(a.getNotas()).isEqualTo("Degradé en 2");
        assertThat(a.getInicio()).isEqualTo(enElPasado(18, 30));
        assertThat(a.getFin()).isEqualTo(enElPasado(19, 0));
    }

    @Test
    void noSePuedeRegistrarUnCorteEnElFuturo() throws Exception {
        registrar(lucas(), """
                {"servicioId":"%s","precioFinal":1500000,"metodoPago":"EFECTIVO","fecha":"2099-01-01","hora":"10:00"}
                """.formatted(CORTE), 400);
    }

    @Test
    void barberoNoPuedeRegistrarParaOtroBarbero() throws Exception {
        registrar(lucas(), """
                {"servicioId":"%s","barberoId":"%s","precioFinal":1500000,"metodoPago":"EFECTIVO"}
                """.formatted(CORTE, ANDRES), 403);
    }

    @Test
    void adminTieneQueElegirElBarbero() throws Exception {
        String token = admin(); // el admin del seed no está vinculado a un barbero
        registrar(token, corteEnElPasado("10:00"), 400);
        registrar(token, """
                {"servicioId":"%s","barberoId":"%s","precioFinal":1500000,"metodoPago":"TARJETA"}
                """.formatted(CORTE, ANDRES), 201);
    }

    @Test
    void dosCortesSeguidosNoChocanAunqueDurenMenosQueElServicio() throws Exception {
        String token = lucas();
        String primero = registrar(token, corteEnElPasado("10:00"), 201);
        registrar(token, corteEnElPasado("10:20"), 201); // el Corte dura 30 min nominales

        assertThat(atencionDe(primero).getFin()).isEqualTo(enElPasado(10, 20));
    }

    @Test
    void buscaClientesPorNombreYPorTelefono() throws Exception {
        String token = lucas();
        mvc.perform(get("/api/clientes").header("Authorization", token).param("q", "juan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Juan Gomez"));
        mvc.perform(get("/api/clientes").header("Authorization", token).param("q", "4222"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Martin Lopez"));
    }

    @Test
    void noCreaDosClientesConElMismoTelefono() throws Exception {
        mvc.perform(post("/api/clientes").header("Authorization", lucas())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Otro\",\"telefono\":\"+5493584111111\"}"))
                .andExpect(status().isConflict());
    }
}


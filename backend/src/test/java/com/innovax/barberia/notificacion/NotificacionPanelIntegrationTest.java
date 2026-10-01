package com.innovax.barberia.notificacion;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.OrigenAtencion;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.barbero.BarberoRepository;
import com.innovax.barberia.cliente.ClienteRepository;
import com.innovax.barberia.servicio.ServicioRepository;
import com.innovax.barberia.sucursal.SucursalRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los mensajes pendientes en el panel: listarlos con su link de WhatsApp y marcarlos como enviados.
 * Usa los datos del seed: admin@jade.com (ADMIN) y lucas@jade.com (BARBERO, vinculado a Lucas).
 */
@SpringBootTest
class NotificacionPanelIntegrationTest {

    private static final UUID JADE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID SUCURSAL = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private static final UUID LUCAS = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID ANDRES = UUID.fromString("cccccccc-0000-0000-0000-000000000002");
    private static final UUID CORTE = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
    private static final UUID JUAN = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001");

    @Autowired private WebApplicationContext context;
    @Autowired private AtencionRepository atenciones;
    @Autowired private BarberiaRepository barberias;
    @Autowired private SucursalRepository sucursales;
    @Autowired private BarberoRepository barberos;
    @Autowired private ServicioRepository servicios;
    @Autowired private ClienteRepository clientes;
    @Autowired private NotificacionRepository notificaciones;

    private MockMvc mvc;
    private final List<UUID> creadas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void limpiar() {
        creadas.forEach(atenciones::deleteById); // las notificaciones se van con el turno (ON DELETE CASCADE)
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    /** Un turno de dentro de 3 días con un mensaje de confirmación pendiente, para el barbero dado. */
    private Notificacion mensajePendiente(UUID barbero, EstadoAtencion estadoTurno) {
        Instant inicio = Instant.now().plus(3, ChronoUnit.DAYS);
        Atencion a = new Atencion();
        a.setBarberia(barberias.findById(JADE).orElseThrow());
        a.setSucursal(sucursales.findById(SUCURSAL).orElseThrow());
        a.setBarbero(barberos.findById(barbero).orElseThrow());
        a.setServicio(servicios.findById(CORTE).orElseThrow());
        a.setCliente(clientes.findById(JUAN).orElseThrow());
        a.setInicio(inicio);
        a.setFin(inicio.plus(Duration.ofMinutes(30)));
        a.setEstado(estadoTurno);
        a.setOrigen(OrigenAtencion.ONLINE);
        a.setPrecioFinal(1000000);
        Atencion turno = atenciones.save(a);
        creadas.add(turno.getId());

        Notificacion n = new Notificacion();
        n.setBarberia(a.getBarberia());
        n.setAtencion(turno);
        n.setTipo(TipoNotificacion.CONFIRMACION);
        n.setCanal(TipoCanal.WHATSAPP_LINK);
        n.setDestinatario("+5493584111111");
        n.setMensaje("Hola Juan, tu turno quedó confirmado");
        n.setProgramadaPara(Instant.now());
        return notificaciones.save(n);
    }

    private List<String> idsListados(String token) throws Exception {
        String json = mvc.perform(get("/api/notificaciones").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$[*].id");
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/notificaciones")).andExpect(status().isUnauthorized());
    }

    @Test
    void elAdminVeLosPendientesConElLinkDeWhatsApp() throws Exception {
        Notificacion n = mensajePendiente(LUCAS, EstadoAtencion.RESERVADA);
        String admin = login("admin@jade.com", "admin123");

        String json = mvc.perform(get("/api/notificaciones").header("Authorization", admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> links = JsonPath.read(json, "$[?(@.id=='" + n.getId() + "')].linkWhatsApp");
        assertThat(links).hasSize(1);
        assertThat(links.get(0)).startsWith("https://wa.me/5493584111111?text=");
        List<String> clientes = JsonPath.read(json, "$[?(@.id=='" + n.getId() + "')].clienteNombre");
        assertThat(clientes).containsExactly("Juan Gomez");
    }

    @Test
    void marcarEnviadaLaPasaAEnviadaYNoSePuedeRepetir() throws Exception {
        Notificacion n = mensajePendiente(LUCAS, EstadoAtencion.RESERVADA);
        String admin = login("admin@jade.com", "admin123");

        mvc.perform(post("/api/notificaciones/{id}/enviada", n.getId()).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENVIADA"))
                .andExpect(jsonPath("$.enviadaEn").exists());

        assertThat(idsListados(admin)).doesNotContain(n.getId().toString());

        mvc.perform(post("/api/notificaciones/{id}/enviada", n.getId()).header("Authorization", admin))
                .andExpect(status().isConflict());
    }

    @Test
    void noSePuedeMarcarEnviadoUnMensajeDeUnTurnoCancelado() throws Exception {
        Notificacion n = mensajePendiente(LUCAS, EstadoAtencion.CANCELADA);
        String admin = login("admin@jade.com", "admin123");

        mvc.perform(post("/api/notificaciones/{id}/enviada", n.getId()).header("Authorization", admin))
                .andExpect(status().isConflict());
    }

    @Test
    void unBarberoVeSoloLosMensajesDeSusTurnos() throws Exception {
        Notificacion deLucas = mensajePendiente(LUCAS, EstadoAtencion.RESERVADA);
        Notificacion deAndres = mensajePendiente(ANDRES, EstadoAtencion.RESERVADA);
        String lucas = login("lucas@jade.com", "barbero123");

        List<String> ids = idsListados(lucas);

        assertThat(ids).contains(deLucas.getId().toString());
        assertThat(ids).doesNotContain(deAndres.getId().toString());
    }

    @Test
    void unBarberoNoPuedeMarcarElMensajeDeOtroBarbero() throws Exception {
        Notificacion deAndres = mensajePendiente(ANDRES, EstadoAtencion.RESERVADA);
        String lucas = login("lucas@jade.com", "barbero123");

        mvc.perform(post("/api/notificaciones/{id}/enviada", deAndres.getId()).header("Authorization", lucas))
                .andExpect(status().isForbidden());
    }

    @Test
    void unMensajeInexistenteDevuelve404() throws Exception {
        String admin = login("admin@jade.com", "admin123");

        mvc.perform(post("/api/notificaciones/{id}/enviada", UUID.randomUUID()).header("Authorization", admin))
                .andExpect(status().isNotFound());
    }
}
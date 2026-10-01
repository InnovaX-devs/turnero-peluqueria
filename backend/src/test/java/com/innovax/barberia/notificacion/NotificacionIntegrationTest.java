package com.innovax.barberia.notificacion;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.OrigenAtencion;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.barbero.BarberoRepository;
import com.innovax.barberia.cliente.Cliente;
import com.innovax.barberia.cliente.ClienteRepository;
import com.innovax.barberia.servicio.ServicioRepository;
import com.innovax.barberia.sucursal.SucursalRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirmación al reservar y recordatorio automático (issue de notificaciones).
 *
 * <p>NO es {@code @Transactional}: el servicio abre sus propias transacciones, igual que en
 * producción. El job programado está desactivado en los tests (application.properties), así
 * que cada test lo "ejecuta" llamando a {@link NotificacionService#procesarPendientes()}.
 * Lo creado se borra en {@code @AfterEach} (las notificaciones se van con su turno por el
 * ON DELETE CASCADE).
 */
@SpringBootTest
class NotificacionIntegrationTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    private static final UUID JADE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID SUCURSAL = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private static final UUID LUCAS = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID CORTE = UUID.fromString("dddddddd-0000-0000-0000-000000000001"); // 30 min
    private static final UUID JUAN = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001");

    private static final String TELEFONO_RESERVA = "+5493584990001";

    @Autowired private WebApplicationContext context;
    @Autowired private AtencionRepository atenciones;
    @Autowired private BarberiaRepository barberias;
    @Autowired private SucursalRepository sucursales;
    @Autowired private BarberoRepository barberos;
    @Autowired private ServicioRepository servicios;
    @Autowired private ClienteRepository clientes;
    @Autowired private NotificacionRepository notificaciones;
    @Autowired private NotificacionService servicio;
    @Autowired private JdbcTemplate jdbc;

    private MockMvc mvc;
    private final List<UUID> creadas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void limpiar() {
        creadas.forEach(atenciones::deleteById);
        limpiarCliente(TELEFONO_RESERVA);
    }

    private void limpiarCliente(String telefono) {
        clientes.findByBarberiaIdAndTelefono(JADE, telefono).ifPresent(cliente -> {
            atenciones.findAll().stream()
                    .filter(a -> a.getCliente() != null && a.getCliente().getId().equals(cliente.getId()))
                    .forEach(atenciones::delete);
            clientes.delete(cliente);
        });
    }

    /**
     * Turno RESERVADA (o el estado dado) de Lucas, de 30 minutos. Si {@code reservadoHaceDias},
     * se simula que se reservó hace dos días: created_at no se puede setear desde la entidad
     * (es @CreationTimestamp), por eso va por SQL.
     */
    private Atencion turno(UUID clienteId, Instant inicio, EstadoAtencion estado, boolean reservadoHaceDias) {
        Atencion a = new Atencion();
        a.setBarberia(barberias.findById(JADE).orElseThrow());
        a.setSucursal(sucursales.findById(SUCURSAL).orElseThrow());
        a.setBarbero(barberos.findById(LUCAS).orElseThrow());
        a.setServicio(servicios.findById(CORTE).orElseThrow());
        a.setCliente(clientes.findById(clienteId).orElseThrow());
        a.setInicio(inicio);
        a.setFin(inicio.plus(Duration.ofMinutes(30)));
        a.setEstado(estado);
        a.setOrigen(OrigenAtencion.ONLINE);
        a.setPrecioFinal(1000000);
        Atencion guardada = atenciones.save(a);
        creadas.add(guardada.getId());

        if (reservadoHaceDias) {
            jdbc.update("update atencion set created_at = ? where id = ?",
                    Timestamp.from(Instant.now().minus(2, ChronoUnit.DAYS)), guardada.getId());
        }
        return guardada;
    }

    private List<Notificacion> recordatorios(Atencion a) {
        return notificaciones.findByAtencionIdOrderByCreatedAtAsc(a.getId()).stream()
                .filter(n -> n.getTipo() == TipoNotificacion.RECORDATORIO)
                .toList();
    }

    // ---------- Confirmación al reservar ----------

    @Test
    void alReservarOnlineSeGuardaLaConfirmacionConLosDatosDelTurno() throws Exception {
        LocalDate lunes = LocalDate.now(ZONA).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        String body = """
                {"sucursalId":"%s","servicioId":"%s","barberoId":"%s",
                 "fecha":"%s","hora":"11:30","clienteNombre":"Cliente Test","clienteTelefono":"%s"}
                """.formatted(SUCURSAL, CORTE, LUCAS, lunes, TELEFONO_RESERVA);

        mvc.perform(post("/api/public/reservas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        Cliente cliente = clientes.findByBarberiaIdAndTelefono(JADE, TELEFONO_RESERVA).orElseThrow();
        Atencion reservada = atenciones.findAll().stream()
                .filter(a -> a.getCliente() != null && a.getCliente().getId().equals(cliente.getId()))
                .findFirst().orElseThrow();

        List<Notificacion> registradas = notificaciones.findByAtencionIdOrderByCreatedAtAsc(reservada.getId());
        assertThat(registradas).hasSize(1);
        Notificacion confirmacion = registradas.get(0);
        assertThat(confirmacion.getTipo()).isEqualTo(TipoNotificacion.CONFIRMACION);
        assertThat(confirmacion.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(confirmacion.getCanal()).isEqualTo(TipoCanal.WHATSAPP_LINK);
        assertThat(confirmacion.getDestinatario()).isEqualTo(TELEFONO_RESERVA);
        assertThat(confirmacion.getMensaje()).contains(
                "Cliente Test",
                "11:30 hs",
                "Lucas Sanchez",
                "Sobremonte 240",
                "/turno/" + reservada.getTokenGestion());
    }

    // ---------- Recordatorio ----------

    @Test
    void elRecordatorioSeEnviaCuandoYaLeToca() {
        // Reservado hace 2 días, empieza en 23 h: el recordatorio (24 h antes) ya le toca.
        Atencion a = turno(JUAN, Instant.now().plus(23, ChronoUnit.HOURS), EstadoAtencion.RESERVADA, true);

        servicio.procesarPendientes();

        List<Notificacion> enviados = recordatorios(a);
        assertThat(enviados).hasSize(1);
        Notificacion recordatorio = enviados.get(0);
        assertThat(recordatorio.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(recordatorio.getEnviadaEn()).isNotNull();
        assertThat(recordatorio.getError()).isNull();
        assertThat(recordatorio.getMensaje()).contains("Te recordamos", "/turno/" + a.getTokenGestion());
    }

    @Test
    void siFaltaMasDeLoConfiguradoTodaviaNoSeCreaElRecordatorio() {
        // Empieza en 3 días: faltan más de 24 h, todavía no toca.
        Atencion a = turno(JUAN, Instant.now().plus(3, ChronoUnit.DAYS), EstadoAtencion.RESERVADA, true);

        servicio.procesarPendientes();

        assertThat(recordatorios(a)).isEmpty();
    }

    @Test
    void noSeEnviaRecordatorioSiElTurnoEstaCancelado() {
        Atencion a = turno(JUAN, Instant.now().plus(23, ChronoUnit.HOURS), EstadoAtencion.CANCELADA, true);

        servicio.procesarPendientes();

        assertThat(recordatorios(a)).isEmpty();
    }

    @Test
    void siSeCancelaDespuesDeProgramarseQuedaOmitido() {
        Atencion a = turno(JUAN, Instant.now().plus(23, ChronoUnit.HOURS), EstadoAtencion.RESERVADA, true);

        // Un recordatorio pendiente cuya hora ya llegó...
        Notificacion pendiente = new Notificacion();
        pendiente.setBarberia(barberias.findById(JADE).orElseThrow());
        pendiente.setAtencion(a);
        pendiente.setTipo(TipoNotificacion.RECORDATORIO);
        pendiente.setCanal(TipoCanal.WHATSAPP_LINK);
        pendiente.setDestinatario("+5493584111111");
        pendiente.setMensaje("Recordatorio de prueba");
        pendiente.setProgramadaPara(Instant.now().minus(1, ChronoUnit.MINUTES));
        notificaciones.save(pendiente);

        // ...pero el turno se cancela antes de que corra el job.
        Atencion cancelada = atenciones.findById(a.getId()).orElseThrow();
        cancelada.setEstado(EstadoAtencion.CANCELADA);
        atenciones.save(cancelada);

        servicio.procesarPendientes();

        List<Notificacion> registradas = recordatorios(a);
        assertThat(registradas).hasSize(1);
        assertThat(registradas.get(0).getEstado()).isEqualTo(EstadoNotificacion.OMITIDA);
        assertThat(registradas.get(0).getError()).contains("ya no está reservado");
        assertThat(registradas.get(0).getEnviadaEn()).isNull();
    }

    @Test
    void noDuplicaElRecordatorioSiElJobCorreDosVeces() {
        Atencion a = turno(JUAN, Instant.now().plus(23, ChronoUnit.HOURS), EstadoAtencion.RESERVADA, true);

        servicio.procesarPendientes();
        servicio.procesarPendientes();

        assertThat(recordatorios(a)).hasSize(1);
    }

    @Test
    void siSeReservoDentroDeLaVentanaDelRecordatorioQuedaOmitido() {
        // Reservado recién para dentro de 10 h: ya recibió la confirmación, no hace falta otro aviso.
        Atencion a = turno(JUAN, Instant.now().plus(10, ChronoUnit.HOURS), EstadoAtencion.RESERVADA, false);

        servicio.procesarPendientes();

        List<Notificacion> registradas = recordatorios(a);
        assertThat(registradas).hasSize(1);
        assertThat(registradas.get(0).getEstado()).isEqualTo(EstadoNotificacion.OMITIDA);
        assertThat(registradas.get(0).getError()).contains("ventana");
    }

    // ---------- Registro de fallidos ----------

    @Test
    void unMensajeQueFallaQuedaRegistradoComoFallidoConSuMotivo() {
        // Empieza en 3 días, así el job no le crea un recordatorio propio.
        Atencion a = turno(JUAN, Instant.now().plus(3, ChronoUnit.DAYS), EstadoAtencion.RESERVADA, false);

        // La base no deja guardar un cliente con teléfono inválido (chk_cliente_telefono), así que
        // se arma la notificación a mano con un destinatario sin código de país.
        Notificacion conTelefonoRaro = new Notificacion();
        conTelefonoRaro.setBarberia(barberias.findById(JADE).orElseThrow());
        conTelefonoRaro.setAtencion(a);
        conTelefonoRaro.setTipo(TipoNotificacion.CONFIRMACION);
        conTelefonoRaro.setCanal(TipoCanal.WHATSAPP_LINK);
        conTelefonoRaro.setDestinatario("3584990009");
        conTelefonoRaro.setMensaje("Confirmación de prueba");
        conTelefonoRaro.setProgramadaPara(Instant.now().minus(1, ChronoUnit.MINUTES));
        notificaciones.save(conTelefonoRaro);

        servicio.procesarPendientes();

        List<Notificacion> registradas = notificaciones.findByAtencionIdOrderByCreatedAtAsc(a.getId());
        assertThat(registradas).hasSize(1);
        assertThat(registradas.get(0).getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(registradas.get(0).getError()).contains("formato internacional");
        assertThat(registradas.get(0).getEnviadaEn()).isNull();
    }
}
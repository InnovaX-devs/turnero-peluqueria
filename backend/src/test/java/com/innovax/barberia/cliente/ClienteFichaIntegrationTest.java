package com.innovax.barberia.cliente;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.MetodoPago;
import com.innovax.barberia.atencion.OrigenAtencion;
import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.barbero.BarberoRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ficha del cliente (issue #20). Usa los datos del seed de dev (barbería Jade). */
@SpringBootTest
class ClienteFichaIntegrationTest {

    private static final String BARBERIA = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String SUCURSAL = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final String BARBERO_LUCAS = "cccccccc-0000-0000-0000-000000000001";
    private static final String BARBERO_ANDRES = "cccccccc-0000-0000-0000-000000000002";
    private static final String SERVICIO_CORTE = "dddddddd-0000-0000-0000-000000000001";
    private static final String SERVICIO_BARBA = "dddddddd-0000-0000-0000-000000000002";

    @Autowired private WebApplicationContext context;
    @Autowired private ClienteRepository clientes;
    @Autowired private AtencionRepository atenciones;
    @Autowired private BarberiaRepository barberias;
    @Autowired private SucursalRepository sucursales;
    @Autowired private ServicioRepository servicios;
    @Autowired private BarberoRepository barberos;

    private MockMvc mvc;
    private final List<UUID> atencionesCreadas = new ArrayList<>();
    private final List<UUID> clientesCreados = new ArrayList<>();
    private final List<UUID> barberiasCreadas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void tearDown() {
        atenciones.deleteAllById(atencionesCreadas);
        clientes.deleteAllById(clientesCreados);
        barberias.deleteAllById(barberiasCreadas);
    }

    private Cliente cliente(Barberia barberia, String nombre, String telefono, String notas) {
        Cliente c = new Cliente();
        c.setBarberia(barberia);
        c.setNombre(nombre);
        c.setTelefono(telefono);
        c.setNotas(notas);
        Cliente guardado = clientes.saveAndFlush(c);
        clientesCreados.add(guardado.getId());
        return guardado;
    }

    private Cliente clienteJade(String nombre, String telefono, String notas) {
        return cliente(barberias.getReferenceById(UUID.fromString(BARBERIA)), nombre, telefono, notas);
    }

    private Atencion atencion(Cliente cliente, String barberoId, String servicioId, Instant inicio,
                              EstadoAtencion estado, long precio) {
        Atencion a = new Atencion();
        a.setBarberia(barberias.getReferenceById(UUID.fromString(BARBERIA)));
        a.setSucursal(sucursales.getReferenceById(UUID.fromString(SUCURSAL)));
        a.setBarbero(barberos.getReferenceById(UUID.fromString(barberoId)));
        a.setServicio(servicios.getReferenceById(UUID.fromString(servicioId)));
        a.setCliente(cliente);
        a.setInicio(inicio);
        a.setFin(inicio.plus(30, ChronoUnit.MINUTES));
        a.setEstado(estado);
        a.setOrigen(OrigenAtencion.MANUAL);
        a.setPrecioFinal(precio);
        if (estado == EstadoAtencion.COMPLETADA) {
            a.setMetodoPago(MetodoPago.EFECTIVO);
        }
        Atencion guardada = atenciones.saveAndFlush(a);
        atencionesCreadas.add(guardada.getId());
        return guardada;
    }

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

    @Test
    void sinTokenDevuelve401() throws Exception {
        Cliente c = clienteJade("Sin Token", "+5493584900001", null);
        mvc.perform(get("/api/clientes/listado")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/clientes/{id}", c.getId())).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/clientes/{id}/notas", c.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notas\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listadoSinBusquedaTraeLosClientesOrdenadosPorNombre() throws Exception {
        clienteJade("Zzz Listado Ultimo", "+5493584900002", null);
        clienteJade("Aaa Listado Primero", "+5493584900003", "con nota");

        mvc.perform(get("/api/clientes/listado").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("Aaa Listado Primero")))
                .andExpect(jsonPath("$[*].nombre", hasItem("Zzz Listado Ultimo")))
                .andExpect(jsonPath("$[?(@.nombre=='Aaa Listado Primero')].tieneNotas").value(true))
                .andExpect(jsonPath("$[?(@.nombre=='Zzz Listado Ultimo')].tieneNotas").value(false));
    }

    @Test
    void listadoBuscaPorNombreSinImportarMayusculas() throws Exception {
        clienteJade("Fulano Buscable", "+5493584900004", null);

        mvc.perform(get("/api/clientes/listado").param("q", "buscABLE").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Fulano Buscable"))
                .andExpect(jsonPath("$[0].telefono").value("+5493584900004"));
    }

    @Test
    void listadoBuscaPorParteDelTelefono() throws Exception {
        clienteJade("Telefonista", "+5493584977123", null);

        // Como la gente lo escribe: con espacios y guiones
        mvc.perform(get("/api/clientes/listado").param("q", "4977-123").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Telefonista"));
    }

    @Test
    void listadoNoMuestraClientesDeOtraBarberia() throws Exception {
        Barberia otra = new Barberia();
        otra.setNombre("Otra barberia");
        otra.setSlug("otra-listado-" + UUID.randomUUID().toString().substring(0, 8));
        otra = barberias.saveAndFlush(otra);
        barberiasCreadas.add(otra.getId());
        cliente(otra, "Cliente Ajeno Listado", "+5493584900005", null);

        mvc.perform(get("/api/clientes/listado").param("q", "Ajeno Listado").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void fichaMuestraDatosHistorialYAusencias() throws Exception {
        Cliente c = clienteJade("Cliente Ficha", "+5493584900006", "Degradé en 2, arriba tijera");
        Instant base = LocalDate.of(2020, 3, 10).atTime(15, 0).toInstant(ZoneOffset.UTC);
        atencion(c, BARBERO_LUCAS, SERVICIO_CORTE, base, EstadoAtencion.COMPLETADA, 1_500_000);
        atencion(c, BARBERO_ANDRES, SERVICIO_BARBA, base.plus(30, ChronoUnit.DAYS), EstadoAtencion.COMPLETADA, 500_000);
        atencion(c, BARBERO_LUCAS, SERVICIO_CORTE, base.plus(60, ChronoUnit.DAYS), EstadoAtencion.AUSENTE, 1_500_000);
        atencion(c, BARBERO_LUCAS, SERVICIO_CORTE, base.plus(90, ChronoUnit.DAYS), EstadoAtencion.AUSENTE, 1_500_000);

        mvc.perform(get("/api/clientes/{id}", c.getId()).header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Cliente Ficha"))
                .andExpect(jsonPath("$.telefono").value("+5493584900006"))
                .andExpect(jsonPath("$.notas").value("Degradé en 2, arriba tijera"))
                .andExpect(jsonPath("$.ausencias").value(2))
                .andExpect(jsonPath("$.historial", hasSize(4)))
                // De la más reciente a la más antigua
                .andExpect(jsonPath("$.historial[0].estado").value("AUSENTE"))
                .andExpect(jsonPath("$.historial[3].estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.historial[3].barberoNombre").value("Lucas Sanchez"))
                .andExpect(jsonPath("$.historial[3].servicioNombre").value("Corte"))
                .andExpect(jsonPath("$.historial[3].precioFinal").value(1_500_000))
                .andExpect(jsonPath("$.historial[2].barberoNombre").value("Andres Biassi"))
                .andExpect(jsonPath("$.historial[2].servicioNombre").value("Barba"))
                .andExpect(jsonPath("$.historial[2].precioFinal").value(500_000));
    }

    @Test
    void fichaDeClienteSinAtencionesTieneHistorialVacio() throws Exception {
        Cliente c = clienteJade("Cliente Nuevo", "+5493584900007", null);

        mvc.perform(get("/api/clientes/{id}", c.getId()).header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ausencias").value(0))
                .andExpect(jsonPath("$.historial", hasSize(0)))
                .andExpect(jsonPath("$.notas").doesNotExist());
    }

    @Test
    void fichaDeClienteInexistenteODeOtraBarberiaDevuelve404() throws Exception {
        Barberia otra = new Barberia();
        otra.setNombre("Otra barberia");
        otra.setSlug("otra-ficha-" + UUID.randomUUID().toString().substring(0, 8));
        otra = barberias.saveAndFlush(otra);
        barberiasCreadas.add(otra.getId());
        Cliente ajeno = cliente(otra, "Cliente Ajeno Ficha", "+5493584900008", "secreto");
        String token = admin();

        mvc.perform(get("/api/clientes/{id}", ajeno.getId()).header("Authorization", token))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/clientes/{id}/notas", ajeno.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .content("{\"notas\":\"pisado\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/clientes/{id}", UUID.randomUUID()).header("Authorization", token))
                .andExpect(status().isNotFound());

        // La nota del cliente ajeno quedó intacta
        org.junit.jupiter.api.Assertions.assertEquals("secreto", clientes.findById(ajeno.getId()).orElseThrow().getNotas());
    }

    @Test
    void sePuedeEditarYBorrarLaNota() throws Exception {
        Cliente c = clienteJade("Cliente Notas", "+5493584900009", "nota vieja");
        String token = admin();

        mvc.perform(patch("/api/clientes/{id}/notas", c.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .content("{\"notas\":\"  Degradé en 2, arriba tijera  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notas").value("Degradé en 2, arriba tijera"));

        mvc.perform(get("/api/clientes/{id}", c.getId()).header("Authorization", token))
                .andExpect(jsonPath("$.notas").value("Degradé en 2, arriba tijera"));

        // Una nota en blanco borra la nota
        mvc.perform(patch("/api/clientes/{id}/notas", c.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .content("{\"notas\":\"   \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notas").doesNotExist());
    }

    @Test
    void notaMuyLargaDevuelve400() throws Exception {
        Cliente c = clienteJade("Cliente Nota Larga", "+5493584900010", null);

        mvc.perform(patch("/api/clientes/{id}/notas", c.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", admin())
                        .content("{\"notas\":\"%s\"}".formatted("a".repeat(2001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.notas").exists());
    }

    @Test
    void unBarberoTambienPuedeVerLaFichaYEditarLaNota() throws Exception {
        Cliente c = clienteJade("Cliente Barbero", "+5493584900011", null);
        String token = login("lucas@jade.com", "barbero123");

        mvc.perform(get("/api/clientes/listado").param("q", "Cliente Barbero").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(patch("/api/clientes/{id}/notas", c.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .content("{\"notas\":\"Máquina en 1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notas").value("Máquina en 1"));
    }

    @Test
    void elDetalleDelTurnoTraeLaNotaDelCliente() throws Exception {
        Cliente c = clienteJade("Cliente Agenda", "+5493584900012", "Degradé en 2, arriba tijera");
        Instant inicio = Instant.now().plus(45, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Atencion turno = atencion(c, BARBERO_ANDRES, SERVICIO_CORTE, inicio, EstadoAtencion.RESERVADA, 1_500_000);
        LocalDate dia = inicio.atZone(ZoneOffset.UTC).toLocalDate();

        mvc.perform(get("/api/agenda")
                        .param("desde", dia.minusDays(1).toString())
                        .param("hasta", dia.plusDays(1).toString())
                        .header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos[?(@.id=='%s')].clienteNotas".formatted(turno.getId()))
                        .value("Degradé en 2, arriba tijera"))
                .andExpect(jsonPath("$.turnos[?(@.id=='%s')].clienteId".formatted(turno.getId()))
                        .value(c.getId().toString()));
    }
}
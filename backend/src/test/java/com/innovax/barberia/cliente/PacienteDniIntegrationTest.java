package com.innovax.barberia.cliente;

import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.negocio.Rubro;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pacientes identificados por DNI (issue #66). Usa los datos del seed de dev: para probar el rubro
 * SALUD se cambia el rubro de la barbería Jade y se lo devuelve a BARBERIA al terminar.
 *
 * <p>No es {@code @Transactional}: cada request corre en su propia transacción, así que cada test
 * limpia a mano lo que crea (los teléfonos de prueba empiezan con +549358488).
 */
@SpringBootTest
class PacienteDniIntegrationTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    private static final UUID NEGOCIO = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final String SUCURSAL = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final String SERVICIO = "dddddddd-0000-0000-0000-000000000001"; // 30 min
    private static final String PROFESIONAL = "cccccccc-0000-0000-0000-000000000001";
    private static final String PREFIJO_TELEFONO = "+549358488";

    @Autowired private WebApplicationContext context;
    @Autowired private ClienteRepository clientes;
    @Autowired private AtencionRepository atenciones;
    @Autowired private NegocioRepository negocios;

    private MockMvc mvc;
    private LocalDate fecha;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        fecha = LocalDate.now(ZONA).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    @AfterEach
    void tearDown() {
        List<UUID> ids = clientes.findAll().stream()
                .filter(c -> c.getTelefono().startsWith(PREFIJO_TELEFONO))
                .map(Cliente::getId)
                .toList();
        atenciones.deleteAll(atenciones.findAll().stream()
                .filter(a -> a.getCliente() != null && ids.contains(a.getCliente().getId()))
                .toList());
        clientes.deleteAllById(ids);
        conRubro(Rubro.BARBERIA);
    }

    private void conRubro(Rubro rubro) {
        Negocio jade = negocios.findById(NEGOCIO).orElseThrow();
        jade.setRubro(rubro);
        negocios.saveAndFlush(jade);
    }

    private String admin() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@jade.com\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private String reserva(LocalTime hora, String nombre, String telefono, String dni) {
        String campoDni = dni == null ? "" : ",\"clienteDni\":\"%s\"".formatted(dni);
        return """
                {"sucursalId":"%s","servicioId":"%s","profesionalId":"%s",
                 "fecha":"%s","hora":"%s","clienteNombre":"%s","clienteTelefono":"%s"%s}
                """.formatted(SUCURSAL, SERVICIO, PROFESIONAL, fecha, hora, nombre, telefono, campoDni);
    }

    private void reservar(LocalTime hora, String nombre, String telefono, String dni, int estadoEsperado)
            throws Exception {
        mvc.perform(post("/api/public/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(reserva(hora, nombre, telefono, dni)))
                .andExpect(status().is(estadoEsperado));
    }

    private long cuantosConTelefono(String telefono) {
        return clientes.findAll().stream()
                .filter(c -> c.getNegocio().getId().equals(NEGOCIO) && telefono.equals(c.getTelefono()))
                .count();
    }

    // ---------- Reserva pública en SALUD ----------

    @Test
    void dosPacientesConElMismoTelefonoYDistintoDniPuedenExistir() throws Exception {
        conRubro(Rubro.SALUD);
        String telefonoDeLaMadre = PREFIJO_TELEFONO + "01";

        reservar(LocalTime.of(10, 0), "Madre Paciente", telefonoDeLaMadre, "30111222", 201);
        reservar(LocalTime.of(10, 30), "Hijo Paciente", telefonoDeLaMadre, "50333444", 201);

        assertThat(cuantosConTelefono(telefonoDeLaMadre)).isEqualTo(2);
        assertThat(clientes.findByNegocioIdAndDni(NEGOCIO, "30111222").orElseThrow().getNombre())
                .isEqualTo("Madre Paciente");
        assertThat(clientes.findByNegocioIdAndDni(NEGOCIO, "50333444").orElseThrow().getNombre())
                .isEqualTo("Hijo Paciente");
    }

    @Test
    void elMismoDniEsElMismoPacienteAunqueCambieElTelefono() throws Exception {
        conRubro(Rubro.SALUD);

        reservar(LocalTime.of(10, 0), "Paciente Uno", PREFIJO_TELEFONO + "02", "30555666", 201);
        reservar(LocalTime.of(10, 30), "Paciente Uno Apellido", PREFIJO_TELEFONO + "03", "30.555.666", 201);

        assertThat(cuantosConTelefono(PREFIJO_TELEFONO + "02")).isZero();
        assertThat(cuantosConTelefono(PREFIJO_TELEFONO + "03")).isEqualTo(1);
        Cliente paciente = clientes.findByNegocioIdAndDni(NEGOCIO, "30555666").orElseThrow();
        assertThat(paciente.getNombre()).isEqualTo("Paciente Uno Apellido");
    }

    @Test
    void sinDniNoSeReservaEnSalud() throws Exception {
        conRubro(Rubro.SALUD);
        reservar(LocalTime.of(10, 0), "Sin Dni", PREFIJO_TELEFONO + "04", null, 400);
        assertThat(cuantosConTelefono(PREFIJO_TELEFONO + "04")).isZero();
    }

    @Test
    void unDniConFormatoInvalidoSeRechaza() throws Exception {
        conRubro(Rubro.SALUD);
        reservar(LocalTime.of(10, 0), "Dni Corto", PREFIJO_TELEFONO + "05", "123456", 400);      // 6 dígitos
        reservar(LocalTime.of(10, 0), "Dni Largo", PREFIJO_TELEFONO + "05", "123456789", 400);   // 9 dígitos
        reservar(LocalTime.of(10, 0), "Dni Letras", PREFIJO_TELEFONO + "05", "12A45678", 400);
        assertThat(cuantosConTelefono(PREFIJO_TELEFONO + "05")).isZero();
    }

    @Test
    void guardaFechaDeNacimientoYSexoSiVienen() throws Exception {
        conRubro(Rubro.SALUD);
        String body = """
                {"sucursalId":"%s","servicioId":"%s","profesionalId":"%s",
                 "fecha":"%s","hora":"10:00","clienteNombre":"Con Datos","clienteTelefono":"%s",
                 "clienteDni":"30777888","clienteFechaNacimiento":"1990-05-17","clienteSexo":"FEMENINO"}
                """.formatted(SUCURSAL, SERVICIO, PROFESIONAL, fecha, PREFIJO_TELEFONO + "06");
        mvc.perform(post("/api/public/reservas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        Cliente paciente = clientes.findByNegocioIdAndDni(NEGOCIO, "30777888").orElseThrow();
        assertThat(paciente.getFechaNacimiento()).isEqualTo(LocalDate.of(1990, 5, 17));
        assertThat(paciente.getSexo()).isEqualTo(Sexo.FEMENINO);
    }

    // ---------- Rubros que no son salud: todo sigue igual ----------

    @Test
    void enBarberiaSeSigueIdentificandoPorTelefonoYElDniNoPide() throws Exception {
        String telefono = PREFIJO_TELEFONO + "07";
        reservar(LocalTime.of(10, 0), "Cliente Uno", telefono, null, 201);
        reservar(LocalTime.of(10, 30), "Cliente Uno Renombrado", telefono, null, 201);

        assertThat(cuantosConTelefono(telefono)).isEqualTo(1);
        assertThat(clientes.findByNegocioIdAndTelefono(NEGOCIO, telefono).orElseThrow().getNombre())
                .isEqualTo("Cliente Uno Renombrado");
    }

    @Test
    void enBarberiaElAltaManualConTelefonoRepetidoSigueDandoConflicto() throws Exception {
        String token = admin();
        String body = "{\"nombre\":\"Cliente Manual\",\"telefono\":\"%s\"}".formatted(PREFIJO_TELEFONO + "08");

        mvc.perform(post("/api/clientes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dni").value(nullValue()));
        mvc.perform(post("/api/clientes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    // ---------- Alta manual y búsqueda en SALUD ----------

    @Test
    void altaManualEnSaludPideDniYNoRepiteDni() throws Exception {
        conRubro(Rubro.SALUD);
        String token = admin();
        String telefono = PREFIJO_TELEFONO + "09";

        mvc.perform(post("/api/clientes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Sin Dni\",\"telefono\":\"%s\"}".formatted(telefono)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/clientes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Con Dni\",\"telefono\":\"%s\",\"dni\":\"30.444.555\"}".formatted(telefono)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dni").value("30444555"));

        // mismo DNI → conflicto, aunque el teléfono sea otro
        mvc.perform(post("/api/clientes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Otro\",\"telefono\":\"%s\",\"dni\":\"30444555\"}".formatted(PREFIJO_TELEFONO + "10")))
                .andExpect(status().isConflict());

        // mismo teléfono con otro DNI → es otro paciente (madre e hijo)
        mvc.perform(post("/api/clientes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Hijo\",\"telefono\":\"%s\",\"dni\":\"50444555\"}".formatted(telefono)))
                .andExpect(status().isCreated());
        assertThat(cuantosConTelefono(telefono)).isEqualTo(2);
    }

    @Test
    void elListadoYElBuscadorEncuentranPacientesPorNombreOPorDni() throws Exception {
        conRubro(Rubro.SALUD);
        String token = admin();
        mvc.perform(post("/api/clientes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Zulema Buscable\",\"telefono\":\"%s\",\"dni\":\"31999888\"}".formatted(PREFIJO_TELEFONO + "11")))
                .andExpect(status().isCreated());

        // listado del panel: por DNI completo, por DNI con puntos y por nombre
        for (String consulta : List.of("31999888", "31.999.888", "Zulema")) {
            mvc.perform(get("/api/clientes/listado").param("q", consulta).header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clientes[*].nombre", hasItem("Zulema Buscable")))
                    .andExpect(jsonPath("$.clientes[?(@.nombre=='Zulema Buscable')].dni", hasItem("31999888")));
        }
        // buscador del turno / del registro de atención
        mvc.perform(get("/api/clientes").param("q", "999888").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("Zulema Buscable")));
        // un DNI que no existe no trae nada
        mvc.perform(get("/api/clientes/listado").param("q", "31000000").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientes", hasSize(0)));
    }

    @Test
    void laFichaDevuelveLosDatosPersonales() throws Exception {
        conRubro(Rubro.SALUD);
        String token = admin();
        String creado = mvc.perform(post("/api/clientes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ficha Paciente\",\"telefono\":\"%s\",\"dni\":\"32111222\",\"fechaNacimiento\":\"1985-03-02\",\"sexo\":\"MASCULINO\"}"
                                .formatted(PREFIJO_TELEFONO + "12")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(creado, "$.id");

        mvc.perform(get("/api/clientes/{id}", id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value("32111222"))
                .andExpect(jsonPath("$.fechaNacimiento").value("1985-03-02"))
                .andExpect(jsonPath("$.sexo").value("MASCULINO"));
    }
}

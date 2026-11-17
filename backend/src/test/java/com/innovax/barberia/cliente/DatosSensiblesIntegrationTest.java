package com.innovax.barberia.cliente;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.MetodoPago;
import com.innovax.barberia.atencion.OrigenAtencion;
import com.innovax.barberia.auditoria.AccesoPacienteRepository;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.profesional.ProfesionalRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Datos sensibles (issue #73, Ley 25.326): auditoría de acceso a la ficha, exportar y anonimizar un paciente.
 * Usa los datos del seed de dev (barbería Jade): los endpoints no dependen del rubro.
 */
@SpringBootTest
class DatosSensiblesIntegrationTest {

    private static final String NEGOCIO = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String SUCURSAL = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final String PROFESIONAL = "cccccccc-0000-0000-0000-000000000001";
    private static final String SERVICIO = "dddddddd-0000-0000-0000-000000000001";

    @Autowired private WebApplicationContext context;
    @Autowired private ClienteRepository clientes;
    @Autowired private AtencionRepository atenciones;
    @Autowired private AccesoPacienteRepository accesos;
    @Autowired private NegocioRepository negocios;
    @Autowired private SucursalRepository sucursales;
    @Autowired private ServicioRepository servicios;
    @Autowired private ProfesionalRepository profesionales;

    private MockMvc mvc;
    private final List<UUID> atencionesCreadas = new ArrayList<>();
    private final List<UUID> clientesCreados = new ArrayList<>();
    private int turnosCreados = 0;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void tearDown() {
        atenciones.deleteAllById(atencionesCreadas);
        // La auditoría no tiene claves foráneas: se limpia a mano lo que dejaron estos tests
        accesos.deleteAll(accesos.findAll().stream().filter(a -> clientesCreados.contains(a.getClienteId())).toList());
        clientes.deleteAllById(clientesCreados);
    }

    private Cliente paciente(String nombre, String telefono, String dni, String notas) {
        Cliente c = new Cliente();
        c.setNegocio(negocios.getReferenceById(UUID.fromString(NEGOCIO)));
        c.setNombre(nombre);
        c.setTelefono(telefono);
        c.setDni(dni);
        c.setNotas(notas);
        Cliente guardado = clientes.saveAndFlush(c);
        clientesCreados.add(guardado.getId());
        return guardado;
    }

    /** Un turno lejos en el futuro (no pisa el seed ni a otros tests): cada llamada usa una hora distinta. */
    private Atencion turno(Cliente cliente, EstadoAtencion estado, String notasDeConsulta) {
        Instant inicio = Instant.now().plus(600, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS)
                .plus(turnosCreados++, ChronoUnit.HOURS);
        Atencion a = new Atencion();
        a.setNegocio(negocios.getReferenceById(UUID.fromString(NEGOCIO)));
        a.setSucursal(sucursales.getReferenceById(UUID.fromString(SUCURSAL)));
        a.setProfesional(profesionales.getReferenceById(UUID.fromString(PROFESIONAL)));
        a.setServicio(servicios.getReferenceById(UUID.fromString(SERVICIO)));
        a.setCliente(cliente);
        a.setInicio(inicio);
        a.setFin(inicio.plus(30, ChronoUnit.MINUTES));
        a.setEstado(estado);
        a.setOrigen(OrigenAtencion.MANUAL);
        a.setPrecioFinal(5000);
        a.setNotas(notasDeConsulta);
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

    // ---------- Auditoría ----------

    @Test
    void abrirLaFichaDejaRegistroDeQuienLaVio() throws Exception {
        Cliente c = paciente("Paciente Auditado", "+5493584910001", null, null);
        String admin = admin();

        mvc.perform(get("/api/clientes/{id}", c.getId()).header("Authorization", admin))
                .andExpect(status().isOk());

        mvc.perform(get("/api/clientes/{id}/accesos", c.getId()).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accion").value("VER_FICHA"))
                .andExpect(jsonPath("$[0].usuarioEmail").value("admin@jade.com"))
                .andExpect(jsonPath("$[0].fecha").value(notNullValue()));
    }

    @Test
    void editarNotasYCoberturaTambienQuedaRegistrado() throws Exception {
        Cliente c = paciente("Paciente Editado", "+5493584910002", null, null);
        String admin = admin();

        mvc.perform(patch("/api/clientes/{id}/notas", c.getId()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notas\":\"x\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/clientes/{id}/cobertura", c.getId()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coberturaId\":null,\"numeroAfiliado\":\"\",\"plan\":\"\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/clientes/{id}/accesos", c.getId()).header("Authorization", admin))
                .andExpect(jsonPath("$[*].accion", hasItem("EDITAR_NOTAS")))
                .andExpect(jsonPath("$[*].accion", hasItem("EDITAR_COBERTURA")));
    }

    @Test
    void soloElAdminPuedeVerAccesosExportarYAnonimizar() throws Exception {
        Cliente c = paciente("Paciente Protegido", "+5493584910003", null, null);
        String lucas = login("lucas@jade.com", "barbero123");

        mvc.perform(get("/api/clientes/{id}/accesos", c.getId()).header("Authorization", lucas))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/clientes/{id}/exportar", c.getId()).header("Authorization", lucas))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/clientes/{id}/anonimizar", c.getId()).header("Authorization", lucas))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/clientes/{id}/exportar", c.getId())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/clientes/{id}/anonimizar", c.getId())).andExpect(status().isUnauthorized());

        assertThat(clientes.findById(c.getId()).orElseThrow().isAnonimizado()).isFalse();
    }

    // ---------- Exportar ----------

    @Test
    void exportarDevuelveLosDatosConLasNotasDeConsultaYQuedaRegistrado() throws Exception {
        Cliente c = paciente("Paciente Exportado", "+5493584910004", "39999004", "nota de la ficha");
        turno(c, EstadoAtencion.COMPLETADA, "nota de la consulta");
        String admin = admin();

        mvc.perform(get("/api/clientes/{id}/exportar", c.getId()).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(jsonPath("$.paciente.nombre").value("Paciente Exportado"))
                .andExpect(jsonPath("$.paciente.dni").value("39999004"))
                .andExpect(jsonPath("$.paciente.notas").value("nota de la ficha"))
                .andExpect(jsonPath("$.turnos", hasSize(1)))
                .andExpect(jsonPath("$.turnos[0].notasDeConsulta").value("nota de la consulta"));

        mvc.perform(get("/api/clientes/{id}/accesos", c.getId()).header("Authorization", admin))
                .andExpect(jsonPath("$[*].accion", hasItem("EXPORTAR")));
    }

    // ---------- Anonimizar ----------

    @Test
    void anonimizarBorraLosDatosPersonalesYConservaLosTurnos() throws Exception {
        Cliente c = paciente("Paciente A Borrar", "+5493584910005", "39999005", "alergia a la penicilina");
        Atencion completada = turno(c, EstadoAtencion.COMPLETADA, "diagnóstico y tratamiento");
        String admin = admin();

        mvc.perform(post("/api/clientes/{id}/anonimizar", c.getId()).header("Authorization", admin))
                .andExpect(status().isNoContent());

        Cliente despues = clientes.findById(c.getId()).orElseThrow();
        assertThat(despues.getNombre()).isEqualTo("Paciente anonimizado");
        assertThat(despues.getTelefono()).startsWith("+999");
        assertThat(despues.getDni()).isNull();
        assertThat(despues.getNotas()).isNull();
        assertThat(despues.getFechaNacimiento()).isNull();
        assertThat(despues.getNumeroAfiliado()).isNull();
        assertThat(despues.getAnonimizadoEn()).isNotNull();

        // El turno y el cobro siguen (caja y contabilidad), pero sin la nota de consulta
        Atencion turno = atenciones.findById(completada.getId()).orElseThrow();
        assertThat(turno.getPrecioFinal()).isEqualTo(5000);
        assertThat(turno.getEstado()).isEqualTo(EstadoAtencion.COMPLETADA);
        assertThat(turno.getNotas()).isNull();

        mvc.perform(get("/api/clientes/{id}", c.getId()).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Paciente anonimizado"))
                .andExpect(jsonPath("$.anonimizadoEn").value(notNullValue()))
                .andExpect(jsonPath("$.historial", hasSize(1)));

        mvc.perform(get("/api/clientes/{id}/accesos", c.getId()).header("Authorization", admin))
                .andExpect(jsonPath("$[*].accion", hasItem("ANONIMIZAR")));
    }

    @Test
    void anonimizarDosVecesDaConflicto() throws Exception {
        Cliente c = paciente("Paciente Dos Veces", "+5493584910006", null, null);
        String admin = admin();

        mvc.perform(post("/api/clientes/{id}/anonimizar", c.getId()).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/clientes/{id}/anonimizar", c.getId()).header("Authorization", admin))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    void noSeAnonimizaUnPacienteConTurnosReservados() throws Exception {
        Cliente c = paciente("Paciente Con Turno", "+5493584910007", null, "una nota");
        turno(c, EstadoAtencion.RESERVADA, null);

        mvc.perform(post("/api/clientes/{id}/anonimizar", c.getId()).header("Authorization", admin()))
                .andExpect(status().isConflict());

        Cliente sigue = clientes.findById(c.getId()).orElseThrow();
        assertThat(sigue.isAnonimizado()).isFalse();
        assertThat(sigue.getNombre()).isEqualTo("Paciente Con Turno");
        assertThat(sigue.getNotas()).isEqualTo("una nota");
    }

    @Test
    void dosPacientesAnonimizadosDelMismoNegocioNoChocanEntreSi() throws Exception {
        // Sin DNI, el teléfono es único por negocio: cada anonimizado tiene que quedar con uno distinto
        Cliente a = paciente("Paciente Uno", "+5493584910008", null, null);
        Cliente b = paciente("Paciente Dos", "+5493584910009", null, null);
        String admin = admin();

        mvc.perform(post("/api/clientes/{id}/anonimizar", a.getId()).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/clientes/{id}/anonimizar", b.getId()).header("Authorization", admin))
                .andExpect(status().isNoContent());

        assertThat(clientes.findById(a.getId()).orElseThrow().getTelefono())
                .isNotEqualTo(clientes.findById(b.getId()).orElseThrow().getTelefono());
    }

    @Test
    void anonimizarUnPacienteQueNoExisteDa404() throws Exception {
        mvc.perform(post("/api/clientes/{id}/anonimizar", UUID.randomUUID()).header("Authorization", admin()))
                .andExpect(status().isNotFound());
    }
}

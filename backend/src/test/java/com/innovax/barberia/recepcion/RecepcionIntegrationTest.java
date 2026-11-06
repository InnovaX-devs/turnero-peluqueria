package com.innovax.barberia.recepcion;

import com.innovax.barberia.auth.Rol;
import com.innovax.barberia.auth.Usuario;
import com.innovax.barberia.auth.UsuarioRepository;
import com.innovax.barberia.negocio.NegocioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rol RECEPCION (issue #70): qué puede hacer recepción, qué no, y que admin y profesional sigan igual.
 * Usa el seed de dev (admin@jade.com, lucas@jade.com). Es @Transactional: cada test se revierte.
 */
@SpringBootTest
@Transactional
class RecepcionIntegrationTest {

    private static final String AUTH = "Authorization";
    private static final String NEGOCIO = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String LUCAS = "cccccccc-0000-0000-0000-000000000001";
    private static final String ANDRES = "cccccccc-0000-0000-0000-000000000002";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private NegocioRepository negocios;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // ---------- Helpers ----------

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

    private String lucas() throws Exception {
        return login("lucas@jade.com", "barbero123");
    }

    /** Crea un usuario de recepción con contraseña conocida y devuelve su token. */
    private String recepcion() throws Exception {
        Usuario u = new Usuario();
        u.setNegocio(negocios.findById(UUID.fromString(NEGOCIO)).orElseThrow());
        u.setEmail("recepcion@jade.com");
        u.setPasswordHash(passwordEncoder.encode("recepcion123"));
        u.setRol(Rol.RECEPCION);
        u.setActivo(true);
        usuarios.saveAndFlush(u);
        return login("recepcion@jade.com", "recepcion123");
    }

    private static String hoy() {
        return LocalDate.now().toString();
    }

    // ---------- Login ----------

    @Test
    void recepcionIniciaSesionSinProfesionalVinculado() throws Exception {
        String token = recepcion();
        mvc.perform(get("/api/auth/yo").header(AUTH, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("RECEPCION"))
                .andExpect(jsonPath("$.profesionalId").value(nullValue()));
    }

    // ---------- Lo que recepción puede hacer ----------

    @Test
    void recepcionVeLaAgendaDeTodosYPuedeFiltrarPorProfesional() throws Exception {
        String token = recepcion();
        mvc.perform(get("/api/agenda").header(AUTH, token).param("desde", hoy()).param("hasta", hoy()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profesionales", hasSize(3)));
        mvc.perform(get("/api/agenda").header(AUTH, token)
                        .param("desde", hoy()).param("hasta", hoy()).param("profesionalId", ANDRES))
                .andExpect(status().isOk());
    }

    @Test
    void recepcionPuedeOperarSobreCualquierProfesional() throws Exception {
        String token = recepcion();
        mvc.perform(get("/api/test-permisos/profesional/{id}", LUCAS).header(AUTH, token)).andExpect(status().isOk());
        mvc.perform(get("/api/test-permisos/profesional/{id}", ANDRES).header(AUTH, token)).andExpect(status().isOk());
    }

    @Test
    void recepcionVeTodosLosProfesionalesAlRegistrarUnaAtencion() throws Exception {
        mvc.perform(get("/api/atenciones/opciones").header(AUTH, recepcion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profesionales", hasSize(3)));
    }

    @Test
    void recepcionBloqueaElHorarioDeUnProfesional() throws Exception {
        String json = """
                {"profesionalIds":["%s"],"fecha":"%s","motivo":"Turno médico"}
                """.formatted(ANDRES, LocalDate.now().plusDays(20));
        mvc.perform(post("/api/bloqueos").header(AUTH, recepcion())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void recepcionTieneQueElegirElProfesionalAlBloquear() throws Exception {
        String json = """
                {"fecha":"%s"}
                """.formatted(LocalDate.now().plusDays(20));
        mvc.perform(post("/api/bloqueos").header(AUTH, recepcion())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recepcionVeClientesYMensajes() throws Exception {
        String token = recepcion();
        mvc.perform(get("/api/clientes/listado").header(AUTH, token)).andExpect(status().isOk());
        mvc.perform(get("/api/notificaciones").header(AUTH, token)).andExpect(status().isOk());
    }

    // ---------- Lo que recepción NO puede hacer ----------

    @Test
    void recepcionNoEntraALoQueEsSoloDelAdmin() throws Exception {
        String token = recepcion();
        mvc.perform(get("/api/negocio").header(AUTH, token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/profesionales").header(AUTH, token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/servicios").header(AUTH, token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/caja").header(AUTH, token).param("desde", hoy()).param("hasta", hoy()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/recepcion/usuarios").header(AUTH, token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/test-permisos/solo-admin").header(AUTH, token)).andExpect(status().isForbidden());
    }

    @Test
    void recepcionNoVeReportes() throws Exception {
        String token = recepcion();
        mvc.perform(get("/api/reportes").header(AUTH, token).param("desde", hoy()).param("hasta", hoy()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/reportes/clientes-inactivos").header(AUTH, token))
                .andExpect(status().isForbidden());
    }

    // ---------- Admin y profesional siguen igual ----------

    @Test
    void profesionalSigueViendoSoloLoSuyo() throws Exception {
        String token = lucas();
        mvc.perform(get("/api/test-permisos/profesional/{id}", LUCAS).header(AUTH, token)).andExpect(status().isOk());
        mvc.perform(get("/api/test-permisos/profesional/{id}", ANDRES).header(AUTH, token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/atenciones/opciones").header(AUTH, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profesionales", hasSize(1)));
        mvc.perform(get("/api/reportes").header(AUTH, token).param("desde", hoy()).param("hasta", hoy()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/recepcion/usuarios").header(AUTH, token)).andExpect(status().isForbidden());
    }

    @Test
    void adminSigueViendoTodo() throws Exception {
        String token = admin();
        mvc.perform(get("/api/test-permisos/profesional/{id}", ANDRES).header(AUTH, token)).andExpect(status().isOk());
        mvc.perform(get("/api/atenciones/opciones").header(AUTH, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profesionales", hasSize(3)));
        mvc.perform(get("/api/reportes").header(AUTH, token).param("desde", hoy()).param("hasta", hoy()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/negocio").header(AUTH, token)).andExpect(status().isOk());
    }

    // ---------- ABM de usuarios de recepción (admin) ----------

    @Test
    void adminCreaUnUsuarioDeRecepcion() throws Exception {
        String body = mvc.perform(post("/api/recepcion/usuarios").header(AUTH, admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"Secre@Jade.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("secre@jade.com"))
                .andExpect(jsonPath("$.activo").value(true))
                .andReturn().getResponse().getContentAsString();

        Usuario creado = usuarios.findById(UUID.fromString(JsonPath.read(body, "$.id"))).orElseThrow();
        assertThat(creado.getRol()).isEqualTo(Rol.RECEPCION);

        mvc.perform(get("/api/recepcion/usuarios").header(AUTH, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("secre@jade.com"));
    }

    @Test
    void noSePuedeUsarUnEmailQueYaExiste() throws Exception {
        mvc.perform(post("/api/recepcion/usuarios").header(AUTH, admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"lucas@jade.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void adminDesactivaUnUsuarioDeRecepcionYYaNoPuedeEntrar() throws Exception {
        recepcion();
        Usuario u = usuarios.findByEmailIgnoreCase("recepcion@jade.com").orElseThrow();

        mvc.perform(patch("/api/recepcion/usuarios/{id}/activo", u.getId()).header(AUTH, admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"recepcion@jade.com\",\"password\":\"recepcion123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elAbmDeRecepcionNoTocaOtrosUsuarios() throws Exception {
        // El usuario de Lucas (profesional) no se puede desactivar desde acá: responde 404.
        Usuario lucas = usuarios.findByEmailIgnoreCase("lucas@jade.com").orElseThrow();
        mvc.perform(patch("/api/recepcion/usuarios/{id}/activo", lucas.getId()).header(AUTH, admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isNotFound());
    }
}

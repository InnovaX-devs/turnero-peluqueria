package com.innovax.barberia.servicio;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.OrigenAtencion;
import com.innovax.barberia.barbero.BarberoRepository;
import com.innovax.barberia.sucursal.SucursalRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** ABM de servicios (solo admin): /api/servicios. @Transactional: no deja cambios en la base. */
@SpringBootTest
@Transactional
class ServicioAdminIntegrationTest {

    private static final String AUTH = "Authorization";
    private static final UUID SUCURSAL = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private static final UUID LUCAS = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID CORTE = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
    private static final UUID BARBA = UUID.fromString("dddddddd-0000-0000-0000-000000000002");
    private static final UUID CORTE_Y_BARBA = UUID.fromString("dddddddd-0000-0000-0000-000000000003");

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private ServicioRepository servicios;
    @Autowired
    private AtencionRepository atenciones;
    @Autowired
    private BarberoRepository barberos;
    @Autowired
    private SucursalRepository sucursales;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private String admin() throws Exception {
        return login("admin@jade.com", "admin123");
    }

    private static String json(String nombre, Object duracion, Object precio) {
        return "{\"nombre\":\"%s\",\"duracionMin\":%s,\"precio\":%s}".formatted(nombre, duracion, precio);
    }

    @Test
    void soloElAdminPuedeUsarElAbm() throws Exception {
        String lucas = login("lucas@jade.com", "barbero123");
        mvc.perform(get("/api/servicios").header(AUTH, lucas)).andExpect(status().isForbidden());
        mvc.perform(get("/api/servicios")).andExpect(status().isUnauthorized());
    }

    @Test
    void listaTodosConInactivosOSoloActivos() throws Exception {
        String token = admin();
        Servicio barba = servicios.findById(BARBA).orElseThrow();
        barba.setActivo(false);
        servicios.saveAndFlush(barba);

        mvc.perform(get("/api/servicios").header(AUTH, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/api/servicios").param("incluirInactivos", "false").header(AUTH, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void creaUnServicioAlFinalDeLaLista() throws Exception {
        mvc.perform(post("/api/servicios").header(AUTH, admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(json("  Perfilado ", 20, 800000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Perfilado"))
                .andExpect(jsonPath("$.duracionMin").value(20))
                .andExpect(jsonPath("$.precio").value(800000))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.orden").value(4));
    }

    @Test
    void rechazaDatosInvalidos() throws Exception {
        String token = admin();
        mvc.perform(post("/api/servicios").header(AUTH, token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(" ", 0, -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.duracionMin").exists())
                .andExpect(jsonPath("$.errores.precio").exists());
    }

    @Test
    void rechazaNombreRepetidoEntreActivos() throws Exception {
        mvc.perform(post("/api/servicios").header(AUTH, admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(json("corte", 30, 100)))
                .andExpect(status().isConflict());
    }

    @Test
    void editaNombreDuracionYPrecio() throws Exception {
        mvc.perform(put("/api/servicios/" + BARBA).header(AUTH, admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(json("Barba completa", 25, 650000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Barba completa"))
                .andExpect(jsonPath("$.duracionMin").value(25))
                .andExpect(jsonPath("$.precio").value(650000));
    }

    @Test
    void bajaLogicaLoSacaDeLaPaginaPublicaYSePuedeReactivar() throws Exception {
        String token = admin();
        mvc.perform(patch("/api/servicios/" + BARBA + "/activo").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));

        assertThat(servicios.findById(BARBA)).isPresent();   // no se borra
        mvc.perform(get("/api/public/barberias/jade/servicios"))
                .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(patch("/api/servicios/" + BARBA + "/activo").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.orden").value(4));   // vuelve al final
    }

    @Test
    void reordenarCambiaElOrdenDeLaPaginaPublica() throws Exception {
        String body = "{\"ids\":[\"%s\",\"%s\",\"%s\"]}".formatted(CORTE_Y_BARBA, BARBA, CORTE);
        mvc.perform(put("/api/servicios/orden").header(AUTH, admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Corte y Barba"));

        mvc.perform(get("/api/public/barberias/jade/servicios"))
                .andExpect(jsonPath("$[0].nombre").value("Corte y Barba"))
                .andExpect(jsonPath("$[1].nombre").value("Barba"))
                .andExpect(jsonPath("$[2].nombre").value("Corte"));
    }

    @Test
    void reordenarExigeTodosLosActivosSinRepetir() throws Exception {
        String token = admin();
        mvc.perform(put("/api/servicios/orden").header(AUTH, token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"%s\",\"%s\"]}".formatted(CORTE, BARBA)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.ids").exists());
        mvc.perform(put("/api/servicios/orden").header(AUTH, token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"%s\",\"%s\",\"%s\"]}".formatted(CORTE, CORTE, BARBA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarElPrecioNoModificaAtencionesYaRegistradas() throws Exception {
        Servicio corte = servicios.findById(CORTE).orElseThrow();
        Atencion a = new Atencion();
        a.setBarberia(corte.getBarberia());
        a.setSucursal(sucursales.findById(SUCURSAL).orElseThrow());
        a.setBarbero(barberos.findById(LUCAS).orElseThrow());
        a.setServicio(corte);
        a.setInicio(Instant.parse("2026-01-10T13:00:00Z"));
        a.setFin(Instant.parse("2026-01-10T13:30:00Z"));
        a.setEstado(EstadoAtencion.COMPLETADA);
        a.setOrigen(OrigenAtencion.MANUAL);
        a.setPrecioFinal(corte.getPrecio());
        UUID atencionId = atenciones.saveAndFlush(a).getId();
        long precioOriginal = corte.getPrecio();

        mvc.perform(put("/api/servicios/" + CORTE).header(AUTH, admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(json("Corte", 30, precioOriginal * 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(precioOriginal * 2));

        assertThat(atenciones.findById(atencionId).orElseThrow().getPrecioFinal()).isEqualTo(precioOriginal);
        mvc.perform(get("/api/public/barberias/jade/servicios"))
                .andExpect(jsonPath("$[0].precio").value(precioOriginal * 2));
    }

    @Test
    void noSePuedeTocarUnServicioInexistente() throws Exception {
        mvc.perform(put("/api/servicios/" + UUID.randomUUID()).header(AUTH, admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json("X", 10, 10)))
                .andExpect(status().isNotFound());
    }
}
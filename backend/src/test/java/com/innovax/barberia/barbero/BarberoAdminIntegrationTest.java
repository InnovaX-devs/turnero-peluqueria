package com.innovax.barberia.barbero;

import com.innovax.barberia.auth.Rol;
import com.innovax.barberia.auth.TokenRecuperacionRepository;
import com.innovax.barberia.auth.Usuario;
import com.innovax.barberia.auth.UsuarioRepository;
import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ABM de barberos (solo admin): /api/barberos.
 * Usa los datos del seed de dev (necesita la base levantada).
 * Es @Transactional: cada test se revierte al terminar y no deja cambios en la base.
 * FotoStorageService se mockea: los tests no llaman a Supabase.
 */
@SpringBootTest
@Transactional
class BarberoAdminIntegrationTest {

    private static final String AUTH = "Authorization";
    private static final String SUCURSAL = "bbbbbbbb-0000-0000-0000-000000000001";
    private static final String CORTE = "dddddddd-0000-0000-0000-000000000001";
    private static final String BARBA = "dddddddd-0000-0000-0000-000000000002";
    private static final String LUCAS = "cccccccc-0000-0000-0000-000000000001";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private BarberiaRepository barberias;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TokenRecuperacionRepository tokens;
    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private FotoStorageService fotos;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // ---------- Helpers ----------

    private ResultActions intentarLogin(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private String login(String email, String password) throws Exception {
        String body = intentarLogin(email, password)
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

    /** Admin de una segunda barbería, para probar el aislamiento entre barberías. */
    private String adminDeOtraBarberia() throws Exception {
        Barberia otra = new Barberia();
        otra.setNombre("Otra");
        otra.setSlug("otra-barberia");
        barberias.saveAndFlush(otra);

        Usuario u = new Usuario();
        u.setBarberia(otra);
        u.setEmail("admin@otra.com");
        u.setPasswordHash(passwordEncoder.encode("admin123"));
        u.setRol(Rol.ADMIN);
        u.setActivo(true);
        usuarios.saveAndFlush(u);
        return login("admin@otra.com", "admin123");
    }

    private static String lista(String... ids) {
        return Arrays.stream(ids).map(id -> "\"" + id + "\"").collect(Collectors.joining(",", "[", "]"));
    }

    private static String datos(String nombre, String apellido, String sucursalIds, String servicioIds) {
        return """
                {"nombre":"%s","apellido":"%s","sucursalIds":%s,"servicioIds":%s}
                """.formatted(nombre, apellido, sucursalIds, servicioIds);
    }

    private static String tramo(String sucursal, int dia, String inicio, String fin) {
        return """
                {"sucursalId":"%s","diaSemana":%d,"horaInicio":"%s","horaFin":"%s"}
                """.formatted(sucursal, dia, inicio, fin);
    }

    private static String horarios(String... tramos) {
        return "{\"horarios\":[" + String.join(",", tramos) + "]}";
    }

    /** Lunes a viernes, 10-13 y 16-21: 10 tramos. */
    private static String semanaLaboral(String sucursal) {
        return IntStream.rangeClosed(1, 5).boxed()
                .flatMap(d -> Stream.of(tramo(sucursal, d, "10:00", "13:00"), tramo(sucursal, d, "16:00", "21:00")))
                .collect(Collectors.joining(",", "{\"horarios\":[", "]}"));
    }

    private String crearBarbero(String token, String nombre) throws Exception {
        String body = mvc.perform(post("/api/barberos").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datos(nombre, "Test", lista(SUCURSAL), lista(CORTE))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String crearBarberoConUsuario(String token, String nombre, String email, String password) throws Exception {
        String body = mvc.perform(post("/api/barberos").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","apellido":"Test","sucursalIds":%s,"servicioIds":%s,
                                 "usuario":{"email":"%s"}}
                                """.formatted(nombre, lista(SUCURSAL), lista(CORTE), email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        fijarPassword(email, password);   // simula que el barbero ya eligió su contraseña desde la invitación
        return JsonPath.read(body, "$.id");
    }

    private void fijarPassword(String email, String password) {
        Usuario u = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        u.setPasswordHash(passwordEncoder.encode(password));
        usuarios.saveAndFlush(u);
    }

    private boolean tieneInvitacion(String email) {
        UUID usuarioId = usuarios.findByEmailIgnoreCase(email).orElseThrow().getId();
        return tokens.existsByUsuarioIdAndCreatedAtAfter(usuarioId, Instant.EPOCH);
    }

    private void guardarHorarios(String token, String id, String json) throws Exception {
        mvc.perform(put("/api/barberos/" + id + "/horarios").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }

    // ---------- Permisos ----------

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/barberos")).andExpect(status().isUnauthorized());
    }

    @Test
    void barberoNoPuedeUsarElAbm() throws Exception {
        String token = lucas();
        mvc.perform(get("/api/barberos").header(AUTH, token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/barberos/opciones").header(AUTH, token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/barberos").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datos("Pedro", "Perez", lista(SUCURSAL), lista(CORTE))))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/barberos/" + LUCAS + "/activo").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isForbidden());
    }

    // ---------- Lectura ----------

    @Test
    void listaLosBarberosDelSeedConSusAsignaciones() throws Exception {
        mvc.perform(get("/api/barberos").header(AUTH, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.id == '%s')].usuarioEmail".formatted(LUCAS)).value("lucas@jade.com"));
    }

    @Test
    void opcionesTraeSucursalesYServiciosActivos() throws Exception {
        mvc.perform(get("/api/barberos/opciones").header(AUTH, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucursales", hasSize(1)))
                .andExpect(jsonPath("$.sucursales[0].id").value(SUCURSAL))
                .andExpect(jsonPath("$.servicios", hasSize(3)))
                .andExpect(jsonPath("$.servicios[0].nombre").value("Corte"))
                .andExpect(jsonPath("$.servicios[0].duracionMin").value(30));
    }

    // ---------- Alta ----------

    @Test
    void altaSinUsuario() throws Exception {
        mvc.perform(post("/api/barberos").header(AUTH, admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datos("  Pedro ", " Perez  ", lista(SUCURSAL), lista(CORTE, BARBA))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nombre").value("Pedro"))      // se recorta
                .andExpect(jsonPath("$.apellido").value("Perez"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.fotoUrl").value(nullValue()))
                .andExpect(jsonPath("$.usuarioEmail").value(nullValue()))
                .andExpect(jsonPath("$.sucursalIds", hasSize(1)))
                .andExpect(jsonPath("$.servicioIds", hasSize(2)));
    }

    @Test
    void altaConUsuarioPermiteEntrarConRolBarbero() throws Exception {
        String id = crearBarberoConUsuario(admin(), "Pedro", "Pedro@Jade.com", "clave12345");

        intentarLogin("pedro@jade.com", "clave12345")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("BARBERO"))
                .andExpect(jsonPath("$.usuario.barberoId").value(id));
    }

    @Test
    void altaConEmailYaUsadoDevuelve409() throws Exception {
        mvc.perform(post("/api/barberos").header(AUTH, admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Pedro","apellido":"Test","sucursalIds":%s,"servicioIds":%s,
                                 "usuario":{"email":"LUCAS@jade.com"}}
                                """.formatted(lista(SUCURSAL), lista(CORTE))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.email").exists());
    }

    @Test
    void altaConDatosInvalidosDevuelve400() throws Exception {
        String token = admin();
        // nombre vacío
        mvc.perform(post("/api/barberos").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datos("", "Perez", lista(SUCURSAL), lista(CORTE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
        // faltan las listas
        mvc.perform(post("/api/barberos").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Pedro\",\"apellido\":\"Perez\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.sucursalIds").exists())
                .andExpect(jsonPath("$.errores.servicioIds").exists());
        // email inválido
        mvc.perform(post("/api/barberos").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Pedro","apellido":"Test","sucursalIds":%s,"servicioIds":%s,
                                 "usuario":{"email":"esto-no-es-un-email"}}
                                """.formatted(lista(SUCURSAL), lista(CORTE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").exists());
    }

    @Test
    void altaConSucursalOServicioInexistenteDevuelve400() throws Exception {
        String token = admin();
        String inexistente = UUID.randomUUID().toString();
        mvc.perform(post("/api/barberos").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datos("Pedro", "Perez", lista(inexistente), lista(CORTE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.sucursalIds").exists());
        mvc.perform(post("/api/barberos").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datos("Pedro", "Perez", lista(SUCURSAL), lista(inexistente))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.servicioIds").exists());
    }

    // ---------- Edición ----------

    @Test
    void editaDatosYServicios() throws Exception {
        String token = admin();
        String id = crearBarbero(token, "Pedro");

        mvc.perform(put("/api/barberos/" + id).header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datos(" Pablo ", "Gomez", lista(SUCURSAL), lista(BARBA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pablo"))
                .andExpect(jsonPath("$.apellido").value("Gomez"))
                .andExpect(jsonPath("$.servicioIds", hasSize(1)))
                .andExpect(jsonPath("$.servicioIds[0]").value(BARBA));

        mvc.perform(get("/api/barberos/" + id).header(AUTH, token))
                .andExpect(jsonPath("$.nombre").value("Pablo"));
    }

    @Test
    void sacarleUnaSucursalBorraSusHorariosEnEsaSucursal() throws Exception {
        String token = admin();
        String id = crearBarbero(token, "Pedro");
        guardarHorarios(token, id, semanaLaboral(SUCURSAL));

        mvc.perform(put("/api/barberos/" + id).header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datos("Pedro", "Test", lista(), lista(CORTE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucursalIds", hasSize(0)));

        mvc.perform(get("/api/barberos/" + id + "/horarios").header(AUTH, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void barberoInexistenteDevuelve404() throws Exception {
        String token = admin();
        String inexistente = UUID.randomUUID().toString();
        mvc.perform(get("/api/barberos/" + inexistente).header(AUTH, token)).andExpect(status().isNotFound());
        mvc.perform(put("/api/barberos/" + inexistente).header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datos("Pedro", "Perez", lista(SUCURSAL), lista(CORTE))))
                .andExpect(status().isNotFound());
    }

    // ---------- Baja lógica ----------

    @Test
    void desactivarNoBorraAlBarberoYPuedeReactivarse() throws Exception {
        String token = admin();
        String id = crearBarbero(token, "Pedro");

        mvc.perform(patch("/api/barberos/" + id + "/activo").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        // sigue existiendo: aparece en el listado completo y no en el de activos
        mvc.perform(get("/api/barberos").header(AUTH, token))
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(id), hasSize(1)));
        mvc.perform(get("/api/barberos").param("incluirInactivos", "false").header(AUTH, token))
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(id), hasSize(0)));

        mvc.perform(patch("/api/barberos/" + id + "/activo").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void desactivarAlBarberoTambienLeQuitaElAccesoYReactivarloSeLoDevuelve() throws Exception {
        String token = admin();
        String id = crearBarberoConUsuario(token, "Pedro", "pedro@jade.com", "clave12345");
        intentarLogin("pedro@jade.com", "clave12345").andExpect(status().isOk());

        mvc.perform(patch("/api/barberos/" + id + "/activo").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk());
        intentarLogin("pedro@jade.com", "clave12345").andExpect(status().isUnauthorized());

        mvc.perform(patch("/api/barberos/" + id + "/activo").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}"))
                .andExpect(status().isOk());
        intentarLogin("pedro@jade.com", "clave12345").andExpect(status().isOk());
    }

    @Test
    void cambiarActivoSinElCampoDevuelve400() throws Exception {
        String token = admin();
        String id = crearBarbero(token, "Pedro");
        mvc.perform(patch("/api/barberos/" + id + "/activo").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.activo").exists());
    }

    // ---------- Usuario de acceso ----------

    @Test
    void creaElUsuarioDespuesYNoPermiteCrearloDosVeces() throws Exception {
        String token = admin();
        String id = crearBarbero(token, "Pedro");

        mvc.perform(post("/api/barberos/" + id + "/usuario").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"pedro@jade.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioEmail").value("pedro@jade.com"));
        assertThat(tieneInvitacion("pedro@jade.com")).isTrue();

        mvc.perform(post("/api/barberos/" + id + "/usuario").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"otro@jade.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.email").exists());
    }

    @Test
    void crearUsuarioConEmailYaUsadoDevuelve409() throws Exception {
        String token = admin();
        String id = crearBarbero(token, "Pedro");
        mvc.perform(post("/api/barberos/" + id + "/usuario").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@jade.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.email").exists());
    }

    @Test
    void reenviarInvitacionEsperaUnMinutoYDespuesGeneraLinkNuevo() throws Exception {
        String token = admin();
        String id = crearBarberoConUsuario(token, "Pedro", "pedro@jade.com", "clave12345");

        // Recién invitado: reenviar enseguida se rechaza
        mvc.perform(post("/api/barberos/" + id + "/invitacion").header(AUTH, token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.usuario").exists());

        // Pasado el minuto, sí se puede
        jdbc.update("update token_recuperacion set created_at = now() - interval '5 minutes'");
        mvc.perform(post("/api/barberos/" + id + "/invitacion").header(AUTH, token))
                .andExpect(status().isNoContent());
    }

    @Test
    void noSePuedeReenviarSiNoHayUsuarioOSiEstaInactivo() throws Exception {
        String token = admin();
        String sinUsuario = crearBarbero(token, "Pedro");
        mvc.perform(post("/api/barberos/" + sinUsuario + "/invitacion").header(AUTH, token))
                .andExpect(status().isBadRequest());

        String conUsuario = crearBarberoConUsuario(token, "Juan", "juan@jade.com", "clave12345");
        mvc.perform(patch("/api/barberos/" + conUsuario + "/activo").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/barberos/" + conUsuario + "/invitacion").header(AUTH, token))
                .andExpect(status().isBadRequest());
    }

    // ---------- Foto ----------

    @Test
    void subeYQuitaLaFoto() throws Exception {
        when(fotos.subir(any(), any(), any())).thenReturn("https://cdn.test/foto1.jpg");
        String token = admin();
        String id = crearBarbero(token, "Pedro");

        mvc.perform(multipart("/api/barberos/" + id + "/foto")
                        .file(new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .header(AUTH, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl").value("https://cdn.test/foto1.jpg"));

        mvc.perform(delete("/api/barberos/" + id + "/foto").header(AUTH, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl").value(nullValue()));
        verify(fotos).borrarSilencioso("https://cdn.test/foto1.jpg");
    }

    @Test
    void subirFotoSinArchivoDevuelve400() throws Exception {
        String token = admin();
        String id = crearBarbero(token, "Pedro");
        mvc.perform(multipart("/api/barberos/" + id + "/foto").header(AUTH, token))
                .andExpect(status().isBadRequest());
    }

    // ---------- Horarios ----------

    @Test
    void guardaLaSemanaLaboralConDosTramosPorDia() throws Exception {
        String token = admin();
        String id = crearBarbero(token, "Pedro");

        mvc.perform(put("/api/barberos/" + id + "/horarios").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON).content(semanaLaboral(SUCURSAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].diaSemana").value(1))
                .andExpect(jsonPath("$[0].horaInicio", startsWith("10:00")))
                .andExpect(jsonPath("$[1].horaInicio", startsWith("16:00")))
                .andExpect(jsonPath("$[9].diaSemana").value(5));

        mvc.perform(get("/api/barberos/" + id + "/horarios").header(AUTH, token))
                .andExpect(jsonPath("$", hasSize(10)));
    }

    @Test
    void guardarReemplazaTodoElHorarioAnteriorYUnaListaVaciaLoBorra() throws Exception {
        String token = admin();
        String id = crearBarbero(token, "Pedro");
        guardarHorarios(token, id, semanaLaboral(SUCURSAL));

        guardarHorarios(token, id, horarios(tramo(SUCURSAL, 6, "09:00", "14:00")));
        mvc.perform(get("/api/barberos/" + id + "/horarios").header(AUTH, token))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].diaSemana").value(6));

        guardarHorarios(token, id, horarios());
        mvc.perform(get("/api/barberos/" + id + "/horarios").header(AUTH, token))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void tramosConsecutivosSinSuperponerseSeAceptan() throws Exception {
        String token = admin();
        String id = crearBarbero(token, "Pedro");
        mvc.perform(put("/api/barberos/" + id + "/horarios").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(horarios(tramo(SUCURSAL, 1, "10:00", "13:00"), tramo(SUCURSAL, 1, "13:00", "16:00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void tramosSuperpuestosEnElMismoDiaDevuelven400() throws Exception {
        String token = admin();
        String id = crearBarbero(token, "Pedro");
        mvc.perform(put("/api/barberos/" + id + "/horarios").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(horarios(tramo(SUCURSAL, 1, "10:00", "13:00"), tramo(SUCURSAL, 1, "12:00", "15:00"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.horarios").exists());
    }

    @Test
    void horaDeFinAnteriorOIgualALaDeInicioDevuelve400() throws Exception {
        String token = admin();
        String id = crearBarbero(token, "Pedro");
        for (String fin : new String[]{"09:00", "10:00"}) {
            mvc.perform(put("/api/barberos/" + id + "/horarios").header(AUTH, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(horarios(tramo(SUCURSAL, 1, "10:00", fin))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.horarios").exists());
        }
    }

    @Test
    void horarioEnUnaSucursalDondeNoTrabajaDevuelve400() throws Exception {
        String token = admin();
        // barbero sin sucursales asignadas
        String body = mvc.perform(post("/api/barberos").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datos("Pedro", "Test", lista(), lista(CORTE))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(put("/api/barberos/" + id + "/horarios").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(horarios(tramo(SUCURSAL, 1, "10:00", "13:00"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.horarios").exists());
    }

    @Test
    void diaOHorasInvalidosDevuelven400() throws Exception {
        String token = admin();
        String id = crearBarbero(token, "Pedro");
        mvc.perform(put("/api/barberos/" + id + "/horarios").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(horarios(tramo(SUCURSAL, 8, "10:00", "13:00"))))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/barberos/" + id + "/horarios").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"horarios\":[{\"sucursalId\":\"%s\",\"diaSemana\":1,\"horaInicio\":\"10:00\"}]}"
                                .formatted(SUCURSAL)))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/barberos/" + id + "/horarios").header(AUTH, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.horarios").exists());
    }

    // ---------- Aislamiento entre barberías ----------

    @Test
    void otraBarberiaNoVeNiTocaLosBarberosDeJade() throws Exception {
        String id = crearBarbero(admin(), "Pedro");
        String otro = adminDeOtraBarberia();

        mvc.perform(get("/api/barberos").header(AUTH, otro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/barberos/" + id).header(AUTH, otro)).andExpect(status().isNotFound());
        mvc.perform(get("/api/barberos/" + id + "/horarios").header(AUTH, otro)).andExpect(status().isNotFound());
        mvc.perform(put("/api/barberos/" + id).header(AUTH, otro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datos("Hack", "Hack", lista(), lista())))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/barberos/" + id + "/activo").header(AUTH, otro)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/barberos/" + id + "/horarios").header(AUTH, otro)
                        .contentType(MediaType.APPLICATION_JSON).content(horarios()))
                .andExpect(status().isNotFound());
    }

    @Test
    void noPuedeAsignarSucursalesNiServiciosDeOtraBarberia() throws Exception {
        mvc.perform(post("/api/barberos").header(AUTH, adminDeOtraBarberia())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datos("Pedro", "Test", lista(SUCURSAL), lista(CORTE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.sucursalIds").exists());
    }
}
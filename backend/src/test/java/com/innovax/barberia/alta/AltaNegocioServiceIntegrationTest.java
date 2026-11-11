package com.innovax.barberia.alta;

import com.innovax.barberia.auth.Rol;
import com.innovax.barberia.auth.UsuarioRepository;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.negocio.Rubro;
import com.innovax.barberia.sucursal.SucursalRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Alta de barberías nuevas (lo que usa scripts/alta-negocio.sh). @Transactional: todo se revierte. */
@SpringBootTest
@Transactional
class AltaNegocioServiceIntegrationTest {

    @Autowired private AltaNegocioService alta;
    @Autowired private NegocioRepository negocios;
    @Autowired private SucursalRepository sucursales;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private WebApplicationContext context;

    private static DatosAltaNegocio elTano(String slug, String email) {
        return new DatosAltaNegocio("Barbería El Tano", slug, null, null,
                "Sobremonte 1234", "Río Cuarto", "+5493584000000", email, "tano2026!", null);
    }

    @Test
    void creaElNegocioConSuSucursalYSuAdmin() {
        ResultadoAltaNegocio r = alta.crear(elTano(null, "Tano@Ejemplo.com"));

        var negocio = negocios.findById(r.negocioId()).orElseThrow();
        assertThat(negocio.getNombre()).isEqualTo("Barbería El Tano");
        assertThat(negocio.getSlug()).isEqualTo("barberia-el-tano");
        assertThat(negocio.getZonaHoraria()).isEqualTo("America/Argentina/Cordoba");
        assertThat(negocio.isReservasOnline()).isFalse();

        assertThat(sucursales.findAll()).anySatisfy(s -> {
            assertThat(s.getNegocio().getId()).isEqualTo(r.negocioId());
            assertThat(s.getNombre()).isEqualTo("Barbería El Tano");
            assertThat(s.getCiudad()).isEqualTo("Río Cuarto");
        });

        var admin = usuarios.findByEmailIgnoreCase("tano@ejemplo.com").orElseThrow();
        assertThat(admin.getEmail()).isEqualTo("tano@ejemplo.com");
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(admin.getNegocio().getId()).isEqualTo(r.negocioId());
        assertThat(passwordEncoder.matches("tano2026!", admin.getPasswordHash())).isTrue();
    }

    @Test
    void elAdminNuevoEntraYVeSuNegocioYNoLaDeOtros() throws Exception {
        alta.crear(elTano("el-tano", "tano@ejemplo.com"));
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tano@ejemplo.com\",\"password\":\"tano2026!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + JsonPath.read(body, "$.token");

        mvc.perform(get("/api/negocio").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("el-tano"))
                .andExpect(jsonPath("$.reservasOnline").value(false));
        // Arranca vacía: no ve los barberos de Jade
        mvc.perform(get("/api/profesionales").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void respetaElSlugElegido() {
        assertThat(alta.crear(elTano("tano-barber", "tano@ejemplo.com")).slug()).isEqualTo("tano-barber");
    }

    @Test
    void noPermiteUnSlugRepetido() {
        assertThatThrownBy(() -> alta.crear(elTano("jade", "tano@ejemplo.com")))
                .isInstanceOf(AltaInvalidaException.class)
                .hasMessageContaining("slug");
    }

    @Test
    void noPermiteUnEmailQueYaUsaOtroUsuario() {
        assertThatThrownBy(() -> alta.crear(elTano("el-tano", "ADMIN@jade.com")))
                .isInstanceOf(AltaInvalidaException.class)
                .hasMessageContaining("email");
    }

    @Test
    void rechazaDatosInvalidos() {
        assertThatThrownBy(() -> alta.crear(elTano("El Tano!", "tano@ejemplo.com")))
                .hasMessageContaining("slug");
        assertThatThrownBy(() -> alta.crear(elTano("el-tano", "no-es-un-email")))
                .hasMessageContaining("email");
        assertThatThrownBy(() -> alta.crear(new DatosAltaNegocio("El Tano", null, "Marte/Olympus", null,
                "Sobremonte 1234", "Río Cuarto", null, "tano@ejemplo.com", "tano2026!", null)))
                .hasMessageContaining("zona horaria");
        assertThatThrownBy(() -> alta.crear(new DatosAltaNegocio("El Tano", null, null, null,
                " ", "Río Cuarto", null, "tano@ejemplo.com", "tano2026!", null)))
                .hasMessageContaining("dirección");
        assertThatThrownBy(() -> alta.crear(new DatosAltaNegocio("El Tano", null, null, null,
                "Sobremonte 1234", "Río Cuarto", null, "tano@ejemplo.com", "corta", null)))
                .hasMessageContaining("al menos 8");
    }

    @Test
    void sinRubroElNegocioEsUnaBarberia() {
        ResultadoAltaNegocio r = alta.crear(elTano("el-tano", "tano@ejemplo.com"));

        assertThat(r.rubro()).isEqualTo(Rubro.BARBERIA);
        assertThat(negocios.findById(r.negocioId()).orElseThrow().getRubro()).isEqualTo(Rubro.BARBERIA);
    }

    @Test
    void guardaElRubroQueSeLePasa() {
        ResultadoAltaNegocio r = alta.crear(new DatosAltaNegocio("Consultorio Sol", "consultorio-sol", null, null,
                "Sobremonte 1234", "Río Cuarto", null, "sol@ejemplo.com", "sol2026!!", "salud"));

        assertThat(r.rubro()).isEqualTo(Rubro.SALUD);
        assertThat(negocios.findById(r.negocioId()).orElseThrow().getRubro()).isEqualTo(Rubro.SALUD);
    }

    @Test
    void rechazaUnRubroQueNoExiste() {
        assertThatThrownBy(() -> alta.crear(new DatosAltaNegocio("El Tano", null, null, null,
                "Sobremonte 1234", "Río Cuarto", null, "tano@ejemplo.com", "tano2026!", "PANADERIA")))
                .isInstanceOf(AltaInvalidaException.class)
                .hasMessageContaining("rubro")
                .hasMessageContaining("SALUD");
    }

    @Test
    void armaElSlugDesdeElNombre() {
        assertThat(AltaNegocioService.slugDe("Barbería El Tano")).isEqualTo("barberia-el-tano");
        assertThat(AltaNegocioService.slugDe("  Jade & Co. — Centro ")).isEqualTo("jade-co-centro");
        assertThat(AltaNegocioService.slugDe("Peñarol 2")).isEqualTo("penarol-2");
    }
}

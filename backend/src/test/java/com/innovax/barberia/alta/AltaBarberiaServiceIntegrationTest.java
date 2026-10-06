package com.innovax.barberia.alta;

import com.innovax.barberia.auth.Rol;
import com.innovax.barberia.auth.UsuarioRepository;
import com.innovax.barberia.barberia.BarberiaRepository;
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

/** Alta de barberías nuevas (lo que usa scripts/alta-barberia.sh). @Transactional: todo se revierte. */
@SpringBootTest
@Transactional
class AltaBarberiaServiceIntegrationTest {

    @Autowired private AltaBarberiaService alta;
    @Autowired private BarberiaRepository barberias;
    @Autowired private SucursalRepository sucursales;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private WebApplicationContext context;

    private static DatosAltaBarberia elTano(String slug, String email) {
        return new DatosAltaBarberia("Barbería El Tano", slug, null, null,
                "Sobremonte 1234", "Río Cuarto", "+5493584000000", email, "tano2026!");
    }

    @Test
    void creaLaBarberiaConSuSucursalYSuAdmin() {
        ResultadoAltaBarberia r = alta.crear(elTano(null, "Tano@Ejemplo.com"));

        var barberia = barberias.findById(r.barberiaId()).orElseThrow();
        assertThat(barberia.getNombre()).isEqualTo("Barbería El Tano");
        assertThat(barberia.getSlug()).isEqualTo("barberia-el-tano");
        assertThat(barberia.getZonaHoraria()).isEqualTo("America/Argentina/Cordoba");
        assertThat(barberia.isReservasOnline()).isFalse();

        assertThat(sucursales.findAll()).anySatisfy(s -> {
            assertThat(s.getBarberia().getId()).isEqualTo(r.barberiaId());
            assertThat(s.getNombre()).isEqualTo("Barbería El Tano");
            assertThat(s.getCiudad()).isEqualTo("Río Cuarto");
        });

        var admin = usuarios.findByEmailIgnoreCase("tano@ejemplo.com").orElseThrow();
        assertThat(admin.getEmail()).isEqualTo("tano@ejemplo.com");
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(admin.getBarberia().getId()).isEqualTo(r.barberiaId());
        assertThat(passwordEncoder.matches("tano2026!", admin.getPasswordHash())).isTrue();
    }

    @Test
    void elAdminNuevoEntraYVeSuBarberiaYNoLaDeOtros() throws Exception {
        alta.crear(elTano("el-tano", "tano@ejemplo.com"));
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tano@ejemplo.com\",\"password\":\"tano2026!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + JsonPath.read(body, "$.token");

        mvc.perform(get("/api/barberia").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("el-tano"))
                .andExpect(jsonPath("$.reservasOnline").value(false));
        // Arranca vacía: no ve los barberos de Jade
        mvc.perform(get("/api/barberos").header("Authorization", token))
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
        assertThatThrownBy(() -> alta.crear(new DatosAltaBarberia("El Tano", null, "Marte/Olympus", null,
                "Sobremonte 1234", "Río Cuarto", null, "tano@ejemplo.com", "tano2026!")))
                .hasMessageContaining("zona horaria");
        assertThatThrownBy(() -> alta.crear(new DatosAltaBarberia("El Tano", null, null, null,
                " ", "Río Cuarto", null, "tano@ejemplo.com", "tano2026!")))
                .hasMessageContaining("dirección");
        assertThatThrownBy(() -> alta.crear(new DatosAltaBarberia("El Tano", null, null, null,
                "Sobremonte 1234", "Río Cuarto", null, "tano@ejemplo.com", "corta")))
                .hasMessageContaining("al menos 8");
    }

    @Test
    void armaElSlugDesdeElNombre() {
        assertThat(AltaBarberiaService.slugDe("Barbería El Tano")).isEqualTo("barberia-el-tano");
        assertThat(AltaBarberiaService.slugDe("  Jade & Co. — Centro ")).isEqualTo("jade-co-centro");
        assertThat(AltaBarberiaService.slugDe("Peñarol 2")).isEqualTo("penarol-2");
    }
}

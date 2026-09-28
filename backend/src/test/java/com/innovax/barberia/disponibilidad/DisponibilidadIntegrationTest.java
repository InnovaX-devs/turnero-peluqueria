package com.innovax.barberia.disponibilidad;

import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.servicio.ServicioRepository;
import com.innovax.barberia.sucursal.Sucursal;
import com.innovax.barberia.sucursal.SucursalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reglas de negocio de GET /api/public/disponibilidad que no cubre la calculadora:
 * anticipación máxima, desactivados y reservas online apagadas.
 * Usa los datos del seed de dev (necesita la base levantada). @Transactional: no deja cambios.
 */
@SpringBootTest
@Transactional
class DisponibilidadIntegrationTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    private static final UUID BARBERIA_JADE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID SUCURSAL = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private static final UUID SERVICIO_CORTE = UUID.fromString("dddddddd-0000-0000-0000-000000000001");

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private BarberiaRepository barberias;
    @Autowired
    private SucursalRepository sucursales;
    @Autowired
    private ServicioRepository servicios;

    private MockMvc mvc;
    private LocalDate proximoLunes;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        proximoLunes = LocalDate.now(ZONA).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private ResultActions consultar(LocalDate fecha) throws Exception {
        return mvc.perform(get("/api/public/disponibilidad")
                .param("sucursal", SUCURSAL.toString())
                .param("servicio", SERVICIO_CORTE.toString())
                .param("fecha", fecha.toString()));
    }

    @Test
    void unDiaLaboralDentroDelRangoTieneHorarios() throws Exception {
        consultar(proximoLunes)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", not(empty())));
    }

    @Test
    void masAllaDeLaAnticipacionMaximaNoHayHorarios() throws Exception {
        // El seed tiene 30 días de anticipación máxima; 10 semanas después siempre queda afuera
        consultar(proximoLunes.plusWeeks(10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", empty()));
    }

    @Test
    void servicioDesactivadoDevuelve404() throws Exception {
        Servicio corte = servicios.findById(SERVICIO_CORTE).orElseThrow();
        corte.setActivo(false);
        servicios.saveAndFlush(corte);

        consultar(proximoLunes)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Servicio no encontrado"));
    }

    @Test
    void sucursalDesactivadaDevuelve404() throws Exception {
        Sucursal sucursal = sucursales.findById(SUCURSAL).orElseThrow();
        sucursal.setActiva(false);
        sucursales.saveAndFlush(sucursal);

        consultar(proximoLunes)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Sucursal no encontrada"));
    }

    @Test
    void conReservasOnlineApagadasDevuelve400() throws Exception {
        Barberia jade = barberias.findById(BARBERIA_JADE).orElseThrow();
        jade.setReservasOnline(false);
        barberias.saveAndFlush(jade);

        consultar(proximoLunes)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.barberia").exists());
    }

    // ---------- /dias ----------

    private ResultActions consultarDias(LocalDate desde, LocalDate hasta) throws Exception {
        return mvc.perform(get("/api/public/disponibilidad/dias")
                .param("sucursal", SUCURSAL.toString())
                .param("servicio", SERVICIO_CORTE.toString())
                .param("desde", desde.toString())
                .param("hasta", hasta.toString()));
    }

    @Test
    void unaSemanaConAnticipacionMaximaMasCortaNoFallaYSeCortaEnElLimite() throws Exception {
        Barberia jade = barberias.findById(BARBERIA_JADE).orElseThrow();
        jade.setAnticipacionMaximaDias(3);
        barberias.saveAndFlush(jade);

        LocalDate hoy = LocalDate.now(ZONA);
        consultarDias(hoy, hoy.plusDays(6))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias", everyItem(lessThanOrEqualTo(hoy.plusDays(3).toString()))));
    }

    @Test
    void unRangoDemasiadoLargoDevuelve400() throws Exception {
        LocalDate hoy = LocalDate.now(ZONA);
        consultarDias(hoy, hoy.plusDays(100))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.hasta").exists());
    }
}

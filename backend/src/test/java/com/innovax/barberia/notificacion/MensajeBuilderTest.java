package com.innovax.barberia.notificacion;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.cliente.Cliente;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.Rubro;
import com.innovax.barberia.profesional.Profesional;
import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.sucursal.Sucursal;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Los mensajes al cliente hablan en los términos del rubro del negocio (issue #65). */
class MensajeBuilderTest {

    private final MensajeBuilder mensajes = new MensajeBuilder(new NotificacionLinks("https://turnea.test"));

    private static Atencion turno(Rubro rubro, String nombreServicio) {
        Negocio negocio = new Negocio();
        negocio.setNombre("Mi negocio");
        negocio.setRubro(rubro);

        Sucursal sucursal = new Sucursal();
        sucursal.setDireccion("Sobremonte 1234");
        sucursal.setCiudad("Río Cuarto");

        Profesional profesional = new Profesional();
        profesional.setNombre("Ana");
        profesional.setApellido("Pérez");

        Servicio servicio = new Servicio();
        servicio.setNombre(nombreServicio);

        Cliente cliente = new Cliente();
        cliente.setNombre("Juan");

        Atencion a = new Atencion();
        a.setNegocio(negocio);
        a.setSucursal(sucursal);
        a.setProfesional(profesional);
        a.setServicio(servicio);
        a.setCliente(cliente);
        a.setInicio(Instant.parse("2030-03-10T13:00:00Z"));
        a.setTokenGestion(UUID.randomUUID());
        return a;
    }

    @Test
    void unaBarberiaSigueDiciendoBarberoYServicio() {
        String texto = mensajes.confirmacion(turno(Rubro.BARBERIA, "Corte"));

        assertThat(texto).contains("Barbero: Ana Pérez").contains("Servicio: Corte");
    }

    @Test
    void unNegocioDeSaludDiceProfesionalYPrestacionSinPalabrasDeBarberia() {
        Atencion a = turno(Rubro.SALUD, "Consulta clínica");

        for (String texto : new String[]{mensajes.confirmacion(a), mensajes.recordatorio(a)}) {
            assertThat(texto).contains("Profesional: Ana Pérez").contains("Prestación: Consulta clínica");
            assertThat(texto).doesNotContainIgnoringCase("barber").doesNotContainIgnoringCase("corte");
        }
    }
}

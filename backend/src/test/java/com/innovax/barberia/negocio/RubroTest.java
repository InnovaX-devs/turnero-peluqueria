package com.innovax.barberia.negocio;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RubroTest {

    @Test
    void laBarberiaUsaLosTerminosDeSiempre() {
        assertThat(Rubro.BARBERIA.negocio().singular()).isEqualTo("barbería");
        assertThat(Rubro.BARBERIA.profesional().capitalizado()).isEqualTo("Barbero");
        assertThat(Rubro.BARBERIA.cliente().capitalizado()).isEqualTo("Cliente");
        assertThat(Rubro.BARBERIA.atencion().plural()).isEqualTo("cortes");
    }

    @Test
    void saludUsaProfesionalPacientePrestacionYAtencion() {
        assertThat(Rubro.SALUD.profesional().capitalizado()).isEqualTo("Profesional");
        assertThat(Rubro.SALUD.cliente().capitalizado()).isEqualTo("Paciente");
        assertThat(Rubro.SALUD.servicio().capitalizado()).isEqualTo("Prestación");
        assertThat(Rubro.SALUD.atencion().plural()).isEqualTo("atenciones");
    }

    @Test
    void losArticulosConcuerdanConElGenero() {
        assertThat(Rubro.BARBERIA.negocio().el()).isEqualTo("la barbería");
        assertThat(Rubro.BARBERIA.negocio().del()).isEqualTo("de la barbería");
        assertThat(Rubro.BARBERIA.negocio().al()).isEqualTo("a la barbería");
        assertThat(Rubro.BARBERIA.negocio().esteCapitalizado()).isEqualTo("Esta barbería");
        assertThat(Rubro.SALUD.negocio().el()).isEqualTo("el consultorio");
        assertThat(Rubro.SALUD.negocio().del()).isEqualTo("del consultorio");
        assertThat(Rubro.SALUD.negocio().al()).isEqualTo("al consultorio");
        assertThat(Rubro.SALUD.servicio().eseCapitalizado()).isEqualTo("Esa prestación");
    }

    @Test
    void unNegocioDeSaludNoUsaPalabrasDeBarberia() {
        for (Rubro rubro : new Rubro[]{Rubro.PELUQUERIA, Rubro.ESTETICA, Rubro.SALUD}) {
            for (var termino : new Rubro.Termino[]{rubro.negocio(), rubro.profesional(), rubro.cliente(),
                    rubro.servicio(), rubro.atencion()}) {
                assertThat(termino.singular() + " " + termino.plural()).doesNotContainIgnoringCase("barber");
                assertThat(termino.singular() + " " + termino.plural()).doesNotContainIgnoringCase("corte");
            }
        }
    }
}

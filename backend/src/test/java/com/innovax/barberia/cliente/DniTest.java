package com.innovax.barberia.cliente;

import com.innovax.barberia.common.DatosInvalidosException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DniTest {

    @Test
    void aceptaSieteYOchoDigitos() {
        assertThat(Dni.normalizar("1234567")).isEqualTo("1234567");
        assertThat(Dni.normalizar("12345678")).isEqualTo("12345678");
    }

    @Test
    void sacaPuntosYEspacios() {
        assertThat(Dni.normalizar("12.345.678")).isEqualTo("12345678");
        assertThat(Dni.normalizar(" 1.234.567 ")).isEqualTo("1234567");
    }

    @Test
    void vacioEsNull() {
        assertThat(Dni.normalizar(null)).isNull();
        assertThat(Dni.normalizar("")).isNull();
        assertThat(Dni.normalizar("   ")).isNull();
    }

    @Test
    void rechazaLoQueNoSonSieteOOchoDigitos() {
        for (String invalido : new String[]{"123456", "123456789", "12A45678", "12-345-678", "-1234567"}) {
            assertThatThrownBy(() -> Dni.normalizar(invalido))
                    .as(invalido)
                    .isInstanceOf(DatosInvalidosException.class);
        }
    }
}

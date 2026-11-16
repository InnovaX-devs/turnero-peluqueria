package com.innovax.barberia.cobertura;

/** Cómo se paga una atención (issue #67). */
public enum TipoPago {
    /** La paga el paciente entera. */
    PARTICULAR,
    /** La cubre la obra social / prepaga; el paciente no paga nada en el momento. */
    COBERTURA,
    /** La cubre la obra social / prepaga y el paciente paga un copago. */
    COBERTURA_CON_COPAGO;

    public boolean usaCobertura() {
        return this != PARTICULAR;
    }
}

package com.innovax.barberia.cliente;

import com.innovax.barberia.common.DatosInvalidosException;

import java.util.regex.Pattern;

/** Normalización y validación del DNI argentino: 7 u 8 dígitos. */
public final class Dni {

    private static final Pattern FORMATO = Pattern.compile("^[0-9]{7,8}$");

    private Dni() {
    }

    /**
     * Saca puntos y espacios ("12.345.678" → "12345678"). Devuelve null si viene vacío.
     *
     * @throws DatosInvalidosException si lo que queda no son 7 u 8 dígitos
     */
    public static String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.replaceAll("[.\\s]", "");
        if (!FORMATO.matcher(limpio).matches()) {
            throw new DatosInvalidosException("dni", "El DNI tiene que tener 7 u 8 números, sin puntos");
        }
        return limpio;
    }
}

package com.innovax.barberia.cobertura.dto;

import com.innovax.barberia.cobertura.Cobertura;

import java.util.UUID;

/** Panel: incluye si está activa. La página pública solo recibe las activas (activa siempre true). */
public record CoberturaResponse(UUID id, String nombre, boolean activa) {

    public static CoberturaResponse de(Cobertura c) {
        return new CoberturaResponse(c.getId(), c.getNombre(), c.isActiva());
    }
}

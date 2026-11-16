package com.innovax.barberia.cliente.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Obra social / prepaga del paciente (issue #67). {@code coberturaId} null = particular (se borran
 * también afiliado y plan).
 */
public record ActualizarCoberturaRequest(
        UUID coberturaId,

        @Size(max = 40, message = "El número de afiliado puede tener hasta 40 caracteres")
        String numeroAfiliado,

        @Size(max = 60, message = "El plan puede tener hasta 60 caracteres")
        String plan
) {
}

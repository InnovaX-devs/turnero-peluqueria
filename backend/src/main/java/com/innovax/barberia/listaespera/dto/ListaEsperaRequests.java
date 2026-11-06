package com.innovax.barberia.listaespera.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class ListaEsperaRequests {

    private ListaEsperaRequests() {
    }

    /** profesionalId null = con cualquiera. Para un profesional logueado, siempre él mismo. */
    public record Crear(
            @NotNull(message = "Elegí el cliente")
            UUID clienteId,

            UUID profesionalId,

            @NotNull(message = "La fecha es obligatoria")
            LocalDate fecha,

            @Size(max = 500, message = "La nota puede tener hasta 500 caracteres")
            String nota) {
    }
}

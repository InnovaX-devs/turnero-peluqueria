package com.innovax.barberia.listaespera.dto;

import com.innovax.barberia.listaespera.ListaEspera;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ListaEsperaResponse(
        UUID id,
        UUID clienteId,
        String clienteNombre,
        String clienteTelefono,
        /** Null = con cualquier profesional. */
        UUID profesionalId,
        String profesionalNombre,
        LocalDate fecha,
        String nota,
        /** Cuándo se liberó un lugar que le sirve. Null = todavía no. */
        Instant avisoEn,
        String avisoDetalle,
        Instant creadoEn) {

    public static ListaEsperaResponse de(ListaEspera l) {
        var p = l.getProfesional();
        return new ListaEsperaResponse(
                l.getId(),
                l.getCliente().getId(),
                l.getCliente().getNombre(),
                l.getCliente().getTelefono(),
                p != null ? p.getId() : null,
                p != null ? p.getNombre() + " " + p.getApellido() : null,
                l.getFecha(),
                l.getNota(),
                l.getAvisoEn(),
                l.getAvisoDetalle(),
                l.getCreatedAt());
    }
}

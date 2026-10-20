package com.innovax.barberia.cliente.dto;

import com.innovax.barberia.cliente.Cliente;

import java.time.Instant;
import java.util.UUID;

/** Fila del listado de clientes del panel. {@code ultimaVisita} es null si nunca completó un turno. */
public record ClienteListadoResponse(UUID id, String nombre, String telefono, boolean tieneNotas,
                                     Instant ultimaVisita) {

    public static ClienteListadoResponse de(Cliente c, Instant ultimaVisita) {
        return new ClienteListadoResponse(c.getId(), c.getNombre(), c.getTelefono(),
                c.getNotas() != null && !c.getNotas().isBlank(), ultimaVisita);
    }
}

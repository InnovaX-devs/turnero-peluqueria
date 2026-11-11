package com.innovax.barberia.cliente.dto;

import com.innovax.barberia.cliente.Cliente;

import java.util.UUID;

/** {@code dni} es null en los clientes de los rubros que no son salud. */
public record ClienteResponse(UUID id, String nombre, String telefono, String dni) {

    public static ClienteResponse de(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNombre(), c.getTelefono(), c.getDni());
    }
}
package com.innovax.barberia.cliente.dto;

import com.innovax.barberia.cliente.Cliente;

import java.util.UUID;

public record ClienteResponse(UUID id, String nombre, String telefono) {

    public static ClienteResponse de(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNombre(), c.getTelefono());
    }
}
package com.innovax.barberia.cliente.dto;

import com.innovax.barberia.cliente.Cliente;

import java.util.UUID;

/** Fila del listado de clientes del panel. */
public record ClienteListadoResponse(UUID id, String nombre, String telefono, boolean tieneNotas) {

    public static ClienteListadoResponse de(Cliente c) {
        return new ClienteListadoResponse(c.getId(), c.getNombre(), c.getTelefono(),
                c.getNotas() != null && !c.getNotas().isBlank());
    }
}
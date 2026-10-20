package com.innovax.barberia.cliente.dto;

import java.util.List;

/**
 * Una página del listado de clientes. {@code total} es la cantidad de clientes que coinciden con la
 * búsqueda (no solo los de esta página); {@code hayMas} dice si conviene pedir la página siguiente.
 */
public record PaginaClientesResponse(List<ClienteListadoResponse> clientes, long total, int pagina,
                                     int tamano, boolean hayMas) {
}

package com.innovax.barberia.reporte.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClientesInactivosResponse(String barberiaNombre, int dias, List<Cliente> clientes) {

    public record Cliente(UUID id, String nombre, String telefono, LocalDate ultimaVisita,
                          long diasSinVenir, long visitas) {
    }
}

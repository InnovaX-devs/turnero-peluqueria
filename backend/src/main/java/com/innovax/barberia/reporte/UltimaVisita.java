package com.innovax.barberia.reporte;

import java.time.Instant;
import java.util.UUID;

/** Última vez que un cliente fue atendido, y cuántas veces vino en total. */
public record UltimaVisita(UUID clienteId, String nombre, String telefono, Instant ultima, long visitas) {
}

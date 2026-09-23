package com.innovax.barberia.disponibilidad;

import java.time.Instant;

/** Un rango de tiempo ocupado: una atención (reservada/completada) o un bloqueo. Medio abierto: [inicio, fin). */
public record Intervalo(Instant inicio, Instant fin) {

    public boolean seSuperponeCon(Instant otroInicio, Instant otroFin) {
        return inicio.isBefore(otroFin) && fin.isAfter(otroInicio);
    }
}
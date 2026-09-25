package com.innovax.barberia.agenda.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * {@code barberos} son todos los barberos activos de la barbería (para el selector "ver por
 * barbero" del admin), independientemente de si tienen turnos en el rango. {@code turnos} y
 * {@code bloqueos} vienen en una sola lista, ya acotados al rango y al filtro de barbero pedido;
 * el front los agrupa/pinta por barbero y por hora.
 */
public record AgendaResponse(
        LocalDate desde,
        LocalDate hasta,
        List<BarberoResumen> barberos,
        List<TurnoAgendaResponse> turnos,
        List<BloqueoAgendaResponse> bloqueos
) {
}
package com.innovax.barberia.caja.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Resumen de caja de un período. Todos los montos en centavos.
 * Solo cuentan las atenciones COMPLETADAS (turnos atendidos y cortes registrados a mano).
 */
public record CajaResponse(
        LocalDate desde,
        LocalDate hasta,
        long total,
        int cantidad,
        long ticketPromedio,
        List<Grupo> porBarbero,
        List<Grupo> porServicio,
        List<GrupoMetodo> porMetodoPago,
        List<Dia> porDia) {

    public record Grupo(UUID id, String nombre, int cantidad, long total) {
    }

    /** metodo es null si la atención se completó sin informar cómo pagó. */
    public record GrupoMetodo(String metodo, int cantidad, long total) {
    }

    /** Un registro por cada día del período, aunque no haya habido cortes (para el gráfico). */
    public record Dia(LocalDate fecha, int cantidad, long total) {
    }
}

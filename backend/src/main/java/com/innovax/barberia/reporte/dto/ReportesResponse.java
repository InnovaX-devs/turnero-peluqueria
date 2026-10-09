package com.innovax.barberia.reporte.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Reportes de un período. Montos en centavos. */
public record ReportesResponse(
        LocalDate desde,
        LocalDate hasta,
        List<Barbero> porBarbero,
        List<ClienteFrecuente> clientesFrecuentes,
        Ausentismo ausentismo,
        List<Demanda> demanda) {

    /** facturado: suma (en centavos) de lo cobrado en los cortes atendidos del período. */
    public record Barbero(UUID id, String nombre, int atendidos, int ausentes, int cancelados, long facturado) {
    }

    public record ClienteFrecuente(UUID id, String nombre, String telefono, int visitas, long total,
                                   LocalDate ultimaVisita) {
    }

    /**
     * Sobre turnos reservados online que ya pasaron: atendidos + ausentes.
     * Los cortes registrados a mano no cuentan (nunca pueden ser un ausente).
     * sinMarcar: turnos pasados que siguen como RESERVADA (si hay muchos, la tasa no es confiable).
     */
    public record Ausentismo(int atendidos, int ausentes, int cancelados, int sinMarcar) {
    }

    /** diaSemana 1 = lunes … 7 = domingo; hora 0-23 en la hora de la barbería. */
    public record Demanda(int diaSemana, int hora, int cantidad) {
    }
}
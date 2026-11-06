package com.innovax.barberia.agenda.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Horarios libres de un profesional para un servicio en un día, para dar turno desde el panel. */
public record HorariosLibresResponse(LocalDate fecha, List<LocalTime> horas) {
}

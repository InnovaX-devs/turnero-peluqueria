package com.innovax.barberia.disponibilidad.dto;

import java.time.LocalDate;
import java.util.List;

public record DisponibilidadResponse(LocalDate fecha, List<SlotDisponibleResponse> slots) {
}
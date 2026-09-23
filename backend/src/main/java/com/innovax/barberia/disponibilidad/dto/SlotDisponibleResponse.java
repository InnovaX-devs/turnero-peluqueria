package com.innovax.barberia.disponibilidad.dto;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record SlotDisponibleResponse(LocalTime hora, List<UUID> barberos) {
}
package com.innovax.barberia.disponibilidad.dto;

import java.time.LocalDate;
import java.util.List;

public record DiasDisponiblesResponse(List<LocalDate> dias) {
}
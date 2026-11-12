package com.innovax.barberia.profesional.dto;

import com.innovax.barberia.especialidad.Especialidad;
import com.innovax.barberia.profesional.Profesional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Profesional tal como se muestra en la página pública.
 * {@code proximoDiaDisponible} es null si no tiene horarios libres dentro de la anticipación máxima.
 * {@code matricula} y {@code especialidades} se muestran en la tarjeta (rubro salud); en los demás rubros
 * vienen null / vacías.
 */
public record ProfesionalPublicoResponse(
        UUID id,
        String nombre,
        String apellido,
        String fotoUrl,
        String matricula,
        List<String> especialidades,
        LocalDate proximoDiaDisponible) {

    /** Accede a colecciones lazy: llamarlo siempre dentro de una transacción. */
    public static ProfesionalPublicoResponse de(Profesional b, LocalDate proximoDiaDisponible) {
        return new ProfesionalPublicoResponse(b.getId(), b.getNombre(), b.getApellido(), b.getFotoUrl(),
                b.getMatricula(),
                b.getEspecialidades().stream()
                        .map(Especialidad::getNombre)
                        .sorted(Comparator.comparing(String::toLowerCase))
                        .toList(),
                proximoDiaDisponible);
    }
}

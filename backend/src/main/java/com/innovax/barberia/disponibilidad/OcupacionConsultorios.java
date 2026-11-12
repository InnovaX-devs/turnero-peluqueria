package com.innovax.barberia.disponibilidad;

import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.horario.HorarioLaboral;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lo que tienen ocupado OTROS profesionales en los consultorios de los tramos de un profesional (issue #69),
 * listo para pasarle a {@link CalculadoraDisponibilidad}. Si ningún tramo tiene consultorio no consulta la base.
 */
@Component
@RequiredArgsConstructor
public class OcupacionConsultorios {

    /** Mismo criterio que la agenda del profesional: canceladas y ausentes liberan el consultorio. */
    private static final Set<EstadoAtencion> OCUPAN = EnumSet.of(EstadoAtencion.RESERVADA, EstadoAtencion.COMPLETADA);

    private final AtencionRepository atenciones;

    public Map<UUID, List<Intervalo>> ocupadosPorOtros(List<HorarioLaboral> horarios, UUID profesionalId,
                                                       Instant desde, Instant hasta) {
        Set<UUID> consultorioIds = horarios.stream()
                .map(HorarioLaboral::getConsultorio)
                .filter(Objects::nonNull)
                .map(c -> c.getId())
                .collect(Collectors.toSet());
        if (consultorioIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<Intervalo>> porConsultorio = new HashMap<>();
        atenciones.findOcupadasEnConsultoriosPorOtros(consultorioIds, profesionalId, OCUPAN, desde, hasta)
                .forEach(a -> porConsultorio
                        .computeIfAbsent(a.getConsultorio().getId(), id -> new ArrayList<>())
                        .add(new Intervalo(a.getInicio(), a.getFin())));
        return porConsultorio;
    }
}

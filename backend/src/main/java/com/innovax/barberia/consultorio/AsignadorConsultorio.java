package com.innovax.barberia.consultorio;

import com.innovax.barberia.horario.HorarioLaboral;
import com.innovax.barberia.horario.HorarioLaboralRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * Decide en qué consultorio va un turno (issue #69): el del tramo del horario del profesional en el que
 * empieza el turno. Si ese tramo no tiene consultorio, o el turno cae fuera del horario (un sobreturno o
 * una atención cargada a mano), el turno queda sin consultorio.
 *
 * <p>El consultorio se guarda en el turno al darlo: si después el admin cambia el horario, los turnos ya
 * dados conservan el consultorio que tenían (y lo siguen ocupando).
 */
@Component
@RequiredArgsConstructor
public class AsignadorConsultorio {

    private final HorarioLaboralRepository horarios;

    /** {@code inicio} se pasa a la hora local del negocio para compararlo con el horario semanal. */
    public Consultorio paraTurno(UUID profesionalId, UUID sucursalId, Instant inicio, ZoneId zona) {
        LocalDateTime local = LocalDateTime.ofInstant(inicio, zona);
        LocalTime hora = local.toLocalTime();
        return horarios.findByProfesionalIdAndSucursalIdAndDiaSemana(profesionalId, sucursalId, local.getDayOfWeek())
                .stream()
                .filter(h -> !hora.isBefore(h.getHoraInicio()) && hora.isBefore(h.getHoraFin()))
                .map(HorarioLaboral::getConsultorio)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}

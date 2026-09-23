package com.innovax.barberia.disponibilidad;

import com.innovax.barberia.disponibilidad.dto.SlotDisponibleResponse;
import com.innovax.barberia.horario.HorarioLaboral;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public final class CalculadoraDisponibilidad {

    private CalculadoraDisponibilidad() {
    }

    public static List<LocalTime> slotsLibresDeUnBarbero(
            List<HorarioLaboral> horariosDelDia,
            List<Intervalo> ocupados,
            int duracionServicioMin,
            int intervaloSlotsMin,
            LocalDate fecha,
            ZoneId zonaHoraria,
            Instant ahora,
            int anticipacionMinimaMin) {

        Instant limiteAnticipacion = ahora.plus(Duration.ofMinutes(anticipacionMinimaMin));

        List<LocalTime> libres = new ArrayList<>();
        for (HorarioLaboral horario : horariosDelDia) {
            for (LocalTime horaCandidata : candidatosDelBloque(horario, duracionServicioMin, intervaloSlotsMin)) {
                Instant inicio = ZonedDateTime.of(fecha, horaCandidata, zonaHoraria).toInstant();
                Instant fin = inicio.plus(Duration.ofMinutes(duracionServicioMin));

                if (inicio.isBefore(limiteAnticipacion)) {
                    continue;
                }
                if (seSuperponeConAlguno(inicio, fin, ocupados)) {
                    continue;
                }
                libres.add(horaCandidata);
            }
        }

        return libres.stream().distinct().sorted().toList();
    }

    public static List<SlotDisponibleResponse> combinarPorBarbero(Map<UUID, List<LocalTime>> slotsPorBarbero) {
        Map<LocalTime, List<UUID>> agrupados = new TreeMap<>();
        slotsPorBarbero.forEach((barberoId, horas) ->
                horas.forEach(hora -> agrupados.computeIfAbsent(hora, h -> new ArrayList<>()).add(barberoId)));

        return agrupados.entrySet().stream()
                .map(e -> new SlotDisponibleResponse(e.getKey(), List.copyOf(e.getValue())))
                .toList();
    }

    private static List<LocalTime> candidatosDelBloque(HorarioLaboral horario, int duracionServicioMin, int intervaloSlotsMin) {
        int inicioMin = horario.getHoraInicio().toSecondOfDay() / 60;
        int finMin = horario.getHoraFin().toSecondOfDay() / 60;

        List<LocalTime> candidatos = new ArrayList<>();
        for (int minuto = inicioMin; minuto + duracionServicioMin <= finMin; minuto += intervaloSlotsMin) {
            candidatos.add(LocalTime.ofSecondOfDay(minuto * 60L));
        }
        return candidatos;
    }

    private static boolean seSuperponeConAlguno(Instant inicio, Instant fin, List<Intervalo> ocupados) {
        return ocupados.stream().anyMatch(o -> o.seSuperponeCon(inicio, fin));
    }
}
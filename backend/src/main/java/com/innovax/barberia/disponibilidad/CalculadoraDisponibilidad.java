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

    /** Sin consultorios compartidos (o ninguno ocupado por otros profesionales). */
    public static List<LocalTime> slotsLibresDeUnProfesional(
            List<HorarioLaboral> horariosDelDia,
            List<Intervalo> ocupados,
            int duracionServicioMin,
            int intervaloSlotsMin,
            LocalDate fecha,
            ZoneId zonaHoraria,
            Instant ahora,
            int anticipacionMinimaMin) {
        return slotsLibresDeUnProfesional(horariosDelDia, ocupados, Map.of(), duracionServicioMin, intervaloSlotsMin,
                fecha, zonaHoraria, ahora, anticipacionMinimaMin);
    }

    /**
     * @param ocupados                 turnos y bloqueos del propio profesional
     * @param ocupadosPorConsultorio   por id de consultorio, lo que tienen ocupado OTROS profesionales ese día
     *                                 (issue #69). Solo se mira para los tramos del horario que tienen consultorio:
     *                                 un slot de un tramo en el "Consultorio 2" se descarta si otro profesional
     *                                 tiene un turno en el Consultorio 2 que se superpone.
     */
    public static List<LocalTime> slotsLibresDeUnProfesional(
            List<HorarioLaboral> horariosDelDia,
            List<Intervalo> ocupados,
            Map<UUID, List<Intervalo>> ocupadosPorConsultorio,
            int duracionServicioMin,
            int intervaloSlotsMin,
            LocalDate fecha,
            ZoneId zonaHoraria,
            Instant ahora,
            int anticipacionMinimaMin) {

        Instant limiteAnticipacion = ahora.plus(Duration.ofMinutes(anticipacionMinimaMin));

        List<LocalTime> libres = new ArrayList<>();
        for (HorarioLaboral horario : horariosDelDia) {
            List<Intervalo> ocupadosDelConsultorio = horario.getConsultorio() == null
                    ? List.of()
                    : ocupadosPorConsultorio.getOrDefault(horario.getConsultorio().getId(), List.of());

            for (LocalTime horaCandidata : candidatosDelBloque(horario, duracionServicioMin, intervaloSlotsMin)) {
                Instant inicio = ZonedDateTime.of(fecha, horaCandidata, zonaHoraria).toInstant();
                Instant fin = inicio.plus(Duration.ofMinutes(duracionServicioMin));

                if (inicio.isBefore(limiteAnticipacion)) {
                    continue;
                }
                if (seSuperponeConAlguno(inicio, fin, ocupados)) {
                    continue;
                }
                if (seSuperponeConAlguno(inicio, fin, ocupadosDelConsultorio)) {
                    continue;
                }
                libres.add(horaCandidata);
            }
        }

        return libres.stream().distinct().sorted().toList();
    }

    public static List<SlotDisponibleResponse> combinarPorProfesional(Map<UUID, List<LocalTime>> slotsPorProfesional) {
        Map<LocalTime, List<UUID>> agrupados = new TreeMap<>();
        slotsPorProfesional.forEach((profesionalId, horas) ->
                horas.forEach(hora -> agrupados.computeIfAbsent(hora, h -> new ArrayList<>()).add(profesionalId)));

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
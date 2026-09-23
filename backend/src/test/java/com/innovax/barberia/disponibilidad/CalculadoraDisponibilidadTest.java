package com.innovax.barberia.disponibilidad;

import com.innovax.barberia.disponibilidad.dto.SlotDisponibleResponse;
import com.innovax.barberia.horario.HorarioLaboral;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraDisponibilidadTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    private static final LocalDate UN_LUNES = LocalDate.of(2026, 9, 28); // lunes

    private static HorarioLaboral horario(LocalTime inicio, LocalTime fin) {
        HorarioLaboral h = new HorarioLaboral();
        h.setHoraInicio(inicio);
        h.setHoraFin(fin);
        return h;
    }

    private static Instant enZona(LocalDate fecha, LocalTime hora) {
        return ZonedDateTime.of(fecha, hora, ZONA).toInstant();
    }

    @Test
    void diaSinHorarioLaboralNoTieneSlots() {
        List<LocalTime> libres = CalculadoraDisponibilidad.slotsLibresDeUnBarbero(
                List.of(), List.of(), 30, 30, UN_LUNES, ZONA,
                enZona(UN_LUNES, LocalTime.of(0, 0)), 60);

        assertThat(libres).isEmpty();
    }

    @Test
    void descartaElUltimoSlotSiElServicioNoEntraAntesDeQueCierre() {
        HorarioLaboral jornada = horario(LocalTime.of(9, 0), LocalTime.of(20, 30));

        List<LocalTime> libres = CalculadoraDisponibilidad.slotsLibresDeUnBarbero(
                List.of(jornada), List.of(), 45, 15, UN_LUNES, ZONA,
                enZona(UN_LUNES, LocalTime.of(0, 0)), 0);

        assertThat(libres).contains(LocalTime.of(19, 45));
        assertThat(libres).doesNotContain(LocalTime.of(20, 0), LocalTime.of(20, 15));
    }

    @Test
    void descartaSlotsQueSeSuperponenParcialmenteConUnaAtencion() {
        HorarioLaboral jornada = horario(LocalTime.of(9, 0), LocalTime.of(13, 0));
        Intervalo turnoOcupado = new Intervalo(enZona(UN_LUNES, LocalTime.of(10, 0)), enZona(UN_LUNES, LocalTime.of(10, 40)));

        List<LocalTime> libres = CalculadoraDisponibilidad.slotsLibresDeUnBarbero(
                List.of(jornada), List.of(turnoOcupado), 30, 30, UN_LUNES, ZONA,
                enZona(UN_LUNES, LocalTime.of(0, 0)), 0);

        assertThat(libres).contains(LocalTime.of(9, 30), LocalTime.of(11, 0));
        assertThat(libres).doesNotContain(LocalTime.of(10, 0), LocalTime.of(10, 30));
    }

    @Test
    void descartaSlotsQueCaenDentroDeUnBloqueoDeMedioDia() {
        HorarioLaboral jornada = horario(LocalTime.of(9, 0), LocalTime.of(18, 0));
        Intervalo bloqueoMañana = new Intervalo(enZona(UN_LUNES, LocalTime.of(9, 0)), enZona(UN_LUNES, LocalTime.of(13, 0)));

        List<LocalTime> libres = CalculadoraDisponibilidad.slotsLibresDeUnBarbero(
                List.of(jornada), List.of(bloqueoMañana), 30, 30, UN_LUNES, ZONA,
                enZona(UN_LUNES, LocalTime.of(0, 0)), 0);

        assertThat(libres).doesNotContain(LocalTime.of(9, 0), LocalTime.of(12, 30));
        assertThat(libres).contains(LocalTime.of(13, 0), LocalTime.of(17, 30));
    }

    @Test
    void descartaSlotsAnterioresALaAnticipacionMinima() {
        HorarioLaboral jornada = horario(LocalTime.of(9, 0), LocalTime.of(18, 0));
        Instant ahora = enZona(UN_LUNES, LocalTime.of(9, 50));

        List<LocalTime> libres = CalculadoraDisponibilidad.slotsLibresDeUnBarbero(
                List.of(jornada), List.of(), 30, 30, UN_LUNES, ZONA, ahora, 60);

        assertThat(libres).doesNotContain(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30));
        assertThat(libres).contains(LocalTime.of(11, 0));
    }

    @Test
    void cualquierBarberoUneLosSlotsDeTodosYRegistraQuienTieneCada() {
        UUID lucas = UUID.randomUUID();
        UUID andres = UUID.randomUUID();

        Map<UUID, List<LocalTime>> slotsPorBarbero = Map.of(
                lucas, List.of(LocalTime.of(9, 0), LocalTime.of(9, 30)),
                andres, List.of(LocalTime.of(9, 30), LocalTime.of(10, 0)));

        List<SlotDisponibleResponse> combinados = CalculadoraDisponibilidad.combinarPorBarbero(slotsPorBarbero);

        assertThat(combinados).extracting(SlotDisponibleResponse::hora)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0));

        SlotDisponibleResponse nueveYMedia = combinados.stream()
                .filter(s -> s.hora().equals(LocalTime.of(9, 30)))
                .findFirst().orElseThrow();
        assertThat(nueveYMedia.barberos()).containsExactlyInAnyOrder(lucas, andres);

        SlotDisponibleResponse nueveEnPunto = combinados.stream()
                .filter(s -> s.hora().equals(LocalTime.of(9, 0)))
                .findFirst().orElseThrow();
        assertThat(nueveEnPunto.barberos()).containsExactly(lucas);
    }
}
package com.innovax.barberia.disponibilidad;

import com.innovax.barberia.consultorio.Consultorio;
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

    private static HorarioLaboral horario(LocalTime inicio, LocalTime fin, Consultorio consultorio) {
        HorarioLaboral h = horario(inicio, fin);
        h.setConsultorio(consultorio);
        return h;
    }

    private static Consultorio consultorio(String nombre) {
        Consultorio c = new Consultorio();
        c.setId(UUID.randomUUID());
        c.setNombre(nombre);
        return c;
    }

    private static Instant enZona(LocalDate fecha, LocalTime hora) {
        return ZonedDateTime.of(fecha, hora, ZONA).toInstant();
    }

    @Test
    void diaSinHorarioLaboralNoTieneSlots() {
        List<LocalTime> libres = CalculadoraDisponibilidad.slotsLibresDeUnProfesional(
                List.of(), List.of(), 30, 30, UN_LUNES, ZONA,
                enZona(UN_LUNES, LocalTime.of(0, 0)), 60);

        assertThat(libres).isEmpty();
    }

    @Test
    void descartaElUltimoSlotSiElServicioNoEntraAntesDeQueCierre() {
        HorarioLaboral jornada = horario(LocalTime.of(9, 0), LocalTime.of(20, 30));

        List<LocalTime> libres = CalculadoraDisponibilidad.slotsLibresDeUnProfesional(
                List.of(jornada), List.of(), 45, 15, UN_LUNES, ZONA,
                enZona(UN_LUNES, LocalTime.of(0, 0)), 0);

        assertThat(libres).contains(LocalTime.of(19, 45));
        assertThat(libres).doesNotContain(LocalTime.of(20, 0), LocalTime.of(20, 15));
    }

    @Test
    void descartaSlotsQueSeSuperponenParcialmenteConUnaAtencion() {
        HorarioLaboral jornada = horario(LocalTime.of(9, 0), LocalTime.of(13, 0));
        Intervalo turnoOcupado = new Intervalo(enZona(UN_LUNES, LocalTime.of(10, 0)), enZona(UN_LUNES, LocalTime.of(10, 40)));

        List<LocalTime> libres = CalculadoraDisponibilidad.slotsLibresDeUnProfesional(
                List.of(jornada), List.of(turnoOcupado), 30, 30, UN_LUNES, ZONA,
                enZona(UN_LUNES, LocalTime.of(0, 0)), 0);

        assertThat(libres).contains(LocalTime.of(9, 30), LocalTime.of(11, 0));
        assertThat(libres).doesNotContain(LocalTime.of(10, 0), LocalTime.of(10, 30));
    }

    @Test
    void descartaSlotsQueCaenDentroDeUnBloqueoDeMedioDia() {
        HorarioLaboral jornada = horario(LocalTime.of(9, 0), LocalTime.of(18, 0));
        Intervalo bloqueoMañana = new Intervalo(enZona(UN_LUNES, LocalTime.of(9, 0)), enZona(UN_LUNES, LocalTime.of(13, 0)));

        List<LocalTime> libres = CalculadoraDisponibilidad.slotsLibresDeUnProfesional(
                List.of(jornada), List.of(bloqueoMañana), 30, 30, UN_LUNES, ZONA,
                enZona(UN_LUNES, LocalTime.of(0, 0)), 0);

        assertThat(libres).doesNotContain(LocalTime.of(9, 0), LocalTime.of(12, 30));
        assertThat(libres).contains(LocalTime.of(13, 0), LocalTime.of(17, 30));
    }

    @Test
    void descartaSlotsAnterioresALaAnticipacionMinima() {
        HorarioLaboral jornada = horario(LocalTime.of(9, 0), LocalTime.of(18, 0));
        Instant ahora = enZona(UN_LUNES, LocalTime.of(9, 50));

        List<LocalTime> libres = CalculadoraDisponibilidad.slotsLibresDeUnProfesional(
                List.of(jornada), List.of(), 30, 30, UN_LUNES, ZONA, ahora, 60);

        assertThat(libres).doesNotContain(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30));
        assertThat(libres).contains(LocalTime.of(11, 0));
    }

    @Test
    void cualquierProfesionalUneLosSlotsDeTodosYRegistraQuienTieneCada() {
        UUID lucas = UUID.randomUUID();
        UUID andres = UUID.randomUUID();

        Map<UUID, List<LocalTime>> slotsPorProfesional = Map.of(
                lucas, List.of(LocalTime.of(9, 0), LocalTime.of(9, 30)),
                andres, List.of(LocalTime.of(9, 30), LocalTime.of(10, 0)));

        List<SlotDisponibleResponse> combinados = CalculadoraDisponibilidad.combinarPorProfesional(slotsPorProfesional);

        assertThat(combinados).extracting(SlotDisponibleResponse::hora)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0));

        SlotDisponibleResponse nueveYMedia = combinados.stream()
                .filter(s -> s.hora().equals(LocalTime.of(9, 30)))
                .findFirst().orElseThrow();
        assertThat(nueveYMedia.profesionales()).containsExactlyInAnyOrder(lucas, andres);

        SlotDisponibleResponse nueveEnPunto = combinados.stream()
                .filter(s -> s.hora().equals(LocalTime.of(9, 0)))
                .findFirst().orElseThrow();
        assertThat(nueveEnPunto.profesionales()).containsExactly(lucas);
    }

    // ---------- Consultorio compartido (issue #69) ----------

    @Test
    void descartaSlotsDondeOtroProfesionalOcupaElConsultorioDelTramo() {
        Consultorio c2 = consultorio("Consultorio 2");
        HorarioLaboral martesEnC2 = horario(LocalTime.of(9, 0), LocalTime.of(13, 0), c2);
        // Otro profesional tiene un turno en el Consultorio 2 de 10:00 a 10:45
        Map<UUID, List<Intervalo>> ocupadosPorConsultorio = Map.of(c2.getId(),
                List.of(new Intervalo(enZona(UN_LUNES, LocalTime.of(10, 0)), enZona(UN_LUNES, LocalTime.of(10, 45)))));

        List<LocalTime> libres = CalculadoraDisponibilidad.slotsLibresDeUnProfesional(
                List.of(martesEnC2), List.of(), ocupadosPorConsultorio, 30, 30, UN_LUNES, ZONA,
                enZona(UN_LUNES, LocalTime.of(0, 0)), 0);

        assertThat(libres).contains(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(11, 0));
        assertThat(libres).doesNotContain(LocalTime.of(10, 0), LocalTime.of(10, 30));
    }

    @Test
    void laOcupacionDelConsultorioSoloAfectaALosTramosEnEseConsultorio() {
        Consultorio c2 = consultorio("Consultorio 2");
        Consultorio c3 = consultorio("Consultorio 3");
        HorarioLaboral mananaEnC3 = horario(LocalTime.of(9, 0), LocalTime.of(13, 0), c3);
        HorarioLaboral tardeSinConsultorio = horario(LocalTime.of(16, 0), LocalTime.of(18, 0));
        // El Consultorio 2 está ocupado todo el día por otro: a este profesional no le importa
        Map<UUID, List<Intervalo>> ocupadosPorConsultorio = Map.of(c2.getId(),
                List.of(new Intervalo(enZona(UN_LUNES, LocalTime.of(0, 0)), enZona(UN_LUNES.plusDays(1), LocalTime.of(0, 0)))));

        List<LocalTime> libres = CalculadoraDisponibilidad.slotsLibresDeUnProfesional(
                List.of(mananaEnC3, tardeSinConsultorio), List.of(), ocupadosPorConsultorio, 30, 30, UN_LUNES, ZONA,
                enZona(UN_LUNES, LocalTime.of(0, 0)), 0);

        assertThat(libres).contains(LocalTime.of(9, 0), LocalTime.of(12, 30), LocalTime.of(16, 0), LocalTime.of(17, 30));
    }

    @Test
    void unTramoSinConsultorioIgnoraLaOcupacionDeConsultorios() {
        Consultorio c2 = consultorio("Consultorio 2");
        HorarioLaboral sinConsultorio = horario(LocalTime.of(9, 0), LocalTime.of(11, 0));
        Map<UUID, List<Intervalo>> ocupadosPorConsultorio = Map.of(c2.getId(),
                List.of(new Intervalo(enZona(UN_LUNES, LocalTime.of(9, 0)), enZona(UN_LUNES, LocalTime.of(11, 0)))));

        List<LocalTime> libres = CalculadoraDisponibilidad.slotsLibresDeUnProfesional(
                List.of(sinConsultorio), List.of(), ocupadosPorConsultorio, 30, 30, UN_LUNES, ZONA,
                enZona(UN_LUNES, LocalTime.of(0, 0)), 0);

        assertThat(libres).containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30));
    }

    @Test
    void conConsultorioSiguenContandoLosTurnosPropiosYLosDelConsultorio() {
        Consultorio c2 = consultorio("Consultorio 2");
        HorarioLaboral enC2 = horario(LocalTime.of(9, 0), LocalTime.of(12, 0), c2);
        List<Intervalo> propios = List.of(new Intervalo(enZona(UN_LUNES, LocalTime.of(9, 0)), enZona(UN_LUNES, LocalTime.of(9, 30))));
        Map<UUID, List<Intervalo>> delConsultorio = Map.of(c2.getId(),
                List.of(new Intervalo(enZona(UN_LUNES, LocalTime.of(11, 0)), enZona(UN_LUNES, LocalTime.of(11, 30)))));

        List<LocalTime> libres = CalculadoraDisponibilidad.slotsLibresDeUnProfesional(
                List.of(enC2), propios, delConsultorio, 30, 30, UN_LUNES, ZONA,
                enZona(UN_LUNES, LocalTime.of(0, 0)), 0);

        assertThat(libres).containsExactly(LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30), LocalTime.of(11, 30));
    }
}
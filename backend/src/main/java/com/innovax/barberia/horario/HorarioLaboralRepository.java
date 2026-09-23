package com.innovax.barberia.horario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface HorarioLaboralRepository extends JpaRepository<HorarioLaboral, UUID> {

    /** Los bloques de horario de un barbero en una sucursal para un día de la semana dado (puede haber varios, ej. mañana y tarde). */
    List<HorarioLaboral> findByBarberoIdAndSucursalIdAndDiaSemana(UUID barberoId, UUID sucursalId, DayOfWeek diaSemana);
}
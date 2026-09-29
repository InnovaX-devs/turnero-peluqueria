package com.innovax.barberia.horario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface HorarioLaboralRepository extends JpaRepository<HorarioLaboral, UUID> {

    /** Los bloques de horario de un barbero en una sucursal para un día de la semana dado (puede haber varios, ej. mañana y tarde). */
    List<HorarioLaboral> findByBarberoIdAndSucursalIdAndDiaSemana(UUID barberoId, UUID sucursalId, DayOfWeek diaSemana);

    List<HorarioLaboral> findByBarberoIdOrderByDiaSemanaAscHoraInicioAsc(UUID barberoId);

    @Modifying(flushAutomatically = true)
    @Query("delete from HorarioLaboral h where h.barbero.id = :barberoId")
    void borrarDeBarbero(@Param("barberoId") UUID barberoId);
}
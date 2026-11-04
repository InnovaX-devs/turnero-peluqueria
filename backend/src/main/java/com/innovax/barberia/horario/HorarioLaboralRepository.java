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
    List<HorarioLaboral> findByProfesionalIdAndSucursalIdAndDiaSemana(UUID profesionalId, UUID sucursalId, DayOfWeek diaSemana);

    List<HorarioLaboral> findByProfesionalIdOrderByDiaSemanaAscHoraInicioAsc(UUID profesionalId);

    @Modifying(flushAutomatically = true)
    @Query("delete from HorarioLaboral h where h.profesional.id = :profesionalId")
    void borrarDeProfesional(@Param("profesionalId") UUID profesionalId);
}
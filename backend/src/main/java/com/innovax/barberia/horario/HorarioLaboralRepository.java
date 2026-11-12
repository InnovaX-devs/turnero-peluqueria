package com.innovax.barberia.horario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface HorarioLaboralRepository extends JpaRepository<HorarioLaboral, UUID> {

    /** Los bloques de horario de un barbero en una sucursal para un día de la semana dado (puede haber varios, ej. mañana y tarde). */
    List<HorarioLaboral> findByProfesionalIdAndSucursalIdAndDiaSemana(UUID profesionalId, UUID sucursalId, DayOfWeek diaSemana);

    List<HorarioLaboral> findByProfesionalIdOrderByDiaSemanaAscHoraInicioAsc(UUID profesionalId);

    /** Horarios de OTROS profesionales que usan alguno de esos consultorios (para validar que no se pisen). */
    @Query("""
            select h from HorarioLaboral h
            join fetch h.profesional
            join fetch h.consultorio
            where h.consultorio.id in :consultorioIds
              and h.profesional.id <> :profesionalId
            """)
    List<HorarioLaboral> findEnConsultoriosDeOtros(
            @Param("consultorioIds") Collection<UUID> consultorioIds,
            @Param("profesionalId") UUID profesionalId);

    /** Horarios que usan el consultorio (para no desactivarlo mientras esté asignado). */
    @Query("select h from HorarioLaboral h join fetch h.profesional where h.consultorio.id = :consultorioId")
    List<HorarioLaboral> findByConsultorioId(@Param("consultorioId") UUID consultorioId);

    @Modifying(flushAutomatically = true)
    @Query("delete from HorarioLaboral h where h.profesional.id = :profesionalId")
    void borrarDeProfesional(@Param("profesionalId") UUID profesionalId);
}
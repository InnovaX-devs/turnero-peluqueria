package com.innovax.barberia.atencion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AtencionRepository extends JpaRepository<Atencion, UUID> {

    /** ¿El paciente tiene turnos en ese estado? (issue #73: no se anonimiza con turnos reservados). */
    boolean existsByClienteIdAndEstado(UUID clienteId, EstadoAtencion estado);

    /** Issue #73: borra las notas de consulta de todos los turnos del paciente (pueden tener datos de salud). */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Atencion a
            set a.notas = null, a.notasEditadasPor = null, a.notasEditadasEn = null
            where a.cliente.id = :clienteId
            """)
    int borrarNotasDeCliente(@Param("clienteId") UUID clienteId);

    /** Atenciones del barbero, en alguno de los estados dados, que se solapan con [inicioRango, finRango). */
    @Query("""
            select a from Atencion a
            where a.profesional.id = :profesionalId
              and a.estado in :estados
              and a.inicio < :finRango
              and a.fin > :inicioRango
            """)
    List<Atencion> findOcupadas(
            @Param("profesionalId") UUID profesionalId,
            @Param("estados") Collection<EstadoAtencion> estados,
            @Param("inicioRango") Instant inicioRango,
            @Param("finRango") Instant finRango);

    /**
     * Atenciones de OTROS profesionales, en alguno de los estados dados, que ocupan alguno de esos
     * consultorios en [inicioRango, finRango) (issue #69). Las del propio profesional no se traen:
     * ya cuentan en {@link #findOcupadas}.
     */
    @Query("""
            select a from Atencion a
            join fetch a.consultorio
            where a.consultorio.id in :consultorioIds
              and a.profesional.id <> :profesionalId
              and a.estado in :estados
              and a.inicio < :finRango
              and a.fin > :inicioRango
            """)
    List<Atencion> findOcupadasEnConsultoriosPorOtros(
            @Param("consultorioIds") Collection<UUID> consultorioIds,
            @Param("profesionalId") UUID profesionalId,
            @Param("estados") Collection<EstadoAtencion> estados,
            @Param("inicioRango") Instant inicioRango,
            @Param("finRango") Instant finRango);

    /**
     * Turnos de la agenda del panel: todos los de la barbería (en cualquier estado, para que
     * admin/barbero vean también cancelados y ausentes) que se solapan con [inicioRango, finRango),
     * opcionalmente acotados a un barbero puntual.
     */
    @Query("""
            select a from Atencion a
            where a.negocio.id = :negocioId
              and (:profesionalId is null or a.profesional.id = :profesionalId)
              and a.inicio < :finRango
              and a.fin > :inicioRango
            order by a.inicio asc
            """)
    List<Atencion> findParaAgenda(
            @Param("negocioId") UUID negocioId,
            @Param("profesionalId") UUID profesionalId,
            @Param("inicioRango") Instant inicioRango,
            @Param("finRango") Instant finRango);

    /** Historial de un cliente (cualquier estado), de la más reciente a la más antigua. */
    @Query("""
            select a from Atencion a
            join fetch a.profesional
            join fetch a.servicio
            where a.negocio.id = :negocioId
              and a.cliente.id = :clienteId
            order by a.inicio desc
            """)
    List<Atencion> findHistorialDeCliente(
            @Param("negocioId") UUID negocioId,
            @Param("clienteId") UUID clienteId);
}
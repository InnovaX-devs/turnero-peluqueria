package com.innovax.barberia.atencion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AtencionRepository extends JpaRepository<Atencion, UUID> {

    /** Atenciones del barbero, en alguno de los estados dados, que se solapan con [inicioRango, finRango). */
    @Query("""
            select a from Atencion a
            where a.barbero.id = :barberoId
              and a.estado in :estados
              and a.inicio < :finRango
              and a.fin > :inicioRango
            """)
    List<Atencion> findOcupadas(
            @Param("barberoId") UUID barberoId,
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
            where a.barberia.id = :barberiaId
              and (:barberoId is null or a.barbero.id = :barberoId)
              and a.inicio < :finRango
              and a.fin > :inicioRango
            order by a.inicio asc
            """)
    List<Atencion> findParaAgenda(
            @Param("barberiaId") UUID barberiaId,
            @Param("barberoId") UUID barberoId,
            @Param("inicioRango") Instant inicioRango,
            @Param("finRango") Instant finRango);

    /** Historial de un cliente (cualquier estado), de la más reciente a la más antigua. */
    @Query("""
            select a from Atencion a
            join fetch a.barbero
            join fetch a.servicio
            where a.barberia.id = :barberiaId
              and a.cliente.id = :clienteId
            order by a.inicio desc
            """)
    List<Atencion> findHistorialDeCliente(
            @Param("barberiaId") UUID barberiaId,
            @Param("clienteId") UUID clienteId);
}
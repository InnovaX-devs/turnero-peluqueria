package com.innovax.barberia.horario;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BloqueoRepository extends JpaRepository<Bloqueo, UUID> {

    /** Bloqueos del barbero que se solapan con [inicioRango, finRango). */
    @Query("""
            select b from Bloqueo b
            where b.profesional.id = :profesionalId
              and b.inicio < :finRango
              and b.fin > :inicioRango
            """)
    List<Bloqueo> findSolapados(
            @Param("profesionalId") UUID profesionalId,
            @Param("inicioRango") Instant inicioRango,
            @Param("finRango") Instant finRango);

    /**
     * Bloqueos de la agenda del panel: todos los de la barbería que se solapan con
     * [inicioRango, finRango), opcionalmente acotados a un barbero puntual.
     */
    @Query("""
            select b from Bloqueo b
            where b.profesional.negocio.id = :negocioId
              and (:profesionalId is null or b.profesional.id = :profesionalId)
              and b.inicio < :finRango
              and b.fin > :inicioRango
            order by b.inicio asc
            """)
    List<Bloqueo> findParaAgenda(
            @Param("negocioId") UUID negocioId,
            @Param("profesionalId") UUID profesionalId,
            @Param("inicioRango") Instant inicioRango,
            @Param("finRango") Instant finRango);
}
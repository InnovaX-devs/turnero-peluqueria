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
            where b.barbero.id = :barberoId
              and b.inicio < :finRango
              and b.fin > :inicioRango
            """)
    List<Bloqueo> findSolapados(
            @Param("barberoId") UUID barberoId,
            @Param("inicioRango") Instant inicioRango,
            @Param("finRango") Instant finRango);
}
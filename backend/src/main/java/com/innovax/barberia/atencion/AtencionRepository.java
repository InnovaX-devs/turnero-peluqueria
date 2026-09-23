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
}
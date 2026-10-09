package com.innovax.barberia.reporte;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.EstadoAtencion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReporteRepository extends Repository<Atencion, UUID> {

    /**
     * Todas las atenciones (cualquier estado) de una barbería en [desde, hasta).
     * Con :barberoId, solo las de ese barbero (null = todos, solo lo usa el admin).
     */
    @Query("""
            select new com.innovax.barberia.reporte.FilaReporte(
                a.inicio, a.estado, a.origen, a.precioFinal,
                b.id, b.nombre, b.apellido,
                c.id, c.nombre, c.telefono)
            from Atencion a
            join a.barbero b
            left join a.cliente c
            where a.barberia.id = :barberiaId
              and a.inicio >= :desde
              and a.inicio < :hasta
              and (:sucursalId is null or a.sucursal.id = :sucursalId)
              and (:barberoId is null or b.id = :barberoId)
            """)
    List<FilaReporte> buscar(
            @Param("barberiaId") UUID barberiaId,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta,
            @Param("sucursalId") UUID sucursalId,
            @Param("barberoId") UUID barberoId);

    /** Clientes cuya última atención completada fue antes de :limite. Los que más vinieron, primero. */
    @Query("""
            select new com.innovax.barberia.reporte.UltimaVisita(
                c.id, c.nombre, c.telefono, max(a.inicio), count(a))
            from Atencion a
            join a.cliente c
            where a.barberia.id = :barberiaId
              and a.estado = :completada
              and (:barberoId is null or a.barbero.id = :barberoId)
            group by c.id, c.nombre, c.telefono
            having max(a.inicio) < :limite
            order by count(a) desc, max(a.inicio) desc
            """)
    List<UltimaVisita> ultimasVisitasAntesDe(
            @Param("barberiaId") UUID barberiaId,
            @Param("completada") EstadoAtencion completada,
            @Param("limite") Instant limite,
            @Param("barberoId") UUID barberoId);

    /** Clientes que ya tienen un turno reservado a futuro (esos van a volver). */
    @Query("""
            select distinct a.cliente.id
            from Atencion a
            where a.barberia.id = :barberiaId
              and a.estado = :reservada
              and a.inicio > :ahora
              and a.cliente is not null
              and (:barberoId is null or a.barbero.id = :barberoId)
            """)
    List<UUID> clientesConTurnoFuturo(
            @Param("barberiaId") UUID barberiaId,
            @Param("reservada") EstadoAtencion reservada,
            @Param("ahora") Instant ahora,
            @Param("barberoId") UUID barberoId);
}
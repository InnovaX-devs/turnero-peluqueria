package com.innovax.barberia.caja;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.EstadoAtencion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CajaRepository extends Repository<Atencion, UUID> {

    /** Atenciones en un estado dado (la caja usa COMPLETADA) de una barbería, en [desde, hasta). */
    @Query("""
            select new com.innovax.barberia.caja.FilaCaja(
                a.inicio, a.precioFinal, a.metodoPago,
                b.id, b.nombre, b.apellido,
                s.id, s.nombre,
                c.nombre)
            from Atencion a
            join a.barbero b
            join a.servicio s
            left join a.cliente c
            where a.barberia.id = :barberiaId
              and a.estado = :estado
              and a.inicio >= :desde
              and a.inicio < :hasta
              and (:sucursalId is null or a.sucursal.id = :sucursalId)
            order by a.inicio
            """)
    List<FilaCaja> buscar(
            @Param("barberiaId") UUID barberiaId,
            @Param("estado") EstadoAtencion estado,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta,
            @Param("sucursalId") UUID sucursalId);
}

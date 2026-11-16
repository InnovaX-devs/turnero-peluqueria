package com.innovax.barberia.cobertura;

import com.innovax.barberia.atencion.Atencion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReporteCoberturaRepository extends Repository<Atencion, UUID> {

    /** Atenciones COMPLETADAS con cobertura del negocio en [desde, hasta), opcionalmente de una sola cobertura. */
    @Query("""
            select new com.innovax.barberia.cobertura.FilaCobertura(
                a.id, a.inicio, co.id, co.nombre, a.tipoPago, a.precioFinal, a.copago, a.numeroAutorizacion,
                c.nombre, c.dni, c.numeroAfiliado, c.plan,
                p.nombre, p.apellido, p.matricula,
                s.nombre)
            from Atencion a
            join a.cobertura co
            join a.profesional p
            join a.servicio s
            left join a.cliente c
            where a.negocio.id = :negocioId
              and a.estado = com.innovax.barberia.atencion.EstadoAtencion.COMPLETADA
              and a.inicio >= :desde
              and a.inicio < :hasta
              and (:coberturaId is null or co.id = :coberturaId)
            order by co.nombre, a.inicio
            """)
    List<FilaCobertura> buscar(
            @Param("negocioId") UUID negocioId,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta,
            @Param("coberturaId") UUID coberturaId);
}

package com.innovax.barberia.notificacion;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.EstadoAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {

    /** Lo que el job tiene que procesar: pendientes cuya hora ya llegó. */
    List<Notificacion> findByEstadoAndProgramadaParaLessThanEqual(EstadoNotificacion estado, Instant hasta);

    boolean existsByAtencionIdAndTipo(UUID atencionId, TipoNotificacion tipo);

    List<Notificacion> findByAtencionIdOrderByCreatedAtAsc(UUID atencionId);

    /**
     * Turnos con cliente, en el estado dado, que empiezan entre {@code desde} y {@code hasta}
     * y todavía no tienen una notificación de ese tipo.
     */
    @Query("""
            select a from Atencion a
            where a.estado = :estado
              and a.cliente is not null
              and a.inicio > :desde
              and a.inicio <= :hasta
              and not exists (
                  select n.id from Notificacion n
                  where n.atencion = a and n.tipo = :tipo)
            order by a.inicio asc
            """)
    List<Atencion> findSinNotificacion(
            @Param("estado") EstadoAtencion estado,
            @Param("tipo") TipoNotificacion tipo,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta);
}
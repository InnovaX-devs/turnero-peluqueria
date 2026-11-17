package com.innovax.barberia.notificacion;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.EstadoAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {

    /** Issue #73: los mensajes que todavía no salieron a un paciente anonimizado ya no se mandan. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Notificacion n
            set n.estado = com.innovax.barberia.notificacion.EstadoNotificacion.OMITIDA,
                n.error = 'Paciente anonimizado'
            where n.estado = com.innovax.barberia.notificacion.EstadoNotificacion.PENDIENTE
              and n.atencion.id in (select a.id from Atencion a where a.cliente.id = :clienteId)
            """)
    int omitirPendientesDeCliente(@Param("clienteId") UUID clienteId);

    /** Issue #73: el mensaje y el destinatario tienen el nombre y el teléfono del paciente. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Notificacion n
            set n.destinatario = 'anonimizado',
                n.mensaje = '(contenido eliminado a pedido del titular)'
            where n.atencion.id in (select a.id from Atencion a where a.cliente.id = :clienteId)
            """)
    int borrarContenidoDeCliente(@Param("clienteId") UUID clienteId);

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

    /** Mensajes de una barbería para el panel, opcionalmente acotados a un barbero (null = todos). */
    @Query("""
            select n from Notificacion n
            where n.negocio.id = :negocioId
              and n.estado = :estado
              and (:profesionalId is null or n.atencion.profesional.id = :profesionalId)
            order by n.programadaPara asc
            """)
    List<Notificacion> findParaPanel(
            @Param("negocioId") UUID negocioId,
            @Param("estado") EstadoNotificacion estado,
            @Param("profesionalId") UUID profesionalId);
}
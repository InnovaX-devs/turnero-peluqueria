package com.innovax.barberia.notificacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {

    /** Lo que el job tiene que procesar: pendientes cuya hora ya llegó. */
    List<Notificacion> findByEstadoAndProgramadaParaLessThanEqual(EstadoNotificacion estado, Instant hasta);

    boolean existsByAtencionIdAndTipo(UUID atencionId, TipoNotificacion tipo);

    List<Notificacion> findByAtencionIdOrderByCreatedAtAsc(UUID atencionId);
}
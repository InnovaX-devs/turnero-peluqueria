package com.innovax.barberia.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, UUID> {

    /** Con lock: si llegan dos pedidos con el mismo token a la vez, el segundo ve el token ya usado. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    /** Para no mandar mails en ráfaga si alguien aprieta el botón muchas veces. */
    boolean existsByUsuarioIdAndCreatedAtAfter(UUID usuarioId, Instant desde);

    /** Deja sin efecto los links pendientes del usuario (al pedir uno nuevo o al cambiar la contraseña). */
    @Modifying(flushAutomatically = true)
    @Query("""
            update TokenRecuperacion t set t.usadoEn = :ahora
            where t.usuario.id = :usuarioId and t.usadoEn is null
            """)
    int invalidarPendientes(@Param("usuarioId") UUID usuarioId, @Param("ahora") Instant ahora);
}
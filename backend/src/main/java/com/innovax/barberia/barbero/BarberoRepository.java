package com.innovax.barberia.barbero;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BarberoRepository extends JpaRepository<Barbero, UUID> {

    Optional<Barbero> findByUsuarioId(UUID usuarioId);
}

package com.innovax.barberia.negocio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NegocioRepository extends JpaRepository<Negocio, UUID> {

    boolean existsBySlug(String slug);

    Optional<Negocio> findBySlug(String slug);
}
    
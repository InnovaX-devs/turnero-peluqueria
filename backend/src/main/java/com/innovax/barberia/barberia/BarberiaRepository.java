package com.innovax.barberia.barberia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BarberiaRepository extends JpaRepository<Barberia, UUID> {

    boolean existsBySlug(String slug);

    Optional<Barberia> findBySlug(String slug);
}
    
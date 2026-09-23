package com.innovax.barberia.servicio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServicioRepository extends JpaRepository<Servicio, UUID> {
}
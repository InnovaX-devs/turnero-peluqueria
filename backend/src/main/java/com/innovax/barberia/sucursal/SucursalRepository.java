package com.innovax.barberia.sucursal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SucursalRepository extends JpaRepository<Sucursal, UUID> {
}
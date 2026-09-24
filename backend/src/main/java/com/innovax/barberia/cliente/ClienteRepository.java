package com.innovax.barberia.cliente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    /** Un cliente se identifica por su teléfono dentro de la barbería (ver uq_cliente_telefono). */
    Optional<Cliente> findByBarberiaIdAndTelefono(UUID barberiaId, String telefono);
}
package com.innovax.barberia.barbero;

import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.sucursal.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BarberoRepository extends JpaRepository<Barbero, UUID> {

    Optional<Barbero> findByUsuarioId(UUID usuarioId);

    /** Para "cualquier barbero": todos los que están activos, trabajan en la sucursal y ofrecen el servicio. */
    List<Barbero> findByActivoTrueAndSucursalesContainingAndServiciosContaining(Sucursal sucursal, Servicio servicio);
}

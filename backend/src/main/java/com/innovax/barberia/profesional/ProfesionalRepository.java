package com.innovax.barberia.profesional;

import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.sucursal.Sucursal;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfesionalRepository extends JpaRepository<Profesional, UUID> {

    Optional<Profesional> findByUsuarioId(UUID usuarioId);

    /** Para "cualquier barbero": todos los que están activos, trabajan en la sucursal y ofrecen el servicio. */
    List<Profesional> findByActivoTrueAndSucursalesContainingAndServiciosContaining(Sucursal sucursal, Servicio servicio);

    /** Lista para el selector de barbero de la agenda del panel. */
    List<Profesional> findByNegocioIdAndActivoTrueOrderByNombreAscApellidoAsc(UUID negocioId);

    @EntityGraph(attributePaths = {"sucursales", "servicios", "usuario"})
    List<Profesional> findByNegocioIdOrderByNombreAscApellidoAsc(UUID negocioId);

    @EntityGraph(attributePaths = {"sucursales", "servicios", "usuario"})
    Optional<Profesional> findByIdAndNegocioId(UUID id, UUID negocioId);
}

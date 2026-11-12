package com.innovax.barberia.profesional;

import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.sucursal.Sucursal;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfesionalRepository extends JpaRepository<Profesional, UUID> {

    Optional<Profesional> findByUsuarioId(UUID usuarioId);

    /** Para "cualquier barbero": todos los que están activos, trabajan en la sucursal y ofrecen el servicio. */
    List<Profesional> findByActivoTrueAndSucursalesContainingAndServiciosContaining(Sucursal sucursal, Servicio servicio);

    /** Rubro salud, "elegí la especialidad → profesional": activos de la sucursal que tienen esa especialidad. */
    @Query("""
            select distinct p from Profesional p
            join p.sucursales su
            join p.especialidades e
            where p.activo = true
              and su.id = :sucursalId
              and e.id = :especialidadId
            """)
    List<Profesional> findActivosDeSucursalConEspecialidad(
            @Param("sucursalId") UUID sucursalId,
            @Param("especialidadId") UUID especialidadId);

    /** Lista para el selector de barbero de la agenda del panel. */
    List<Profesional> findByNegocioIdAndActivoTrueOrderByNombreAscApellidoAsc(UUID negocioId);

    @EntityGraph(attributePaths = {"sucursales", "servicios", "especialidades", "usuario"})
    List<Profesional> findByNegocioIdOrderByNombreAscApellidoAsc(UUID negocioId);

    @EntityGraph(attributePaths = {"sucursales", "servicios", "especialidades", "usuario"})
    Optional<Profesional> findByIdAndNegocioId(UUID id, UUID negocioId);
}

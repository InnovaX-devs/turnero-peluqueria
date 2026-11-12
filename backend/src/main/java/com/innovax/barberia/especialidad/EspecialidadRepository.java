package com.innovax.barberia.especialidad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EspecialidadRepository extends JpaRepository<Especialidad, UUID> {

    List<Especialidad> findByNegocioIdOrderByNombreAsc(UUID negocioId);

    Optional<Especialidad> findByIdAndNegocioId(UUID id, UUID negocioId);

    boolean existsByNegocioId(UUID negocioId);

    boolean existsByNegocioIdAndNombreIgnoreCase(UUID negocioId, String nombre);

    boolean existsByNegocioIdAndNombreIgnoreCaseAndIdNot(UUID negocioId, String nombre, UUID id);

    /**
     * Especialidades que se pueden reservar en la sucursal: las tiene al menos un profesional activo
     * que trabaja ahí y hace algún servicio activo. Las demás no se muestran en la página pública.
     */
    @Query("""
            select distinct e from Profesional p
            join p.especialidades e
            join p.sucursales su
            join p.servicios sv
            where e.negocio.id = :negocioId
              and p.activo = true
              and su.id = :sucursalId
              and su.activa = true
              and sv.activo = true
            order by e.nombre asc
            """)
    List<Especialidad> findReservablesEnSucursal(
            @Param("negocioId") UUID negocioId,
            @Param("sucursalId") UUID sucursalId);
}

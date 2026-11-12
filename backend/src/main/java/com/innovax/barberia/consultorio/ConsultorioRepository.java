package com.innovax.barberia.consultorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultorioRepository extends JpaRepository<Consultorio, UUID> {

    /** Los consultorios de todas las sucursales del negocio (activas o no), ordenados para mostrar. */
    @Query("""
            select c from Consultorio c
            join fetch c.sucursal s
            where s.negocio.id = :negocioId
            order by s.nombre asc, c.nombre asc
            """)
    List<Consultorio> findDelNegocio(@Param("negocioId") UUID negocioId);

    @Query("""
            select c from Consultorio c
            join fetch c.sucursal s
            where c.id = :id
              and s.negocio.id = :negocioId
            """)
    Optional<Consultorio> findByIdAndNegocioId(@Param("id") UUID id, @Param("negocioId") UUID negocioId);

    boolean existsBySucursalIdAndNombreIgnoreCase(UUID sucursalId, String nombre);

    boolean existsBySucursalIdAndNombreIgnoreCaseAndIdNot(UUID sucursalId, String nombre, UUID id);
}

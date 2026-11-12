package com.innovax.barberia.servicio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface ServicioRepository extends JpaRepository<Servicio, UUID> {

    List<Servicio> findByNegocioIdAndActivoTrueOrderByOrdenAscNombreAsc(UUID negocioId);

    List<Servicio> findByNegocioIdOrderByOrdenAscNombreAsc(UUID negocioId);

    Optional<Servicio> findByIdAndNegocioId(UUID id, UUID negocioId);

    boolean existsByNegocioIdAndActivoTrueAndNombreIgnoreCase(UUID negocioId, String nombre);

    boolean existsByNegocioIdAndActivoTrueAndNombreIgnoreCaseAndIdNot(UUID negocioId, String nombre, UUID id);

    @Query("select coalesce(max(s.orden), 0) from Servicio s where s.negocio.id = :negocioId")
    int maxOrden(@Param("negocioId") UUID negocioId);

    /** Servicios activos que hace al menos un barbero activo de esa sucursal (los demás no se pueden reservar ahí). */
    @Query("""
            select s from Servicio s
            where s.negocio.id = :negocioId
              and s.activo = true
              and exists (
                  select b.id from Profesional b
                  join b.sucursales su
                  join b.servicios sv
                  where b.activo = true
                    and su.id = :sucursalId
                    and sv.id = s.id)
            order by s.orden asc, s.nombre asc
            """)
    List<Servicio> findOfrecidosEnSucursal(
            @Param("negocioId") UUID negocioId,
            @Param("sucursalId") UUID sucursalId);

    /** Como {@link #findOfrecidosEnSucursal}, pero solo contando profesionales de esa especialidad (rubro salud). */
    @Query("""
            select s from Servicio s
            where s.negocio.id = :negocioId
              and s.activo = true
              and exists (
                  select b.id from Profesional b
                  join b.sucursales su
                  join b.servicios sv
                  join b.especialidades e
                  where b.activo = true
                    and su.id = :sucursalId
                    and sv.id = s.id
                    and e.id = :especialidadId)
            order by s.orden asc, s.nombre asc
            """)
    List<Servicio> findOfrecidosEnSucursalPorEspecialidad(
            @Param("negocioId") UUID negocioId,
            @Param("sucursalId") UUID sucursalId,
            @Param("especialidadId") UUID especialidadId);
}

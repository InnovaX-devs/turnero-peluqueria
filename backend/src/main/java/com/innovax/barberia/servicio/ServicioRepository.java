package com.innovax.barberia.servicio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface ServicioRepository extends JpaRepository<Servicio, UUID> {

    List<Servicio> findByBarberiaIdAndActivoTrueOrderByOrdenAscNombreAsc(UUID barberiaId);

    List<Servicio> findByBarberiaIdOrderByOrdenAscNombreAsc(UUID barberiaId);

    Optional<Servicio> findByIdAndBarberiaId(UUID id, UUID barberiaId);

    boolean existsByBarberiaIdAndActivoTrueAndNombreIgnoreCase(UUID barberiaId, String nombre);

    boolean existsByBarberiaIdAndActivoTrueAndNombreIgnoreCaseAndIdNot(UUID barberiaId, String nombre, UUID id);

    @Query("select coalesce(max(s.orden), 0) from Servicio s where s.barberia.id = :barberiaId")
    int maxOrden(@Param("barberiaId") UUID barberiaId);

    /** Servicios activos que hace al menos un barbero activo de esa sucursal (los demás no se pueden reservar ahí). */
    @Query("""
            select s from Servicio s
            where s.barberia.id = :barberiaId
              and s.activo = true
              and exists (
                  select b.id from Barbero b
                  join b.sucursales su
                  join b.servicios sv
                  where b.activo = true
                    and su.id = :sucursalId
                    and sv.id = s.id)
            order by s.orden asc, s.nombre asc
            """)
    List<Servicio> findOfrecidosEnSucursal(
            @Param("barberiaId") UUID barberiaId,
            @Param("sucursalId") UUID sucursalId);

}

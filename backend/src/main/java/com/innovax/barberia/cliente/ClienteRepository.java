package com.innovax.barberia.cliente;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    Optional<Cliente> findByBarberiaIdAndTelefono(UUID barberiaId, String telefono);

    /** Búsqueda por nombre (cuando lo tipeado no tiene números). */
    List<Cliente> findTop10ByBarberiaIdAndNombreContainingIgnoreCaseOrderByNombreAsc(UUID barberiaId, String nombre);

    /** Búsqueda por nombre o por parte del teléfono (ej. "4111" encuentra +5493584111111). */
    @Query("""
            select c from Cliente c
            where c.barberia.id = :barberiaId
              and (lower(c.nombre) like lower(concat('%', :texto, '%'))
                   or c.telefono like concat('%', :digitos, '%'))
            order by c.nombre
            """)
    List<Cliente> buscarPorNombreOTelefono(
            @Param("barberiaId") UUID barberiaId,
            @Param("texto") String texto,
            @Param("digitos") String digitos,
            Pageable pagina);
}
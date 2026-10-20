package com.innovax.barberia.cliente;

import com.innovax.barberia.atencion.EstadoAtencion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    Optional<Cliente> findByIdAndBarberiaId(UUID id, UUID barberiaId);

    // ---------- Listado del panel, paginado ----------
    // Filtro común: sin texto trae todos; con texto busca en el nombre o (si hay 3+ dígitos) en el teléfono.
    // Son consultas nativas porque el orden por última visita necesita un LEFT JOIN a un agregado.

    String FILTRO_LISTADO = """
            c.barberia_id = :barberiaId
              AND (:texto = ''
                   OR lower(c.nombre) LIKE '%' || lower(:texto) || '%'
                   OR (:digitos <> '' AND c.telefono LIKE '%' || :digitos || '%'))
            """;

    @Query(value = "SELECT count(*) FROM cliente c WHERE " + FILTRO_LISTADO, nativeQuery = true)
    long contarListado(@Param("barberiaId") UUID barberiaId,
                       @Param("texto") String texto,
                       @Param("digitos") String digitos);

    @Query(value = "SELECT c.* FROM cliente c WHERE " + FILTRO_LISTADO + """
             ORDER BY lower(c.nombre), c.id
             LIMIT :limite OFFSET :desde
            """, nativeQuery = true)
    List<Cliente> listarPorNombre(@Param("barberiaId") UUID barberiaId,
                                  @Param("texto") String texto,
                                  @Param("digitos") String digitos,
                                  @Param("limite") int limite,
                                  @Param("desde") long desde);

    /** Los que vinieron más recientemente primero; los que nunca vinieron, al final. */
    @Query(value = """
            SELECT c.* FROM cliente c
            LEFT JOIN (SELECT a.cliente_id, max(a.inicio) AS ultima
                       FROM atencion a
                       WHERE a.barberia_id = :barberiaId AND a.estado = 'COMPLETADA' AND a.cliente_id IS NOT NULL
                       GROUP BY a.cliente_id) u ON u.cliente_id = c.id
            WHERE\s""" + FILTRO_LISTADO + """
             ORDER BY u.ultima DESC NULLS LAST, lower(c.nombre), c.id
             LIMIT :limite OFFSET :desde
            """, nativeQuery = true)
    List<Cliente> listarPorUltimaVisita(@Param("barberiaId") UUID barberiaId,
                                        @Param("texto") String texto,
                                        @Param("digitos") String digitos,
                                        @Param("limite") int limite,
                                        @Param("desde") long desde);

    /** Última atención completada de cada cliente de la lista: [clienteId, inicio]. */
    @Query("""
            select a.cliente.id, max(a.inicio) from Atencion a
            where a.cliente.id in :ids and a.estado = :estado
            group by a.cliente.id
            """)
    List<Object[]> ultimasVisitas(@Param("ids") Collection<UUID> ids, @Param("estado") EstadoAtencion estado);
}
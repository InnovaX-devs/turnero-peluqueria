package com.innovax.barberia.listaespera;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ListaEsperaRepository extends JpaRepository<ListaEspera, UUID> {

    /** Issue #73: la nota de la lista de espera es texto libre sobre el paciente. */
    @Modifying(flushAutomatically = true)
    @Query("update ListaEspera l set l.nota = null, l.avisoDetalle = null where l.cliente.id = :clienteId")
    int borrarNotasDeCliente(@Param("clienteId") UUID clienteId);

    /** Los que siguen esperando ese día, en orden de anotación. */
    @Query("""
            SELECT l FROM ListaEspera l
            JOIN FETCH l.cliente
            LEFT JOIN FETCH l.profesional
            WHERE l.negocio.id = :negocioId
              AND l.fecha = :fecha
              AND l.estado = com.innovax.barberia.listaespera.EstadoListaEspera.ESPERANDO
            ORDER BY l.createdAt ASC
            """)
    List<ListaEspera> findEsperando(@Param("negocioId") UUID negocioId, @Param("fecha") LocalDate fecha);
}

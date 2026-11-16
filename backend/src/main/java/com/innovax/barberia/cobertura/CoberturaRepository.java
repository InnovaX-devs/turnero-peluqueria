package com.innovax.barberia.cobertura;

import com.innovax.barberia.common.RecursoNoEncontradoException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoberturaRepository extends JpaRepository<Cobertura, UUID> {

    List<Cobertura> findByNegocioIdOrderByNombreAsc(UUID negocioId);

    List<Cobertura> findByNegocioIdAndActivaTrueOrderByNombreAsc(UUID negocioId);

    Optional<Cobertura> findByIdAndNegocioId(UUID id, UUID negocioId);

    boolean existsByNegocioIdAndActivaTrueAndNombreIgnoreCase(UUID negocioId, String nombre);

    boolean existsByNegocioIdAndActivaTrueAndNombreIgnoreCaseAndIdNot(UUID negocioId, String nombre, UUID id);

    /**
     * Para los filtros de la página pública: null si no se eligió cobertura (particular); si se eligió,
     * tiene que ser una activa del negocio.
     */
    default Cobertura activaONull(UUID negocioId, UUID coberturaId) {
        if (coberturaId == null) {
            return null;
        }
        return findByIdAndNegocioId(coberturaId, negocioId)
                .filter(Cobertura::isActiva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra social / prepaga no encontrada"));
    }
}

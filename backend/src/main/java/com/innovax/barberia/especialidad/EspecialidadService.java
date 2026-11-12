package com.innovax.barberia.especialidad;

import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.especialidad.dto.EspecialidadRequest;
import com.innovax.barberia.especialidad.dto.EspecialidadResponse;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** ABM de especialidades del negocio del usuario logueado. Todo se filtra por el negocio del token. */
@Service
@RequiredArgsConstructor
public class EspecialidadService {

    private final EspecialidadRepository especialidades;
    private final NegocioRepository negocios;
    private final UsuarioActual usuarioActual;

    @Transactional(readOnly = true)
    public List<EspecialidadResponse> listar() {
        return especialidades.findByNegocioIdOrderByNombreAsc(usuarioActual.negocioId()).stream()
                .map(EspecialidadResponse::de)
                .toList();
    }

    @Transactional
    public EspecialidadResponse crear(EspecialidadRequest request) {
        UUID negocioId = usuarioActual.negocioId();
        String nombre = normalizar(request.nombre());
        if (especialidades.existsByNegocioIdAndNombreIgnoreCase(negocioId, nombre)) {
            throw repetida();
        }
        Negocio negocio = negocios.findById(negocioId)
                .orElseThrow(() -> new IllegalStateException("El negocio del token no existe"));
        Especialidad especialidad = new Especialidad();
        especialidad.setNegocio(negocio);
        especialidad.setNombre(nombre);
        return EspecialidadResponse.de(guardar(especialidad));
    }

    @Transactional
    public EspecialidadResponse renombrar(UUID id, EspecialidadRequest request) {
        Especialidad especialidad = buscar(id);
        String nombre = normalizar(request.nombre());
        if (especialidades.existsByNegocioIdAndNombreIgnoreCaseAndIdNot(usuarioActual.negocioId(), nombre, id)) {
            throw repetida();
        }
        especialidad.setNombre(nombre);
        return EspecialidadResponse.de(guardar(especialidad));
    }

    /** Se borra de verdad: la base la saca sola de los profesionales que la tenían (ON DELETE CASCADE). */
    @Transactional
    public void borrar(UUID id) {
        especialidades.delete(buscar(id));
    }

    private Especialidad buscar(UUID id) {
        return especialidades.findByIdAndNegocioId(id, usuarioActual.negocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada"));
    }

    /** El UNIQUE de la base cubre la carrera entre dos altas con el mismo nombre. */
    private Especialidad guardar(Especialidad especialidad) {
        try {
            return especialidades.saveAndFlush(especialidad);
        } catch (DataIntegrityViolationException e) {
            throw repetida();
        }
    }

    /** "  kinesiología   deportiva " → "kinesiología deportiva". */
    private static String normalizar(String nombre) {
        return nombre.trim().replaceAll("\\s+", " ");
    }

    private static ConflictoException repetida() {
        return new ConflictoException("nombre", "Ya existe una especialidad con ese nombre");
    }
}

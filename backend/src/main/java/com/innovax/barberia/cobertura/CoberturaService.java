package com.innovax.barberia.cobertura;

import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.cobertura.dto.CoberturaRequests;
import com.innovax.barberia.cobertura.dto.CoberturaResponse;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** ABM de coberturas del negocio del usuario logueado. Todo se filtra por el negocio del token. */
@Service
@RequiredArgsConstructor
public class CoberturaService {

    /** Lista inicial sugerida: el admin la carga con un botón y después edita/desactiva lo que no use. */
    static final List<String> COMUNES = List.of(
            "PAMI", "OSDE", "Swiss Medical", "Galeno", "Medifé", "Sancor Salud", "OMINT",
            "Prevención Salud", "IOSFA", "APROSS", "OSECAC", "Unión Personal");

    private final CoberturaRepository coberturas;
    private final NegocioRepository negocios;
    private final UsuarioActual usuarioActual;

    /** Todas (activas e inactivas), para la pantalla de ABM. */
    @Transactional(readOnly = true)
    public List<CoberturaResponse> listar(boolean soloActivas) {
        UUID negocioId = usuarioActual.negocioId();
        List<Cobertura> lista = soloActivas
                ? coberturas.findByNegocioIdAndActivaTrueOrderByNombreAsc(negocioId)
                : coberturas.findByNegocioIdOrderByNombreAsc(negocioId);
        return lista.stream().map(CoberturaResponse::de).toList();
    }

    @Transactional
    public CoberturaResponse crear(CoberturaRequests.Guardar request) {
        UUID negocioId = usuarioActual.negocioId();
        String nombre = normalizar(request.nombre());
        if (coberturas.existsByNegocioIdAndActivaTrueAndNombreIgnoreCase(negocioId, nombre)) {
            throw repetida();
        }
        Cobertura cobertura = new Cobertura();
        cobertura.setNegocio(negocio(negocioId));
        cobertura.setNombre(nombre);
        return CoberturaResponse.de(guardar(cobertura));
    }

    @Transactional
    public CoberturaResponse renombrar(UUID id, CoberturaRequests.Guardar request) {
        Cobertura cobertura = buscar(id);
        String nombre = normalizar(request.nombre());
        if (cobertura.isActiva()
                && coberturas.existsByNegocioIdAndActivaTrueAndNombreIgnoreCaseAndIdNot(usuarioActual.negocioId(), nombre, id)) {
            throw repetida();
        }
        cobertura.setNombre(nombre);
        return CoberturaResponse.de(guardar(cobertura));
    }

    /**
     * Baja lógica. Una cobertura inactiva no se ofrece en la reserva ni se puede asignar a pacientes o
     * profesionales, pero las atenciones viejas la siguen mostrando y aparece en el reporte.
     */
    @Transactional
    public CoberturaResponse cambiarActiva(UUID id, CoberturaRequests.CambiarActiva request) {
        Cobertura cobertura = buscar(id);
        if (request.activa() && !cobertura.isActiva()
                && coberturas.existsByNegocioIdAndActivaTrueAndNombreIgnoreCaseAndIdNot(
                        usuarioActual.negocioId(), cobertura.getNombre(), id)) {
            throw repetida();
        }
        cobertura.setActiva(request.activa());
        return CoberturaResponse.de(guardar(cobertura));
    }

    /** Carga las coberturas más comunes que el negocio todavía no tenga activas. Devuelve la lista completa. */
    @Transactional
    public List<CoberturaResponse> cargarComunes() {
        UUID negocioId = usuarioActual.negocioId();
        Negocio negocio = negocio(negocioId);
        for (String nombre : COMUNES) {
            if (!coberturas.existsByNegocioIdAndActivaTrueAndNombreIgnoreCase(negocioId, nombre)) {
                Cobertura c = new Cobertura();
                c.setNegocio(negocio);
                c.setNombre(nombre);
                coberturas.save(c);
            }
        }
        coberturas.flush();
        return listar(false);
    }

    /** Para los demás servicios: una cobertura activa del negocio, o 400 en el campo indicado. */
    @Transactional(readOnly = true)
    public Cobertura activaDelNegocio(UUID negocioId, UUID coberturaId, String campo) {
        return coberturas.findByIdAndNegocioId(coberturaId, negocioId)
                .filter(Cobertura::isActiva)
                .orElseThrow(() -> new DatosInvalidosException(campo, "Esa obra social / prepaga no está disponible"));
    }

    private Cobertura buscar(UUID id) {
        return coberturas.findByIdAndNegocioId(id, usuarioActual.negocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cobertura no encontrada"));
    }

    private Negocio negocio(UUID negocioId) {
        return negocios.findById(negocioId)
                .orElseThrow(() -> new IllegalStateException("El negocio del token no existe"));
    }

    /** El índice único parcial de la base cubre la carrera entre dos altas con el mismo nombre. */
    private Cobertura guardar(Cobertura cobertura) {
        try {
            return coberturas.saveAndFlush(cobertura);
        } catch (DataIntegrityViolationException e) {
            throw repetida();
        }
    }

    private static String normalizar(String nombre) {
        return nombre.trim().replaceAll("\\s+", " ");
    }

    private static ConflictoException repetida() {
        return new ConflictoException("nombre", "Ya existe una obra social / prepaga activa con ese nombre");
    }
}

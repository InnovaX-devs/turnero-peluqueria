package com.innovax.barberia.servicio;

import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.servicio.dto.ServicioRequests;
import com.innovax.barberia.servicio.dto.ServicioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ABM de servicios. Todo se filtra por la barbería del token, nunca por el request.
 * Cambiar el precio o la duración NO afecta atenciones ya registradas: cada Atencion guarda su
 * propio precioFinal, inicio y fin.
 */
@Service
@RequiredArgsConstructor
public class ServicioService {

    private static final String NOMBRE_EN_USO = "Ya tenés un servicio activo con ese nombre";

    private final ServicioRepository servicios;
    private final BarberiaRepository barberias;
    private final UsuarioActual usuarioActual;

    @Transactional(readOnly = true)
    public List<ServicioResponse> listar(boolean incluirInactivos) {
        return servicios.findByBarberiaIdOrderByOrdenAscNombreAsc(usuarioActual.barberiaId()).stream()
                .filter(s -> incluirInactivos || s.isActivo())
                .map(ServicioResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServicioResponse obtener(UUID id) {
        return ServicioResponse.de(buscar(id));
    }

    @Transactional
    public ServicioResponse crear(ServicioRequests.Crear request) {
        UUID barberiaId = usuarioActual.barberiaId();
        String nombre = request.nombre().trim();
        if (servicios.existsByBarberiaIdAndActivoTrueAndNombreIgnoreCase(barberiaId, nombre)) {
            throw new ConflictoException("nombre", NOMBRE_EN_USO);
        }

        Servicio servicio = new Servicio();
        servicio.setBarberia(barberias.getReferenceById(barberiaId));
        servicio.setNombre(nombre);
        servicio.setDuracionMin(request.duracionMin());
        servicio.setPrecio(request.precio());
        servicio.setActivo(true);
        servicio.setOrden(servicios.maxOrden(barberiaId) + 1);
        return guardar(servicio);
    }

    @Transactional
    public ServicioResponse actualizar(UUID id, ServicioRequests.Actualizar request) {
        Servicio servicio = buscar(id);
        String nombre = request.nombre().trim();
        if (servicio.isActivo() && servicios.existsByBarberiaIdAndActivoTrueAndNombreIgnoreCaseAndIdNot(
                servicio.getBarberia().getId(), nombre, id)) {
            throw new ConflictoException("nombre", NOMBRE_EN_USO);
        }
        servicio.setNombre(nombre);
        servicio.setDuracionMin(request.duracionMin());
        servicio.setPrecio(request.precio());
        return guardar(servicio);
    }

    /** Baja lógica (activo=false) o reactivación (activo=true, queda al final de la lista). */
    @Transactional
    public ServicioResponse cambiarActivo(UUID id, boolean activo) {
        Servicio servicio = buscar(id);
        if (servicio.isActivo() == activo) {
            return ServicioResponse.de(servicio);
        }
        if (activo) {
            UUID barberiaId = servicio.getBarberia().getId();
            if (servicios.existsByBarberiaIdAndActivoTrueAndNombreIgnoreCase(barberiaId, servicio.getNombre())) {
                throw new ConflictoException("nombre", NOMBRE_EN_USO);
            }
            servicio.setOrden(servicios.maxOrden(barberiaId) + 1);
        }
        servicio.setActivo(activo);
        return guardar(servicio);
    }

    /** La lista tiene que traer cada servicio activo una sola vez. Guarda orden = posición (desde 1). */
    @Transactional
    public List<ServicioResponse> reordenar(ServicioRequests.Reordenar request) {
        UUID barberiaId = usuarioActual.barberiaId();
        Map<UUID, Servicio> activos = servicios.findByBarberiaIdAndActivoTrueOrderByOrdenAscNombreAsc(barberiaId)
                .stream().collect(Collectors.toMap(Servicio::getId, Function.identity()));

        List<UUID> ids = request.ids();
        boolean sinRepetidos = new HashSet<>(ids).size() == ids.size();
        if (!sinRepetidos || ids.size() != activos.size() || !activos.keySet().containsAll(ids)) {
            throw new DatosInvalidosException("ids", "La lista tiene que incluir cada servicio activo una sola vez");
        }

        for (int i = 0; i < ids.size(); i++) {
            activos.get(ids.get(i)).setOrden(i + 1);
        }
        servicios.flush();
        return listar(true);
    }

    // ------------------------------------------------------------------ helpers

    private Servicio buscar(UUID id) {
        return servicios.findByIdAndBarberiaId(id, usuarioActual.barberiaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
    }

    /** El índice único parcial cubre la carrera entre dos requests con el mismo nombre. */
    private ServicioResponse guardar(Servicio servicio) {
        try {
            return ServicioResponse.de(servicios.saveAndFlush(servicio));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictoException("nombre", NOMBRE_EN_USO);
        }
    }
}
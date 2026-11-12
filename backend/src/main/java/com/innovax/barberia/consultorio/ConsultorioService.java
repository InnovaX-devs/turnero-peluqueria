package com.innovax.barberia.consultorio;

import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.consultorio.dto.ConsultorioRequests;
import com.innovax.barberia.consultorio.dto.ConsultorioResponse;
import com.innovax.barberia.horario.HorarioLaboral;
import com.innovax.barberia.horario.HorarioLaboralRepository;
import com.innovax.barberia.sucursal.Sucursal;
import com.innovax.barberia.sucursal.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ABM de consultorios del negocio del usuario logueado (issue #69). Todo se filtra por el negocio del token.
 * Se asignan a los tramos del horario de cada profesional.
 */
@Service
@RequiredArgsConstructor
public class ConsultorioService {

    private final ConsultorioRepository consultorios;
    private final SucursalRepository sucursales;
    private final HorarioLaboralRepository horarios;
    private final UsuarioActual usuarioActual;

    @Transactional(readOnly = true)
    public List<ConsultorioResponse> listar(boolean incluirInactivos) {
        return consultorios.findDelNegocio(usuarioActual.negocioId()).stream()
                .filter(c -> incluirInactivos || c.isActivo())
                .map(ConsultorioResponse::de)
                .toList();
    }

    @Transactional
    public ConsultorioResponse crear(ConsultorioRequests.Crear request) {
        Sucursal sucursal = sucursales.findById(request.sucursalId())
                .filter(s -> s.getNegocio().getId().equals(usuarioActual.negocioId()))
                .filter(Sucursal::isActiva)
                .orElseThrow(() -> new DatosInvalidosException("sucursalId", "La sucursal no existe o está inactiva"));
        String nombre = normalizar(request.nombre());
        if (consultorios.existsBySucursalIdAndNombreIgnoreCase(sucursal.getId(), nombre)) {
            throw repetido();
        }
        Consultorio consultorio = new Consultorio();
        consultorio.setSucursal(sucursal);
        consultorio.setNombre(nombre);
        return ConsultorioResponse.de(guardar(consultorio));
    }

    @Transactional
    public ConsultorioResponse renombrar(UUID id, ConsultorioRequests.Renombrar request) {
        Consultorio consultorio = buscar(id);
        String nombre = normalizar(request.nombre());
        if (consultorios.existsBySucursalIdAndNombreIgnoreCaseAndIdNot(consultorio.getSucursal().getId(), nombre, id)) {
            throw repetido();
        }
        consultorio.setNombre(nombre);
        return ConsultorioResponse.de(guardar(consultorio));
    }

    /**
     * Baja lógica o reactivación. No se puede desactivar un consultorio que sigue asignado en algún
     * horario: primero hay que sacarlo de esos horarios (si no, quedarían horarios apuntando a un
     * consultorio que nadie ve). Los turnos ya dados no se tocan.
     */
    @Transactional
    public ConsultorioResponse cambiarActivo(UUID id, boolean activo) {
        Consultorio consultorio = buscar(id);
        if (!activo && consultorio.isActivo()) {
            List<HorarioLaboral> enUso = horarios.findByConsultorioId(id);
            if (!enUso.isEmpty()) {
                String quienes = enUso.stream()
                        .map(h -> h.getProfesional().getNombre() + " " + h.getProfesional().getApellido())
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining(", "));
                throw new ConflictoException("activo",
                        "Está asignado en el horario de " + quienes + ". Sacalo de esos horarios antes de desactivarlo.");
            }
        }
        consultorio.setActivo(activo);
        return ConsultorioResponse.de(consultorio);
    }

    private Consultorio buscar(UUID id) {
        return consultorios.findByIdAndNegocioId(id, usuarioActual.negocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Consultorio no encontrado"));
    }

    /** El UNIQUE de la base cubre la carrera entre dos altas con el mismo nombre. */
    private Consultorio guardar(Consultorio consultorio) {
        try {
            return consultorios.saveAndFlush(consultorio);
        } catch (DataIntegrityViolationException e) {
            throw repetido();
        }
    }

    /** "  consultorio   2 " → "consultorio 2". */
    private static String normalizar(String nombre) {
        return nombre.trim().replaceAll("\\s+", " ");
    }

    private static ConflictoException repetido() {
        return new ConflictoException("nombre", "Ya existe un consultorio con ese nombre en esa sucursal");
    }
}

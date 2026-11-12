package com.innovax.barberia.servicio;

import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.especialidad.EspecialidadRepository;
import com.innovax.barberia.profesional.Profesional;
import com.innovax.barberia.profesional.ProfesionalRepository;
import com.innovax.barberia.servicio.dto.ServicioPublicoResponse;
import com.innovax.barberia.sucursal.Sucursal;
import com.innovax.barberia.sucursal.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServicioPublicoService {

    private final NegocioRepository negocios;
    private final SucursalRepository sucursales;
    private final ServicioRepository servicios;
    private final ProfesionalRepository profesionales;
    private final EspecialidadRepository especialidades;

    /**
     * Sin sucursal: todos los servicios activos de la barbería.
     * Con sucursal: solo los que se pueden reservar ahí (los hace algún barbero activo de esa sucursal).
     * Con sucursal y profesional: los que hace ese profesional (rubro salud, profesional elegido antes que la prestación).
     * Con sucursal y especialidad: los que hace algún profesional de esa especialidad ("cualquier profesional").
     */
    @Transactional(readOnly = true)
    public List<ServicioPublicoResponse> listar(String slug, UUID sucursalId, UUID profesionalId, UUID especialidadId) {
        Negocio negocio = negocios.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado"));

        if (sucursalId == null && (profesionalId != null || especialidadId != null)) {
            throw new DatosInvalidosException("sucursal", "Indicá la sucursal");
        }

        List<Servicio> lista;
        if (sucursalId == null) {
            lista = servicios.findByNegocioIdAndActivoTrueOrderByOrdenAscNombreAsc(negocio.getId());
        } else {
            Sucursal sucursal = sucursales.findById(sucursalId)
                    .filter(Sucursal::isActiva)
                    .filter(s -> s.getNegocio().getId().equals(negocio.getId()))
                    .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
            if (profesionalId != null) {
                lista = delProfesional(negocio, sucursal, profesionalId);
            } else if (especialidadId != null) {
                especialidades.findByIdAndNegocioId(especialidadId, negocio.getId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada"));
                lista = servicios.findOfrecidosEnSucursalPorEspecialidad(negocio.getId(), sucursal.getId(), especialidadId);
            } else {
                lista = servicios.findOfrecidosEnSucursal(negocio.getId(), sucursal.getId());
            }
        }

        return lista.stream().map(ServicioPublicoResponse::de).toList();
    }

    /** Un profesional inactivo, de otro negocio o que no atiende en la sucursal no tiene nada para reservar. */
    private List<Servicio> delProfesional(Negocio negocio, Sucursal sucursal, UUID profesionalId) {
        Profesional profesional = profesionales.findByIdAndNegocioId(profesionalId, negocio.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesional no encontrado"));
        if (!profesional.isActivo() || !profesional.getSucursales().contains(sucursal)) {
            return List.of();
        }
        return profesional.getServicios().stream()
                .filter(Servicio::isActivo)
                .sorted(Comparator.comparingInt(Servicio::getOrden).thenComparing(Servicio::getNombre))
                .toList();
    }
}

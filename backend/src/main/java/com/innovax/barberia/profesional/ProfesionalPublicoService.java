package com.innovax.barberia.profesional;

import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.disponibilidad.DisponibilidadService;
import com.innovax.barberia.especialidad.Especialidad;
import com.innovax.barberia.especialidad.EspecialidadRepository;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.profesional.dto.ProfesionalPublicoResponse;
import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.servicio.ServicioRepository;
import com.innovax.barberia.sucursal.Sucursal;
import com.innovax.barberia.sucursal.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfesionalPublicoService {

    private final NegocioRepository negocios;
    private final SucursalRepository sucursales;
    private final ServicioRepository servicios;
    private final EspecialidadRepository especialidades;
    private final ProfesionalRepository profesionales;
    private final DisponibilidadService disponibilidadService;

    /**
     * Profesionales activos de la sucursal, con su próximo día libre.
     * <ul>
     *   <li>Con servicio: los que lo hacen (y, si además viene especialidad, la tienen).</li>
     *   <li>Solo con especialidad (rubro salud, todavía no se eligió la prestación): los que la tienen.
     *       El próximo día libre es el de su prestación más corta: si entra la más larga, entra la más corta,
     *       así que es el primer día en que se le puede reservar algo.</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public List<ProfesionalPublicoResponse> listar(String slug, UUID sucursalId, UUID servicioId, UUID especialidadId) {
        if (servicioId == null && especialidadId == null) {
            throw new DatosInvalidosException("servicio", "Indicá el servicio o la especialidad");
        }
        Negocio negocio = negocios.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado"));

        Sucursal sucursal = sucursales.findById(sucursalId)
                .filter(Sucursal::isActiva)
                .filter(s -> s.getNegocio().getId().equals(negocio.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
        Especialidad especialidad = especialidadId == null ? null : especialidades
                .findByIdAndNegocioId(especialidadId, negocio.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Especialidad no encontrada"));

        if (servicioId == null) {
            return profesionales.findActivosDeSucursalConEspecialidad(sucursal.getId(), especialidad.getId()).stream()
                    .sorted(Comparator.comparing(Profesional::getNombre).thenComparing(Profesional::getApellido))
                    .map(p -> ProfesionalPublicoResponse.de(p, proximoDiaConAlgunServicio(sucursal, p)))
                    .toList();
        }

        Servicio servicio = servicios.findById(servicioId)
                .filter(Servicio::isActivo)
                .filter(s -> s.getNegocio().getId().equals(negocio.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));

        return profesionales.findByActivoTrueAndSucursalesContainingAndServiciosContaining(sucursal, servicio).stream()
                .filter(p -> especialidad == null || p.getEspecialidades().contains(especialidad))
                .sorted(Comparator.comparing(Profesional::getNombre).thenComparing(Profesional::getApellido))
                .map(b -> ProfesionalPublicoResponse.de(b,
                        disponibilidadService.proximoDiaDisponible(sucursal.getId(), servicio.getId(), b.getId())
                                .orElse(null)))
                .toList();
    }

    private LocalDate proximoDiaConAlgunServicio(Sucursal sucursal, Profesional profesional) {
        return profesional.getServicios().stream()
                .filter(Servicio::isActivo)
                .min(Comparator.comparingInt(Servicio::getDuracionMin))
                .flatMap(s -> disponibilidadService.proximoDiaDisponible(sucursal.getId(), s.getId(), profesional.getId()))
                .orElse(null);
    }
}

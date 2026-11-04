package com.innovax.barberia.profesional;

import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.profesional.dto.ProfesionalPublicoResponse;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.disponibilidad.DisponibilidadService;
import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.servicio.ServicioRepository;
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
public class ProfesionalPublicoService {

    private final NegocioRepository negocios;
    private final SucursalRepository sucursales;
    private final ServicioRepository servicios;
    private final ProfesionalRepository profesionales;
    private final DisponibilidadService disponibilidadService;

    /** Barberos activos que trabajan en la sucursal y hacen el servicio, con su próximo día libre. */
    @Transactional(readOnly = true)
    public List<ProfesionalPublicoResponse> listar(String slug, UUID sucursalId, UUID servicioId) {
        Negocio negocio = negocios.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RecursoNoEncontradoException("Barbería no encontrada"));

        Sucursal sucursal = sucursales.findById(sucursalId)
                .filter(Sucursal::isActiva)
                .filter(s -> s.getNegocio().getId().equals(negocio.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
        Servicio servicio = servicios.findById(servicioId)
                .filter(Servicio::isActivo)
                .filter(s -> s.getNegocio().getId().equals(negocio.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));

        return profesionales.findByActivoTrueAndSucursalesContainingAndServiciosContaining(sucursal, servicio).stream()
                .sorted(Comparator.comparing(Profesional::getNombre).thenComparing(Profesional::getApellido))
                .map(b -> ProfesionalPublicoResponse.de(b,
                        disponibilidadService.proximoDiaDisponible(sucursal.getId(), servicio.getId(), b.getId())
                                .orElse(null)))
                .toList();
    }
}

package com.innovax.barberia.barbero;

import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.barbero.dto.BarberoPublicoResponse;
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
public class BarberoPublicoService {

    private final BarberiaRepository barberias;
    private final SucursalRepository sucursales;
    private final ServicioRepository servicios;
    private final BarberoRepository barberos;
    private final DisponibilidadService disponibilidadService;

    /** Barberos activos que trabajan en la sucursal y hacen el servicio, con su próximo día libre. */
    @Transactional(readOnly = true)
    public List<BarberoPublicoResponse> listar(String slug, UUID sucursalId, UUID servicioId) {
        Barberia barberia = barberias.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RecursoNoEncontradoException("Barbería no encontrada"));

        Sucursal sucursal = sucursales.findById(sucursalId)
                .filter(Sucursal::isActiva)
                .filter(s -> s.getBarberia().getId().equals(barberia.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
        Servicio servicio = servicios.findById(servicioId)
                .filter(Servicio::isActivo)
                .filter(s -> s.getBarberia().getId().equals(barberia.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));

        return barberos.findByActivoTrueAndSucursalesContainingAndServiciosContaining(sucursal, servicio).stream()
                .sorted(Comparator.comparing(Barbero::getNombre).thenComparing(Barbero::getApellido))
                .map(b -> BarberoPublicoResponse.de(b,
                        disponibilidadService.proximoDiaDisponible(sucursal.getId(), servicio.getId(), b.getId())
                                .orElse(null)))
                .toList();
    }
}

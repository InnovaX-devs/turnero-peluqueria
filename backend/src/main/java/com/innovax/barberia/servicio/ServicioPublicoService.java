package com.innovax.barberia.servicio;

import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.servicio.dto.ServicioPublicoResponse;
import com.innovax.barberia.sucursal.Sucursal;
import com.innovax.barberia.sucursal.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServicioPublicoService {

    private final NegocioRepository negocios;
    private final SucursalRepository sucursales;
    private final ServicioRepository servicios;

    /**
     * Sin sucursal: todos los servicios activos de la barbería.
     * Con sucursal: solo los que se pueden reservar ahí (los hace algún barbero activo de esa sucursal).
     */
    @Transactional(readOnly = true)
    public List<ServicioPublicoResponse> listar(String slug, UUID sucursalId) {
        Negocio negocio = negocios.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado"));

        List<Servicio> lista;
        if (sucursalId == null) {
            lista = servicios.findByNegocioIdAndActivoTrueOrderByOrdenAscNombreAsc(negocio.getId());
        } else {
            Sucursal sucursal = sucursales.findById(sucursalId)
                    .filter(Sucursal::isActiva)
                    .filter(s -> s.getNegocio().getId().equals(negocio.getId()))
                    .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
            lista = servicios.findOfrecidosEnSucursal(negocio.getId(), sucursal.getId());
        }

        return lista.stream().map(ServicioPublicoResponse::de).toList();
    }
}

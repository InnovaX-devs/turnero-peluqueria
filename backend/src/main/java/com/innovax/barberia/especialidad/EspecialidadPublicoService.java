package com.innovax.barberia.especialidad;

import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.especialidad.dto.EspecialidadResponse;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
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
public class EspecialidadPublicoService {

    private final NegocioRepository negocios;
    private final SucursalRepository sucursales;
    private final EspecialidadRepository especialidades;

    /** Solo las especialidades que se pueden reservar en esa sucursal (las tiene algún profesional activo de ahí). */
    @Transactional(readOnly = true)
    public List<EspecialidadResponse> listar(String slug, UUID sucursalId) {
        Negocio negocio = negocios.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado"));
        Sucursal sucursal = sucursales.findById(sucursalId)
                .filter(Sucursal::isActiva)
                .filter(s -> s.getNegocio().getId().equals(negocio.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
        return especialidades.findReservablesEnSucursal(negocio.getId(), sucursal.getId()).stream()
                .map(EspecialidadResponse::de)
                .toList();
    }
}

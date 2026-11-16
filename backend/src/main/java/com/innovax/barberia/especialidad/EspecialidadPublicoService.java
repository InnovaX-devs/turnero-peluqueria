package com.innovax.barberia.especialidad;

import com.innovax.barberia.cobertura.Cobertura;
import com.innovax.barberia.cobertura.CoberturaRepository;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.profesional.ProfesionalRepository;
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
    private final CoberturaRepository coberturas;
    private final ProfesionalRepository profesionales;

    @Transactional(readOnly = true)
    public List<EspecialidadResponse> listar(String slug, UUID sucursalId) {
        return listar(slug, sucursalId, null);
    }

    /**
     * Solo las especialidades que se pueden reservar en esa sucursal (las tiene algún profesional activo de ahí).
     * Con cobertura (issue #67): además, algún profesional de esa especialidad tiene que atender esa obra social.
     */
    @Transactional(readOnly = true)
    public List<EspecialidadResponse> listar(String slug, UUID sucursalId, UUID coberturaId) {
        Negocio negocio = negocios.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado"));
        Sucursal sucursal = sucursales.findById(sucursalId)
                .filter(Sucursal::isActiva)
                .filter(s -> s.getNegocio().getId().equals(negocio.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
        Cobertura cobertura = coberturas.activaONull(negocio.getId(), coberturaId);
        return especialidades.findReservablesEnSucursal(negocio.getId(), sucursal.getId()).stream()
                .filter(e -> cobertura == null || profesionales
                        .findActivosDeSucursalConEspecialidad(sucursal.getId(), e.getId()).stream()
                        .anyMatch(p -> p.atiende(cobertura)))
                .map(EspecialidadResponse::de)
                .toList();
    }
}

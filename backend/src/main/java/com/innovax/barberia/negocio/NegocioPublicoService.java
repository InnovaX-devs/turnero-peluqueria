package com.innovax.barberia.negocio;

import com.innovax.barberia.negocio.dto.NegocioPublicoResponse;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.especialidad.EspecialidadRepository;
import com.innovax.barberia.sucursal.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NegocioPublicoService {

    private final NegocioRepository negocios;
    private final SucursalRepository sucursales;
    private final EspecialidadRepository especialidades;

    /** Se devuelve aunque tenga las reservas online apagadas: la página igual muestra cómo contactarla. */
    @Transactional(readOnly = true)
    public NegocioPublicoResponse obtenerPorSlug(String slug) {
        // Los slugs se guardan en minúscula; así /b/Jade también funciona
        Negocio negocio = negocios.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado"));
        boolean porEspecialidad = negocio.getRubro() == Rubro.SALUD && especialidades.existsByNegocioId(negocio.getId());
        return NegocioPublicoResponse.de(negocio,
                sucursales.findByNegocioIdAndActivaTrueOrderByNombreAsc(negocio.getId()), porEspecialidad);
    }
}
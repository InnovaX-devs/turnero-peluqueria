package com.innovax.barberia.barberia;

import com.innovax.barberia.barberia.dto.BarberiaPublicaResponse;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.sucursal.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BarberiaPublicaService {

    private final BarberiaRepository barberias;
    private final SucursalRepository sucursales;

    /** Se devuelve aunque tenga las reservas online apagadas: la página igual muestra cómo contactarla. */
    @Transactional(readOnly = true)
    public BarberiaPublicaResponse obtenerPorSlug(String slug) {
        // Los slugs se guardan en minúscula; así /b/Jade también funciona
        Barberia barberia = barberias.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RecursoNoEncontradoException("Barbería no encontrada"));
        return BarberiaPublicaResponse.de(barberia,
                sucursales.findByBarberiaIdAndActivaTrueOrderByNombreAsc(barberia.getId()));
    }
}
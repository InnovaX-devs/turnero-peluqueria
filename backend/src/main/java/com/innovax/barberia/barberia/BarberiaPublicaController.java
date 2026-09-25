package com.innovax.barberia.barberia;

import com.innovax.barberia.barberia.dto.BarberiaPublicaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Datos de la barbería para la página pública de reserva (/b/{slug}). Sin login. */
@RestController
@RequestMapping("/api/public/barberias")
@RequiredArgsConstructor
public class BarberiaPublicaController {

    private final BarberiaPublicaService barberiaPublicaService;

    @GetMapping("/{slug}")
    public BarberiaPublicaResponse obtener(@PathVariable String slug) {
        return barberiaPublicaService.obtenerPorSlug(slug);
    }
}
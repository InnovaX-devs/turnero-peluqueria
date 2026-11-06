package com.innovax.barberia.negocio;

import com.innovax.barberia.negocio.dto.NegocioPublicoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Datos de la barbería para la página pública de reserva (/b/{slug}). Sin login. */
@RestController
// /api/public/barberias: ruta anterior al issue #64, queda como alias para links en circulación
@RequestMapping({"/api/public/negocios", "/api/public/barberias"})
@RequiredArgsConstructor
public class NegocioPublicoController {

    private final NegocioPublicoService negocioPublicoService;

    @GetMapping("/{slug}")
    public NegocioPublicoResponse obtener(@PathVariable String slug) {
        return negocioPublicoService.obtenerPorSlug(slug);
    }
}
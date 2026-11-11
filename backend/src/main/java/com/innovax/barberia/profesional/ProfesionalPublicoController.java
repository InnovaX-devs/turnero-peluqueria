package com.innovax.barberia.profesional;

import com.innovax.barberia.profesional.dto.ProfesionalPublicoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Barberos para el paso "Elegí el profesional" de la reserva pública. Sin login. */
@RestController
// /api/public/barberias/{slug}/barberos: ruta anterior al issue #64, queda como alias
@RequestMapping({"/api/public/negocios/{slug}/profesionales", "/api/public/barberias/{slug}/barberos"})
@RequiredArgsConstructor
public class ProfesionalPublicoController {

    private final ProfesionalPublicoService profesionalPublicoService;

    @GetMapping
    public List<ProfesionalPublicoResponse> listar(
            @PathVariable String slug,
            @RequestParam UUID sucursal,
            @RequestParam UUID servicio) {
        return profesionalPublicoService.listar(slug, sucursal, servicio);
    }
}

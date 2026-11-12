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

/**
 * Profesionales para el paso "Elegí el profesional" de la reserva pública. Sin login.
 * Se filtra por servicio (flujo normal: servicio → profesional) o por especialidad
 * (rubro salud: especialidad → profesional → prestación). Hace falta al menos uno de los dos.
 */
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
            @RequestParam(required = false) UUID servicio,
            @RequestParam(required = false) UUID especialidad) {
        return profesionalPublicoService.listar(slug, sucursal, servicio, especialidad);
    }
}

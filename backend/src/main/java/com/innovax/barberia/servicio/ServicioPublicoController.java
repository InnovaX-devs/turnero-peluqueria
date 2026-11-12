package com.innovax.barberia.servicio;

import com.innovax.barberia.servicio.dto.ServicioPublicoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Servicios para el paso "Elegí el servicio" de la reserva pública. Sin login.
 * En el rubro salud la prestación se elige después del profesional: se filtra por {@code profesional}
 * o, si eligió "cualquiera", por {@code especialidad}.
 */
@RestController
// /api/public/barberias/{slug}/servicios: ruta anterior al issue #64, queda como alias
@RequestMapping({"/api/public/negocios/{slug}/servicios", "/api/public/barberias/{slug}/servicios"})
@RequiredArgsConstructor
public class ServicioPublicoController {

    private final ServicioPublicoService servicioPublicoService;

    @GetMapping
    public List<ServicioPublicoResponse> listar(
            @PathVariable String slug,
            @RequestParam(required = false) UUID sucursal,
            @RequestParam(required = false) UUID profesional,
            @RequestParam(required = false) UUID especialidad) {
        return servicioPublicoService.listar(slug, sucursal, profesional, especialidad);
    }
}

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

/** Servicios para el paso "Elegí el servicio" de la reserva pública. Sin login. */
@RestController
@RequestMapping("/api/public/barberias/{slug}/servicios")
@RequiredArgsConstructor
public class ServicioPublicoController {

    private final ServicioPublicoService servicioPublicoService;

    @GetMapping
    public List<ServicioPublicoResponse> listar(
            @PathVariable String slug,
            @RequestParam(required = false) UUID sucursal) {
        return servicioPublicoService.listar(slug, sucursal);
    }
}

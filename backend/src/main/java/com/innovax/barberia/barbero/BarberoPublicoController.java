package com.innovax.barberia.barbero;

import com.innovax.barberia.barbero.dto.BarberoPublicoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Barberos para el paso "Elegí el barbero" de la reserva pública. Sin login. */
@RestController
@RequestMapping("/api/public/barberias/{slug}/barberos")
@RequiredArgsConstructor
public class BarberoPublicoController {

    private final BarberoPublicoService barberoPublicoService;

    @GetMapping
    public List<BarberoPublicoResponse> listar(
            @PathVariable String slug,
            @RequestParam UUID sucursal,
            @RequestParam UUID servicio) {
        return barberoPublicoService.listar(slug, sucursal, servicio);
    }
}

package com.innovax.barberia.especialidad;

import com.innovax.barberia.especialidad.dto.EspecialidadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Especialidades para el paso "Elegí la especialidad" de la reserva pública (rubro salud). Sin login. */
@RestController
@RequestMapping("/api/public/negocios/{slug}/especialidades")
@RequiredArgsConstructor
public class EspecialidadPublicoController {

    private final EspecialidadPublicoService especialidadPublicoService;

    @GetMapping
    public List<EspecialidadResponse> listar(@PathVariable String slug, @RequestParam UUID sucursal) {
        return especialidadPublicoService.listar(slug, sucursal);
    }
}

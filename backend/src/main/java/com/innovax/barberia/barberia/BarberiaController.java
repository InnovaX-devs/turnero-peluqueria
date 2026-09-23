package com.innovax.barberia.barberia;

import com.innovax.barberia.barberia.dto.ActualizarBarberiaRequest;
import com.innovax.barberia.barberia.dto.BarberiaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Configuración de la barbería del usuario logueado. Solo admin. */
@RestController
@RequestMapping("/api/barberia")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BarberiaController {

    private final BarberiaService barberiaService;

    @GetMapping
    public BarberiaResponse obtener() {
        return barberiaService.obtener();
    }

    @PutMapping
    public BarberiaResponse actualizar(@Valid @RequestBody ActualizarBarberiaRequest request) {
        return barberiaService.actualizar(request);
    }
}
package com.innovax.barberia.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Endpoints que solo existen en los tests, para probar roles y aislamiento. */
@RestController
@RequestMapping("/api/test-permisos")
@RequiredArgsConstructor
class PermisosTestController {

    private final UsuarioActual usuarioActual;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/solo-admin")
    String soloAdmin() {
        return "ok";
    }

    @GetMapping("/barbero/{barberoId}")
    String barbero(@PathVariable UUID barberoId) {
        usuarioActual.verificarAccesoABarbero(barberoId);
        return "ok";
    }

    @GetMapping("/mi-barberia")
    String miBarberia() {
        return usuarioActual.barberiaId().toString();
    }
}

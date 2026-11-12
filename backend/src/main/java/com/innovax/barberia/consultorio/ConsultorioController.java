package com.innovax.barberia.consultorio;

import com.innovax.barberia.consultorio.dto.ConsultorioRequests;
import com.innovax.barberia.consultorio.dto.ConsultorioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** ABM de consultorios del negocio del usuario logueado (rubro salud, issue #69). Solo admin. */
@RestController
@RequestMapping("/api/consultorios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ConsultorioController {

    private final ConsultorioService consultorioService;

    @GetMapping
    public List<ConsultorioResponse> listar(@RequestParam(defaultValue = "true") boolean incluirInactivos) {
        return consultorioService.listar(incluirInactivos);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultorioResponse crear(@Valid @RequestBody ConsultorioRequests.Crear request) {
        return consultorioService.crear(request);
    }

    @PutMapping("/{id}")
    public ConsultorioResponse renombrar(@PathVariable UUID id, @Valid @RequestBody ConsultorioRequests.Renombrar request) {
        return consultorioService.renombrar(id, request);
    }

    /** Baja lógica (activo=false) o reactivación (activo=true). */
    @PatchMapping("/{id}/activo")
    public ConsultorioResponse cambiarActivo(@PathVariable UUID id, @Valid @RequestBody ConsultorioRequests.CambiarActivo request) {
        return consultorioService.cambiarActivo(id, request.activo());
    }
}

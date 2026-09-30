package com.innovax.barberia.servicio;

import com.innovax.barberia.servicio.dto.ServicioRequests;
import com.innovax.barberia.servicio.dto.ServicioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** ABM de servicios de la barbería del usuario logueado. Solo admin. */
@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ServicioController {

    private final ServicioService servicioService;

    @GetMapping
    public List<ServicioResponse> listar(@RequestParam(defaultValue = "true") boolean incluirInactivos) {
        return servicioService.listar(incluirInactivos);
    }

    @GetMapping("/{id}")
    public ServicioResponse obtener(@PathVariable UUID id) {
        return servicioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicioResponse crear(@Valid @RequestBody ServicioRequests.Crear request) {
        return servicioService.crear(request);
    }

    @PutMapping("/{id}")
    public ServicioResponse actualizar(@PathVariable UUID id, @Valid @RequestBody ServicioRequests.Actualizar request) {
        return servicioService.actualizar(id, request);
    }

    /** Baja lógica (activo=false) o reactivación (activo=true). */
    @PatchMapping("/{id}/activo")
    public ServicioResponse cambiarActivo(@PathVariable UUID id, @Valid @RequestBody ServicioRequests.CambiarActivo request) {
        return servicioService.cambiarActivo(id, request.activo());
    }

    /** Define el orden en que se muestran en la página pública. */
    @PutMapping("/orden")
    public List<ServicioResponse> reordenar(@Valid @RequestBody ServicioRequests.Reordenar request) {
        return servicioService.reordenar(request);
    }
}
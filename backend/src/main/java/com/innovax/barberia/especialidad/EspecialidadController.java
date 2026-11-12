package com.innovax.barberia.especialidad;

import com.innovax.barberia.especialidad.dto.EspecialidadRequest;
import com.innovax.barberia.especialidad.dto.EspecialidadResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** ABM de especialidades del negocio del usuario logueado (rubro salud). Solo admin. */
@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @GetMapping
    public List<EspecialidadResponse> listar() {
        return especialidadService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EspecialidadResponse crear(@Valid @RequestBody EspecialidadRequest request) {
        return especialidadService.crear(request);
    }

    @PutMapping("/{id}")
    public EspecialidadResponse renombrar(@PathVariable UUID id, @Valid @RequestBody EspecialidadRequest request) {
        return especialidadService.renombrar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@PathVariable UUID id) {
        especialidadService.borrar(id);
    }
}

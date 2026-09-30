package com.innovax.barberia.barbero;

import com.innovax.barberia.barbero.dto.BarberoRequests;
import com.innovax.barberia.barbero.dto.BarberoResponse;
import com.innovax.barberia.barbero.dto.HorarioResponse;
import com.innovax.barberia.barbero.dto.OpcionesBarberoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** ABM de barberos de la barbería del usuario logueado. Solo admin. */
@RestController
@RequestMapping("/api/barberos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BarberoController {

    private final BarberoService barberoService;

    @GetMapping
    public List<BarberoResponse> listar(@RequestParam(defaultValue = "true") boolean incluirInactivos) {
        return barberoService.listar(incluirInactivos);
    }

    @GetMapping("/opciones")
    public OpcionesBarberoResponse opciones() {
        return barberoService.opciones();
    }

    @GetMapping("/{id}")
    public BarberoResponse obtener(@PathVariable UUID id) {
        return barberoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BarberoResponse crear(@Valid @RequestBody BarberoRequests.Crear request) {
        return barberoService.crear(request);
    }

    @PutMapping("/{id}")
    public BarberoResponse actualizar(@PathVariable UUID id, @Valid @RequestBody BarberoRequests.Actualizar request) {
        return barberoService.actualizar(id, request);
    }

    /** Baja lógica (activo=false) o reactivación (activo=true). */
    @PatchMapping("/{id}/activo")
    public BarberoResponse cambiarActivo(@PathVariable UUID id, @Valid @RequestBody BarberoRequests.CambiarActivo request) {
        return barberoService.cambiarActivo(id, request.activo());
    }

    @PostMapping("/{id}/usuario")
    @ResponseStatus(HttpStatus.CREATED)
    public BarberoResponse crearUsuario(@PathVariable UUID id, @Valid @RequestBody BarberoRequests.CrearUsuario request) {
        return barberoService.crearUsuario(id, request);
    }

    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BarberoResponse subirFoto(@PathVariable UUID id, @RequestParam("archivo") MultipartFile archivo) {
        return barberoService.subirFoto(id, archivo);
    }

    @DeleteMapping("/{id}/foto")
    public BarberoResponse quitarFoto(@PathVariable UUID id) {
        return barberoService.quitarFoto(id);
    }

    @GetMapping("/{id}/horarios")
    public List<HorarioResponse> horarios(@PathVariable UUID id) {
        return barberoService.horarios(id);
    }

    @PutMapping("/{id}/horarios")
    public List<HorarioResponse> guardarHorarios(@PathVariable UUID id,
                                                 @Valid @RequestBody BarberoRequests.GuardarHorarios request) {
        return barberoService.guardarHorarios(id, request);
    }
}
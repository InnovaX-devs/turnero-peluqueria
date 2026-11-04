package com.innovax.barberia.profesional;

import com.innovax.barberia.profesional.dto.ProfesionalRequests;
import com.innovax.barberia.profesional.dto.ProfesionalResponse;
import com.innovax.barberia.profesional.dto.HorarioResponse;
import com.innovax.barberia.profesional.dto.OpcionesProfesionalResponse;
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
@RequestMapping("/api/profesionales")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProfesionalController {

    private final ProfesionalService profesionalService;

    @GetMapping
    public List<ProfesionalResponse> listar(@RequestParam(defaultValue = "true") boolean incluirInactivos) {
        return profesionalService.listar(incluirInactivos);
    }

    @GetMapping("/opciones")
    public OpcionesProfesionalResponse opciones() {
        return profesionalService.opciones();
    }

    @GetMapping("/{id}")
    public ProfesionalResponse obtener(@PathVariable UUID id) {
        return profesionalService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfesionalResponse crear(@Valid @RequestBody ProfesionalRequests.Crear request) {
        return profesionalService.crear(request);
    }

    @PutMapping("/{id}")
    public ProfesionalResponse actualizar(@PathVariable UUID id, @Valid @RequestBody ProfesionalRequests.Actualizar request) {
        return profesionalService.actualizar(id, request);
    }

    /** Baja lógica (activo=false) o reactivación (activo=true). */
    @PatchMapping("/{id}/activo")
    public ProfesionalResponse cambiarActivo(@PathVariable UUID id, @Valid @RequestBody ProfesionalRequests.CambiarActivo request) {
        return profesionalService.cambiarActivo(id, request.activo());
    }

    /** Crea el usuario del barbero y le manda la invitación por email. */
    @PostMapping("/{id}/usuario")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfesionalResponse crearUsuario(@PathVariable UUID id, @Valid @RequestBody ProfesionalRequests.CrearUsuario request) {
        return profesionalService.crearUsuario(id, request);
    }

    /** Reenvía la invitación (link nuevo; los anteriores dejan de valer). */
    @PostMapping("/{id}/invitacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviarInvitacion(@PathVariable UUID id) {
        profesionalService.reenviarInvitacion(id);
    }

    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfesionalResponse subirFoto(@PathVariable UUID id, @RequestParam("archivo") MultipartFile archivo) {
        return profesionalService.subirFoto(id, archivo);
    }

    @DeleteMapping("/{id}/foto")
    public ProfesionalResponse quitarFoto(@PathVariable UUID id) {
        return profesionalService.quitarFoto(id);
    }

    @GetMapping("/{id}/horarios")
    public List<HorarioResponse> horarios(@PathVariable UUID id) {
        return profesionalService.horarios(id);
    }

    @PutMapping("/{id}/horarios")
    public List<HorarioResponse> guardarHorarios(@PathVariable UUID id,
                                                 @Valid @RequestBody ProfesionalRequests.GuardarHorarios request) {
        return profesionalService.guardarHorarios(id, request);
    }
}
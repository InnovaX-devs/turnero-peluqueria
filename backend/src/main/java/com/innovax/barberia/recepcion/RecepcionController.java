package com.innovax.barberia.recepcion;

import com.innovax.barberia.recepcion.dto.RecepcionRequests;
import com.innovax.barberia.recepcion.dto.UsuarioRecepcionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** ABM de usuarios de recepción / secretaría (issue #70). Solo admin. */
@RestController
@RequestMapping("/api/recepcion/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class RecepcionController {

    private final RecepcionService recepcionService;

    @GetMapping
    public List<UsuarioRecepcionResponse> listar() {
        return recepcionService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioRecepcionResponse crear(@Valid @RequestBody RecepcionRequests.Crear request) {
        return recepcionService.crear(request);
    }

    @PostMapping("/{id}/invitacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviarInvitacion(@PathVariable UUID id) {
        recepcionService.reenviarInvitacion(id);
    }

    @PatchMapping("/{id}/activo")
    public UsuarioRecepcionResponse cambiarActivo(@PathVariable UUID id,
                                                  @Valid @RequestBody RecepcionRequests.CambiarActivo request) {
        return recepcionService.cambiarActivo(id, request.activo());
    }
}

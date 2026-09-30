package com.innovax.barberia.atencion.gestion;

import com.innovax.barberia.atencion.gestion.dto.ReprogramarTurnoRequest;
import com.innovax.barberia.atencion.gestion.dto.TurnoPublicoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Gestión del turno desde el link del cliente. Público: /api/public/** no pide login. */
@RestController
@RequestMapping("/api/public/turnos/{token}")
@RequiredArgsConstructor
public class GestionTurnoController {

    private final GestionTurnoService gestionTurnoService;

    @GetMapping
    public TurnoPublicoResponse obtener(@PathVariable UUID token) {
        return gestionTurnoService.obtener(token);
    }

    @PostMapping("/cancelar")
    public TurnoPublicoResponse cancelar(@PathVariable UUID token) {
        return gestionTurnoService.cancelar(token);
    }

    @PutMapping("/reprogramar")
    public TurnoPublicoResponse reprogramar(@PathVariable UUID token,
                                            @Valid @RequestBody ReprogramarTurnoRequest request) {
        return gestionTurnoService.reprogramar(token, request);
    }
}

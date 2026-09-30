package com.innovax.barberia.horario;

import com.innovax.barberia.agenda.dto.BloqueoAgendaResponse;
import com.innovax.barberia.agenda.dto.TurnoAgendaResponse;
import com.innovax.barberia.horario.dto.BloqueoRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Bloqueos del panel. Requiere sesión; admin y barbero comparten los endpoints (ver BloqueoService). */
@RestController
@RequestMapping("/api/bloqueos")
@RequiredArgsConstructor
public class BloqueoController {

    private final BloqueoService bloqueoService;

    /** Turnos reservados que este bloqueo taparía. No guarda nada. */
    @PostMapping("/conflictos")
    public List<TurnoAgendaResponse> conflictos(@Valid @RequestBody BloqueoRequest request) {
        return bloqueoService.turnosEnConflicto(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<BloqueoAgendaResponse> crear(@Valid @RequestBody BloqueoRequest request) {
        return bloqueoService.crear(request);
    }

    @PutMapping("/{id}")
    public BloqueoAgendaResponse actualizar(@PathVariable UUID id, @Valid @RequestBody BloqueoRequest request) {
        return bloqueoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        bloqueoService.eliminar(id);
    }
}
package com.innovax.barberia.agenda;

import com.innovax.barberia.agenda.dto.TurnoAgendaResponse;
import com.innovax.barberia.atencion.AtencionService;
import com.innovax.barberia.atencion.dto.CambiarEstadoTurnoRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Cambiar el estado de un turno desde el detalle del panel (marcar completado, ausente o
 * cancelado). Requiere sesión; el aislamiento por barbería/barbero y las reglas de la
 * transición las resuelve {@link AtencionService#cambiarEstado}.
 */
@RestController
@RequestMapping("/api/agenda/turnos")
@RequiredArgsConstructor
public class AgendaTurnoController {

    private final AtencionService atencionService;

    @PatchMapping("/{id}/estado")
    public TurnoAgendaResponse cambiarEstado(@PathVariable UUID id, @Valid @RequestBody CambiarEstadoTurnoRequest request) {
        return TurnoAgendaResponse.de(atencionService.cambiarEstado(id, request));
    }
}
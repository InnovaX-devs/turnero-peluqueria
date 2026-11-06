package com.innovax.barberia.agenda;

import com.innovax.barberia.agenda.dto.NuevoTurnoRequest;
import com.innovax.barberia.agenda.dto.TurnoAgendaResponse;
import com.innovax.barberia.atencion.AtencionService;
import com.innovax.barberia.atencion.dto.CambiarEstadoTurnoRequest;
import com.innovax.barberia.atencion.dto.MarcarLlegadaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    private final TurnoPanelService turnoPanelService;

    /** Dar turno desde el panel (issue #71): por teléfono o en el mostrador, normal o sobreturno. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TurnoAgendaResponse crear(@Valid @RequestBody NuevoTurnoRequest request) {
        return turnoPanelService.crear(request);
    }

    @PatchMapping("/{id}/estado")
    public TurnoAgendaResponse cambiarEstado(@PathVariable UUID id, @Valid @RequestBody CambiarEstadoTurnoRequest request) {
        return atencionService.cambiarEstado(id, request);
    }

    /** Sala de espera (issue #71): {"llego": true} cuando el cliente llega, false para desmarcarlo. */
    @PatchMapping("/{id}/llegada")
    public TurnoAgendaResponse marcarLlegada(@PathVariable UUID id, @Valid @RequestBody MarcarLlegadaRequest request) {
        return atencionService.marcarLlegada(id, request);
    }
}
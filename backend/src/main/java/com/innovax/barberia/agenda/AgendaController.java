package com.innovax.barberia.agenda;

import com.innovax.barberia.agenda.dto.AgendaResponse;
import com.innovax.barberia.agenda.dto.HorariosLibresResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Agenda del panel (turnos + bloqueos). Requiere sesión (ver SecurityConfig); el admin y el
 * barbero comparten el mismo endpoint, la diferencia de qué ven la resuelve AgendaService.
 */
@RestController
@RequestMapping("/api/agenda")
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaService agendaService;
    private final TurnoPanelService turnoPanelService;

    @GetMapping
    public AgendaResponse agenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID profesionalId) {
        return agendaService.consultar(desde, hasta, profesionalId);
    }

    /** Horarios libres para dar turno desde el panel (issue #71). */
    @GetMapping("/horarios-libres")
    public HorariosLibresResponse horariosLibres(
            @RequestParam UUID profesionalId,
            @RequestParam UUID servicioId,
            @RequestParam(required = false) UUID sucursalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return turnoPanelService.horariosLibres(profesionalId, servicioId, sucursalId, fecha);
    }
}
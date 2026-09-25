package com.innovax.barberia.agenda;

import com.innovax.barberia.agenda.dto.AgendaResponse;
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

    @GetMapping
    public AgendaResponse agenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID barberoId) {
        return agendaService.consultar(desde, hasta, barberoId);
    }
}
package com.innovax.barberia.disponibilidad;

import com.innovax.barberia.disponibilidad.dto.DiasDisponiblesResponse;
import com.innovax.barberia.disponibilidad.dto.DisponibilidadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/disponibilidad")
@RequiredArgsConstructor
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    @GetMapping
    public DisponibilidadResponse disponibilidad(
            @RequestParam UUID sucursal,
            @RequestParam UUID servicio,
            @RequestParam(required = false) UUID barbero,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return disponibilidadService.consultar(sucursal, servicio, barbero, fecha);
    }

    @GetMapping("/dias")
    public DiasDisponiblesResponse dias(
            @RequestParam UUID sucursal,
            @RequestParam UUID servicio,
            @RequestParam(required = false) UUID barbero,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return disponibilidadService.consultarDias(sucursal, servicio, barbero, desde, hasta);
    }
}
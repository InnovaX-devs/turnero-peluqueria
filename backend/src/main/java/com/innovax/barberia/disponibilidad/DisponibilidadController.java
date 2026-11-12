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

/**
 * Horarios libres para la reserva pública. Sin {@code profesional} = "cualquiera"; en ese caso
 * {@code especialidad} (rubro salud) limita a los profesionales que la tienen.
 */
@RestController
@RequestMapping("/api/public/disponibilidad")
@RequiredArgsConstructor
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    @GetMapping
    public DisponibilidadResponse disponibilidad(
            @RequestParam UUID sucursal,
            @RequestParam UUID servicio,
            @RequestParam(required = false) UUID profesional,
            @RequestParam(name = "barbero", required = false) UUID profesionalAnterior,
            @RequestParam(required = false) UUID especialidad,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return disponibilidadService.consultar(
                sucursal, servicio, elegido(profesional, profesionalAnterior), especialidad, fecha);
    }

    @GetMapping("/dias")
    public DiasDisponiblesResponse dias(
            @RequestParam UUID sucursal,
            @RequestParam UUID servicio,
            @RequestParam(required = false) UUID profesional,
            @RequestParam(name = "barbero", required = false) UUID profesionalAnterior,
            @RequestParam(required = false) UUID especialidad,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return disponibilidadService.consultarDias(
                sucursal, servicio, elegido(profesional, profesionalAnterior), especialidad, desde, hasta);
    }

    /** ?barbero= es el nombre del parámetro antes del issue #64: se sigue aceptando como alias. */
    private static UUID elegido(UUID profesional, UUID profesionalAnterior) {
        return profesional != null ? profesional : profesionalAnterior;
    }
}
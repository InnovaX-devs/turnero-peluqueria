package com.innovax.barberia.atencion.dto;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.EstadoAtencion;

import java.time.Instant;
import java.util.UUID;

public record AtencionResponse(
        UUID id,
        Instant inicio,
        Instant fin,
        EstadoAtencion estado,
        UUID barberoId,
        String barberoNombre,
        UUID servicioId,
        String servicioNombre,
        long precioFinal,
        UUID tokenGestion
) {

    public static AtencionResponse de(Atencion a) {
        return new AtencionResponse(
                a.getId(),
                a.getInicio(),
                a.getFin(),
                a.getEstado(),
                a.getBarbero().getId(),
                a.getBarbero().getNombre() + " " + a.getBarbero().getApellido(),
                a.getServicio().getId(),
                a.getServicio().getNombre(),
                a.getPrecioFinal(),
                a.getTokenGestion());
    }
}
package com.innovax.barberia.barberia.dto;

import com.innovax.barberia.barberia.Barberia;

import java.util.UUID;

public record BarberiaResponse(
        UUID id,
        String nombre,
        String slug,
        String logoUrl,
        boolean reservasOnline,
        int anticipacionMinimaMin,
        int anticipacionMaximaDias,
        int intervaloSlotsMin,
        int anticipacionCancelacionMin,
        int recordatorioHorasAntes
) {

    public static BarberiaResponse de(Barberia b) {
        return new BarberiaResponse(b.getId(), b.getNombre(), b.getSlug(), b.getLogoUrl(),
                b.isReservasOnline(), b.getAnticipacionMinimaMin(), b.getAnticipacionMaximaDias(),
                b.getIntervaloSlotsMin(), b.getAnticipacionCancelacionMin(), b.getRecordatorioHorasAntes());
    }
}
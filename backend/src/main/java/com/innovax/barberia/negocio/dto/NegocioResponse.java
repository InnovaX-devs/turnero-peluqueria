package com.innovax.barberia.negocio.dto;

import com.innovax.barberia.negocio.Negocio;

import java.util.UUID;

public record NegocioResponse(
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

    public static NegocioResponse de(Negocio b) {
        return new NegocioResponse(b.getId(), b.getNombre(), b.getSlug(), b.getLogoUrl(),
                b.isReservasOnline(), b.getAnticipacionMinimaMin(), b.getAnticipacionMaximaDias(),
                b.getIntervaloSlotsMin(), b.getAnticipacionCancelacionMin(), b.getRecordatorioHorasAntes());
    }
}
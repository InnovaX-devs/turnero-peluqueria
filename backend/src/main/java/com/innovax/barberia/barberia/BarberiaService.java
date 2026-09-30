package com.innovax.barberia.barberia;

import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.barberia.dto.ActualizarBarberiaRequest;
import com.innovax.barberia.barberia.dto.BarberiaResponse;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.DatosInvalidosException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class BarberiaService {

    private static final Set<Integer> INTERVALOS_PERMITIDOS = Set.of(15, 30);
    private static final int MINUTOS_POR_DIA = 24 * 60;

    private final BarberiaRepository barberias;
    private final UsuarioActual usuarioActual;

    @Transactional(readOnly = true)
    public BarberiaResponse obtener() {
        return BarberiaResponse.de(barberiaActual());
    }

    /**
     * Solo toca la configuración de la barbería. Los turnos ya reservados (atencion) no se
     * modifican nunca, aunque se desactiven las reservas online: el flag solo controla
     * la creación de turnos nuevos desde la página pública.
     */
    @Transactional
    public BarberiaResponse actualizar(ActualizarBarberiaRequest request) {
        Barberia barberia = barberiaActual();

        if (!INTERVALOS_PERMITIDOS.contains(request.intervaloSlotsMin())) {
            throw new DatosInvalidosException("intervaloSlotsMin", "El intervalo tiene que ser de 15 o 30 minutos");
        }
        if (request.anticipacionMinimaMin() >= request.anticipacionMaximaDias() * MINUTOS_POR_DIA) {
            throw new DatosInvalidosException("anticipacionMinimaMin",
                    "La anticipación mínima tiene que ser menor que la máxima");
        }

        String slug = request.slug();
        if (!slug.equals(barberia.getSlug()) && barberias.existsBySlug(slug)) {
            throw slugEnUso();
        }

        barberia.setNombre(request.nombre().trim());
        barberia.setSlug(slug);
        barberia.setLogoUrl(request.logoUrl());
        barberia.setReservasOnline(request.reservasOnline());
        barberia.setAnticipacionMinimaMin(request.anticipacionMinimaMin());
        barberia.setAnticipacionMaximaDias(request.anticipacionMaximaDias());
        if (request.anticipacionCancelacionMin() != null) {
            barberia.setAnticipacionCancelacionMin(request.anticipacionCancelacionMin());
        }
        barberia.setIntervaloSlotsMin(request.intervaloSlotsMin());

        try {
            // flush ahora para detectar acá un slug tomado justo en el medio (la base tiene UNIQUE)
            barberias.saveAndFlush(barberia);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("slug")) {
                throw slugEnUso();
            }
            throw e;
        }
        return BarberiaResponse.de(barberia);
    }

    /** La barbería sale del token, nunca del request. */
    private Barberia barberiaActual() {
        return barberias.findById(usuarioActual.barberiaId())
                .orElseThrow(() -> new IllegalStateException("La barbería del token no existe"));
    }

    private ConflictoException slugEnUso() {
        return new ConflictoException("slug", "Ese slug ya está en uso, elegí otro");
    }
}
package com.innovax.barberia.negocio;

import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.profesional.FotoStorageService;
import com.innovax.barberia.negocio.dto.ActualizarNegocioRequest;
import com.innovax.barberia.negocio.dto.NegocioResponse;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.DatosInvalidosException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class NegocioService {

    private static final Set<Integer> INTERVALOS_PERMITIDOS = Set.of(15, 30);
    private static final int MINUTOS_POR_DIA = 24 * 60;

    private final NegocioRepository negocios;
    private final UsuarioActual usuarioActual;
    private final FotoStorageService fotos;

    @Transactional(readOnly = true)
    public NegocioResponse obtener() {
        return NegocioResponse.de(negocioActual());
    }

    /**
     * Solo toca la configuración de la barbería. Los turnos ya reservados (atencion) no se
     * modifican nunca, aunque se desactiven las reservas online: el flag solo controla
     * la creación de turnos nuevos desde la página pública.
     */
    @Transactional
    public NegocioResponse actualizar(ActualizarNegocioRequest request) {
        Negocio negocio = negocioActual();

        if (!INTERVALOS_PERMITIDOS.contains(request.intervaloSlotsMin())) {
            throw new DatosInvalidosException("intervaloSlotsMin", "El intervalo tiene que ser de 15 o 30 minutos");
        }
        if (request.anticipacionMinimaMin() >= request.anticipacionMaximaDias() * MINUTOS_POR_DIA) {
            throw new DatosInvalidosException("anticipacionMinimaMin",
                    "La anticipación mínima tiene que ser menor que la máxima");
        }

        String slug = request.slug();
        if (!slug.equals(negocio.getSlug()) && negocios.existsBySlug(slug)) {
            throw slugEnUso();
        }

        negocio.setNombre(request.nombre().trim());
        negocio.setSlug(slug);
        negocio.setLogoUrl(request.logoUrl());
        negocio.setReservasOnline(request.reservasOnline());
        negocio.setAnticipacionMinimaMin(request.anticipacionMinimaMin());
        negocio.setAnticipacionMaximaDias(request.anticipacionMaximaDias());
        if (request.anticipacionCancelacionMin() != null) {
            negocio.setAnticipacionCancelacionMin(request.anticipacionCancelacionMin());
        }
        if (request.recordatorioHorasAntes() != null) {
            negocio.setRecordatorioHorasAntes(request.recordatorioHorasAntes());
        }
        negocio.setIntervaloSlotsMin(request.intervaloSlotsMin());

        try {
            // flush ahora para detectar acá un slug tomado justo en el medio (la base tiene UNIQUE)
            negocios.saveAndFlush(negocio);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("slug")) {
                throw slugEnUso();
            }
            throw e;
        }
        return NegocioResponse.de(negocio);
    }

    @Transactional
    public NegocioResponse subirLogo(MultipartFile archivo) {
        Negocio negocio = negocioActual();
        String anterior = negocio.getLogoUrl();
        negocio.setLogoUrl(fotos.subirLogo(negocio.getId(), archivo));
        fotos.borrarSilencioso(anterior);
        return NegocioResponse.de(negocio);
    }

    @Transactional
    public NegocioResponse quitarLogo() {
        Negocio negocio = negocioActual();
        String anterior = negocio.getLogoUrl();
        negocio.setLogoUrl(null);
        fotos.borrarSilencioso(anterior);
        return NegocioResponse.de(negocio);
    }

    /** La barbería sale del token, nunca del request. */
    private Negocio negocioActual() {
        return negocios.findById(usuarioActual.negocioId())
                .orElseThrow(() -> new IllegalStateException("La barbería del token no existe"));
    }

    private ConflictoException slugEnUso() {
        return new ConflictoException("slug", "Ese slug ya está en uso, elegí otro");
    }
}
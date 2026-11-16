package com.innovax.barberia.cobertura;

import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.cobertura.dto.ReporteCoberturasResponse;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reporte de atenciones por obra social / prepaga para facturación manual (issue #67). Solo admin. */
@Service
@RequiredArgsConstructor
public class ReporteCoberturaService {

    /** Un año: alcanza para cualquier período de facturación y evita consultas enormes. */
    private static final long MAXIMO_DIAS = 366;

    private final ReporteCoberturaRepository reporte;
    private final CoberturaRepository coberturas;
    private final NegocioRepository negocios;
    private final UsuarioActual usuarioActual;

    @Transactional(readOnly = true)
    public ReporteCoberturasResponse generar(LocalDate desde, LocalDate hasta, UUID coberturaId) {
        if (hasta.isBefore(desde)) {
            throw new DatosInvalidosException("hasta", "No puede ser anterior a «desde»");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAXIMO_DIAS) {
            throw new DatosInvalidosException("hasta", "El rango no puede superar un año");
        }
        UUID negocioId = usuarioActual.negocioId();
        if (coberturaId != null) {
            // Puede ser una desactivada: se sigue pudiendo facturar lo que se atendió con ella
            coberturas.findByIdAndNegocioId(coberturaId, negocioId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cobertura no encontrada"));
        }
        Negocio negocio = negocios.findById(negocioId)
                .orElseThrow(() -> new IllegalStateException("El negocio del token no existe"));
        ZoneId zona = ZoneId.of(negocio.getZonaHoraria());

        List<FilaCobertura> filas = reporte.buscar(negocioId,
                desde.atStartOfDay(zona).toInstant(),
                hasta.plusDays(1).atStartOfDay(zona).toInstant(),
                coberturaId);

        Map<UUID, long[]> acumulado = new LinkedHashMap<>(); // cantidad, prestaciones, copagos
        Map<UUID, String> nombres = new LinkedHashMap<>();
        long totalPrestaciones = 0;
        long totalCopagos = 0;
        for (FilaCobertura f : filas) {
            long copago = f.copago() == null ? 0 : f.copago();
            long[] acc = acumulado.computeIfAbsent(f.coberturaId(), k -> new long[3]);
            acc[0]++;
            acc[1] += f.precioFinal();
            acc[2] += copago;
            nombres.putIfAbsent(f.coberturaId(), f.coberturaNombre());
            totalPrestaciones += f.precioFinal();
            totalCopagos += copago;
        }

        List<ReporteCoberturasResponse.Resumen> resumen = acumulado.entrySet().stream()
                .map(e -> new ReporteCoberturasResponse.Resumen(
                        e.getKey(), nombres.get(e.getKey()), (int) e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .toList();

        List<ReporteCoberturasResponse.Detalle> detalle = filas.stream()
                .map(f -> new ReporteCoberturasResponse.Detalle(
                        f.atencionId(),
                        LocalDateTime.ofInstant(f.inicio(), zona),
                        f.coberturaId(),
                        f.coberturaNombre(),
                        f.tipoPago(),
                        f.pacienteNombre(),
                        f.pacienteDni(),
                        f.numeroAfiliado(),
                        f.plan(),
                        f.profesionalNombre() + " " + f.profesionalApellido(),
                        f.profesionalMatricula(),
                        f.servicioNombre(),
                        f.precioFinal(),
                        f.copago(),
                        f.numeroAutorizacion()))
                .toList();

        return new ReporteCoberturasResponse(desde, hasta, filas.size(), totalPrestaciones, totalCopagos, resumen, detalle);
    }
}

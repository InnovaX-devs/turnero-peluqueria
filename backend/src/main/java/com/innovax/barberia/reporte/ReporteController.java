package com.innovax.barberia.reporte;

import com.innovax.barberia.reporte.dto.ClientesInactivosResponse;
import com.innovax.barberia.reporte.dto.ReportesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Reportes (issue #25). El admin ve los de toda la barbería; un barbero ve solo los suyos
 * (el filtro lo aplica {@link ReporteService} con el barbero del token, nunca con un parámetro).
 */
@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasAnyRole('ADMIN', 'BARBERO')")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping
    public ReportesResponse generar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sucursal) {
        return reporteService.generar(desde, hasta, sucursal);
    }

    /** Clientes que no vuelven hace más de N días (para escribirles). No depende del período. */
    @GetMapping("/clientes-inactivos")
    public ClientesInactivosResponse clientesInactivos(@RequestParam(defaultValue = "45") int dias) {
        return reporteService.clientesInactivos(dias);
    }
}
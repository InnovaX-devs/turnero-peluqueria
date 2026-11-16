package com.innovax.barberia.cobertura;

import com.innovax.barberia.cobertura.dto.CoberturaRequests;
import com.innovax.barberia.cobertura.dto.CoberturaResponse;
import com.innovax.barberia.cobertura.dto.ReporteCoberturasResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Obras sociales y prepagas del negocio (issue #67).
 * Listar lo puede cualquiera del panel (recepción y profesionales la necesitan para cargar pacientes);
 * el ABM y el reporte de facturación son solo del admin.
 */
@RestController
@RequestMapping("/api/coberturas")
@RequiredArgsConstructor
public class CoberturaController {

    private final CoberturaService coberturaService;
    private final ReporteCoberturaService reporteService;

    /** {@code ?activas=true} devuelve solo las activas (para los selects). */
    @GetMapping
    public List<CoberturaResponse> listar(@RequestParam(defaultValue = "false") boolean activas) {
        return coberturaService.listar(activas);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CoberturaResponse crear(@Valid @RequestBody CoberturaRequests.Guardar request) {
        return coberturaService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CoberturaResponse renombrar(@PathVariable UUID id, @Valid @RequestBody CoberturaRequests.Guardar request) {
        return coberturaService.renombrar(id, request);
    }

    @PatchMapping("/{id}/activa")
    @PreAuthorize("hasRole('ADMIN')")
    public CoberturaResponse cambiarActiva(@PathVariable UUID id, @Valid @RequestBody CoberturaRequests.CambiarActiva request) {
        return coberturaService.cambiarActiva(id, request);
    }

    /** Carga la lista inicial común (PAMI, OSDE, Swiss Medical...). No duplica las que ya existen. */
    @PostMapping("/comunes")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CoberturaResponse> cargarComunes() {
        return coberturaService.cargarComunes();
    }

    /** Atenciones completadas por cobertura en el rango (ambos días incluidos), para facturar a mano. */
    @GetMapping("/reporte")
    @PreAuthorize("hasRole('ADMIN')")
    public ReporteCoberturasResponse reporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID cobertura) {
        return reporteService.generar(desde, hasta, cobertura);
    }
}

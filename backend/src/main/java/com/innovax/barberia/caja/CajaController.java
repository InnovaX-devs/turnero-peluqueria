package com.innovax.barberia.caja;

import com.innovax.barberia.caja.dto.CajaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

/** Caja diaria y mensual (issue #24). Solo el admin ve la plata. */
@RestController
@RequestMapping("/api/caja")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CajaController {

    private final CajaService cajaService;

    @GetMapping
    public CajaResponse consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sucursal) {
        return cajaService.consultar(desde, hasta, sucursal);
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sucursal) {
        byte[] csv = cajaService.exportarCsv(desde, hasta, sucursal).getBytes(StandardCharsets.UTF_8);
        String nombre = "caja_" + desde + "_" + hasta + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }
}

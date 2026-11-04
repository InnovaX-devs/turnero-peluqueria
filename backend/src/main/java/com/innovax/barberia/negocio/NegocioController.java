package com.innovax.barberia.negocio;

import com.innovax.barberia.negocio.dto.ActualizarNegocioRequest;
import com.innovax.barberia.negocio.dto.NegocioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Configuración de la barbería del usuario logueado. Solo admin. */
@RestController
@RequestMapping("/api/negocio")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NegocioController {

    private final NegocioService negocioService;

    @GetMapping
    public NegocioResponse obtener() {
        return negocioService.obtener();
    }

    @PutMapping
    public NegocioResponse actualizar(@Valid @RequestBody ActualizarNegocioRequest request) {
        return negocioService.actualizar(request);
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public NegocioResponse subirLogo(@RequestParam("archivo") MultipartFile archivo) {
        return negocioService.subirLogo(archivo);
    }

    @DeleteMapping("/logo")
    public NegocioResponse quitarLogo() {
        return negocioService.quitarLogo();
    }
}
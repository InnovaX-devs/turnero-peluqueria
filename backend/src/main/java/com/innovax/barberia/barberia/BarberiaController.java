package com.innovax.barberia.barberia;

import com.innovax.barberia.barberia.dto.ActualizarBarberiaRequest;
import com.innovax.barberia.barberia.dto.BarberiaResponse;
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
@RequestMapping("/api/barberia")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BarberiaController {

    private final BarberiaService barberiaService;

    @GetMapping
    public BarberiaResponse obtener() {
        return barberiaService.obtener();
    }

    @PutMapping
    public BarberiaResponse actualizar(@Valid @RequestBody ActualizarBarberiaRequest request) {
        return barberiaService.actualizar(request);
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BarberiaResponse subirLogo(@RequestParam("archivo") MultipartFile archivo) {
        return barberiaService.subirLogo(archivo);
    }

    @DeleteMapping("/logo")
    public BarberiaResponse quitarLogo() {
        return barberiaService.quitarLogo();
    }
}
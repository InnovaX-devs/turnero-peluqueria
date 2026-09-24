package com.innovax.barberia.atencion;

import com.innovax.barberia.atencion.dto.AtencionResponse;
import com.innovax.barberia.atencion.dto.CrearReservaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/reservas")
@RequiredArgsConstructor
public class AtencionController {

    private final AtencionService atencionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AtencionResponse reservar(@Valid @RequestBody CrearReservaRequest request) {
        return atencionService.reservarOnline(request);
    }
}
package com.innovax.barberia.atencion;

import com.innovax.barberia.atencion.dto.AtencionResponse;
import com.innovax.barberia.atencion.dto.OpcionesRegistroResponse;
import com.innovax.barberia.atencion.dto.RegistrarCorteRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Atenciones desde el panel (con login). La reserva pública sigue en AtencionController. */
@RestController
@RequestMapping("/api/atenciones")
@RequiredArgsConstructor
public class AtencionPanelController {

    private final RegistroCorteService registroCorteService;

    @GetMapping("/opciones")
    public OpcionesRegistroResponse opciones() {
        return registroCorteService.opciones();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AtencionResponse registrar(@Valid @RequestBody RegistrarCorteRequest request) {
        return registroCorteService.registrar(request);
    }
}
package com.innovax.barberia.listaespera;

import com.innovax.barberia.listaespera.dto.ListaEsperaRequests;
import com.innovax.barberia.listaespera.dto.ListaEsperaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Lista de espera del panel (issue #71). Requiere sesión; los permisos los resuelve el service. */
@RestController
@RequestMapping("/api/lista-espera")
@RequiredArgsConstructor
public class ListaEsperaController {

    private final ListaEsperaService service;

    @GetMapping
    public List<ListaEsperaResponse> listar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.listar(fecha);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListaEsperaResponse crear(@Valid @RequestBody ListaEsperaRequests.Crear request) {
        return service.crear(request);
    }

    @PatchMapping("/{id}/resuelto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolver(@PathVariable UUID id) {
        service.resolver(id);
    }
}

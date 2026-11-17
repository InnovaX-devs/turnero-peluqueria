package com.innovax.barberia.cliente;

import com.innovax.barberia.cliente.dto.AccesoPacienteResponse;
import com.innovax.barberia.cliente.dto.ActualizarCoberturaRequest;
import com.innovax.barberia.cliente.dto.ActualizarNotasRequest;
import com.innovax.barberia.cliente.dto.ClienteResponse;
import com.innovax.barberia.cliente.dto.CrearClienteRequest;
import com.innovax.barberia.cliente.dto.ExportacionPacienteResponse;
import com.innovax.barberia.cliente.dto.FichaClienteResponse;
import com.innovax.barberia.cliente.dto.PaginaClientesResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Clientes de la barbería del usuario logueado (admin y barberos). */
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;
    private final PacienteDatosService pacienteDatos;

    @GetMapping
    public List<ClienteResponse> buscar(@RequestParam(defaultValue = "") String q) {
        return clienteService.buscar(q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse crear(@Valid @RequestBody CrearClienteRequest request) {
        return clienteService.crear(request);
    }

    /**
     * Listado del panel (pantalla Clientes), paginado. {@code q} es opcional: nombre o parte del
     * teléfono. {@code pagina} arranca en 0; {@code tamano} es de hasta 100; {@code orden} es
     * "nombre" o "ultimaVisita".
     */
    @GetMapping("/listado")
    public PaginaClientesResponse listar(@RequestParam(defaultValue = "") String q,
                                         @RequestParam(defaultValue = "0") int pagina,
                                         @RequestParam(defaultValue = "50") int tamano,
                                         @RequestParam(defaultValue = "nombre") String orden) {
        return clienteService.listar(q, pagina, tamano, orden);
    }

    @GetMapping("/{id}")
    public FichaClienteResponse ficha(@PathVariable UUID id) {
        return clienteService.ficha(id);
    }

    @PatchMapping("/{id}/notas")
    public FichaClienteResponse actualizarNotas(@PathVariable UUID id, @Valid @RequestBody ActualizarNotasRequest request) {
        return clienteService.actualizarNotas(id, request);
    }

    /** Issue #67: obra social / prepaga, número de afiliado y plan del paciente. */
    @PatchMapping("/{id}/cobertura")
    public FichaClienteResponse actualizarCobertura(@PathVariable UUID id, @Valid @RequestBody ActualizarCoberturaRequest request) {
        return clienteService.actualizarCobertura(id, request);
    }

    /** Issue #73: quién vio o tocó los datos de este paciente (solo admin). */
    @GetMapping("/{id}/accesos")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AccesoPacienteResponse> accesos(@PathVariable UUID id) {
        return pacienteDatos.accesos(id);
    }

    /** Issue #73, derecho de acceso: todos los datos del paciente en un JSON descargable (solo admin). */
    @GetMapping("/{id}/exportar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExportacionPacienteResponse> exportar(@PathVariable UUID id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("datos-paciente-" + id + ".json").build().toString())
                .body(pacienteDatos.exportar(id));
    }

    /** Issue #73, derecho de supresión: borra los datos personales del paciente y conserva sus turnos sin identificarlo (solo admin). */
    @PostMapping("/{id}/anonimizar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void anonimizar(@PathVariable UUID id) {
        pacienteDatos.anonimizar(id);
    }
}

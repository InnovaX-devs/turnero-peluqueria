package com.innovax.barberia.cliente;

import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.cliente.dto.ActualizarNotasRequest;
import com.innovax.barberia.cliente.dto.ClienteListadoResponse;
import com.innovax.barberia.cliente.dto.ClienteResponse;
import com.innovax.barberia.cliente.dto.CrearClienteRequest;
import com.innovax.barberia.cliente.dto.FichaClienteResponse;
import com.innovax.barberia.cliente.dto.PaginaClientesResponse;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private static final int MAXIMO_RESULTADOS = 10;
    private static final int MAXIMO_POR_PAGINA = 100;
    static final String ORDEN_NOMBRE = "nombre";
    static final String ORDEN_ULTIMA_VISITA = "ultimaVisita";

    private final ClienteRepository clientes;
    private final BarberiaRepository barberias;
    private final AtencionRepository atenciones;
    private final UsuarioActual usuarioActual;

    @Transactional(readOnly = true)
    public List<ClienteResponse> buscar(String consulta) {
        String texto = consulta == null ? "" : consulta.trim();
        if (texto.length() < 2) {
            return List.of();
        }
        UUID barberiaId = usuarioActual.barberiaId();
        String digitos = texto.replaceAll("\\D", "");
        List<Cliente> encontrados = digitos.length() >= 3
                ? clientes.buscarPorNombreOTelefono(barberiaId, texto, digitos, PageRequest.of(0, MAXIMO_RESULTADOS))
                : clientes.findTop10ByBarberiaIdAndNombreContainingIgnoreCaseOrderByNombreAsc(barberiaId, texto);
        return encontrados.stream().map(ClienteResponse::de).toList();
    }

    @Transactional
    public ClienteResponse crear(CrearClienteRequest request) {
        UUID barberiaId = usuarioActual.barberiaId();
        clientes.findByBarberiaIdAndTelefono(barberiaId, request.telefono()).ifPresent(existente -> {
            throw new ConflictoException("telefono", "Ya existe un cliente con ese teléfono: " + existente.getNombre());
        });

        Cliente cliente = new Cliente();
        cliente.setBarberia(barberias.getReferenceById(barberiaId));
        cliente.setNombre(request.nombre().trim());
        cliente.setTelefono(request.telefono());
        try {
            clientes.saveAndFlush(cliente);
        } catch (DataIntegrityViolationException e) {
            // Otro usuario lo creó justo en el medio (la base tiene UNIQUE barberia + teléfono)
            throw new ConflictoException("telefono", "Ya existe un cliente con ese teléfono");
        }
        return ClienteResponse.de(cliente);
    }

    /**
     * Listado del panel, paginado. Sin texto trae todos; con texto filtra por nombre o teléfono.
     * {@code orden}: "nombre" (A-Z) o "ultimaVisita" (los que vinieron más recientemente primero).
     */
    @Transactional(readOnly = true)
    public PaginaClientesResponse listar(String consulta, int pagina, int tamano, String orden) {
        if (!ORDEN_NOMBRE.equals(orden) && !ORDEN_ULTIMA_VISITA.equals(orden)) {
            throw new DatosInvalidosException("orden", "El orden tiene que ser nombre o ultimaVisita");
        }
        int paginaPedida = Math.max(pagina, 0);
        int porPagina = Math.clamp(tamano, 1, MAXIMO_POR_PAGINA);

        String texto = consulta == null ? "" : consulta.trim();
        String digitos = texto.replaceAll("\\D", "");
        // Con 1 o 2 dígitos buscar por teléfono trae casi todo: ahí se busca solo por nombre
        String digitosParaTelefono = digitos.length() >= 3 ? digitos : "";
        UUID barberiaId = usuarioActual.barberiaId();

        long total = clientes.contarListado(barberiaId, texto, digitosParaTelefono);
        long desde = (long) paginaPedida * porPagina;
        List<Cliente> encontrados = ORDEN_ULTIMA_VISITA.equals(orden)
                ? clientes.listarPorUltimaVisita(barberiaId, texto, digitosParaTelefono, porPagina, desde)
                : clientes.listarPorNombre(barberiaId, texto, digitosParaTelefono, porPagina, desde);

        Map<UUID, Instant> ultimas = new HashMap<>();
        if (!encontrados.isEmpty()) {
            List<UUID> ids = encontrados.stream().map(Cliente::getId).toList();
            for (Object[] fila : clientes.ultimasVisitas(ids, EstadoAtencion.COMPLETADA)) {
                ultimas.put((UUID) fila[0], (Instant) fila[1]);
            }
        }
        List<ClienteListadoResponse> filas = encontrados.stream()
                .map(c -> ClienteListadoResponse.de(c, ultimas.get(c.getId())))
                .toList();
        return new PaginaClientesResponse(filas, total, paginaPedida, porPagina, desde + filas.size() < total);
    }

    @Transactional(readOnly = true)
    public FichaClienteResponse ficha(UUID id) {
        UUID barberiaId = usuarioActual.barberiaId();
        return armarFicha(buscarDeMiBarberia(id, barberiaId), barberiaId);
    }

    @Transactional
    public FichaClienteResponse actualizarNotas(UUID id, ActualizarNotasRequest request) {
        UUID barberiaId = usuarioActual.barberiaId();
        Cliente cliente = buscarDeMiBarberia(id, barberiaId);
        String notas = request.notas() == null ? "" : request.notas().strip();
        cliente.setNotas(notas.isEmpty() ? null : notas);
        clientes.save(cliente);
        return armarFicha(cliente, barberiaId);
    }

    /** Si el cliente es de otra barbería se responde igual que si no existiera. */
    private Cliente buscarDeMiBarberia(UUID id, UUID barberiaId) {
        return clientes.findByIdAndBarberiaId(id, barberiaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
    }

    private FichaClienteResponse armarFicha(Cliente cliente, UUID barberiaId) {
        return FichaClienteResponse.de(cliente, atenciones.findHistorialDeCliente(barberiaId, cliente.getId()));
    }
}
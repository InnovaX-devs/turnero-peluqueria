package com.innovax.barberia.cliente;

import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.negocio.Rubro;
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
    private final NegocioRepository negocios;
    private final AtencionRepository atenciones;
    private final UsuarioActual usuarioActual;

    @Transactional(readOnly = true)
    public List<ClienteResponse> buscar(String consulta) {
        String texto = consulta == null ? "" : consulta.trim();
        if (texto.length() < 2) {
            return List.of();
        }
        UUID negocioId = usuarioActual.negocioId();
        String digitos = texto.replaceAll("\\D", "");
        List<Cliente> encontrados = digitos.length() >= 3
                ? clientes.buscarPorNombreTelefonoODni(negocioId, texto, digitos, PageRequest.of(0, MAXIMO_RESULTADOS))
                : clientes.findTop10ByNegocioIdAndNombreContainingIgnoreCaseOrderByNombreAsc(negocioId, texto);
        return encontrados.stream().map(ClienteResponse::de).toList();
    }

    /**
     * Alta manual. En salud el paciente se identifica por DNI (obligatorio): dos pacientes pueden
     * compartir teléfono si tienen distinto DNI. En los demás rubros se identifica por teléfono y
     * el DNI, la fecha de nacimiento y el sexo se ignoran.
     */
    @Transactional
    public ClienteResponse crear(CrearClienteRequest request) {
        UUID negocioId = usuarioActual.negocioId();
        Negocio negocio = negocios.getReferenceById(negocioId);
        Rubro rubro = negocio.getRubro();

        Cliente cliente = new Cliente();
        cliente.setNegocio(negocio);
        cliente.setNombre(request.nombre().trim());
        cliente.setTelefono(request.telefono());

        String campoConflicto;
        if (rubro.identificaPorDni()) {
            String dni = Dni.normalizar(request.dni());
            if (dni == null) {
                throw new DatosInvalidosException("dni", "El DNI es obligatorio");
            }
            clientes.findByNegocioIdAndDni(negocioId, dni).ifPresent(existente -> {
                throw new ConflictoException("dni",
                        "Ya existe " + rubro.cliente().un() + " con ese DNI: " + existente.getNombre());
            });
            cliente.setDni(dni);
            cliente.setFechaNacimiento(request.fechaNacimiento());
            cliente.setSexo(request.sexo());
            campoConflicto = "dni";
        } else {
            clientes.findByNegocioIdAndTelefono(negocioId, request.telefono()).ifPresent(existente -> {
                throw new ConflictoException("telefono",
                        "Ya existe " + rubro.cliente().un() + " con ese teléfono: " + existente.getNombre());
            });
            campoConflicto = "telefono";
        }

        try {
            clientes.saveAndFlush(cliente);
        } catch (DataIntegrityViolationException e) {
            // Otro usuario lo creó justo en el medio (la base tiene UNIQUE negocio + DNI, o negocio + teléfono si no hay DNI)
            throw new ConflictoException(campoConflicto,
                    "Ya existe " + rubro.cliente().un() + " con ese " + ("dni".equals(campoConflicto) ? "DNI" : "teléfono"));
        }
        return ClienteResponse.de(cliente);
    }

    /**
     * Listado del panel, paginado. Sin texto trae todos; con texto filtra por nombre, teléfono o DNI.
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
        UUID negocioId = usuarioActual.negocioId();

        long total = clientes.contarListado(negocioId, texto, digitosParaTelefono);
        long desde = (long) paginaPedida * porPagina;
        List<Cliente> encontrados = ORDEN_ULTIMA_VISITA.equals(orden)
                ? clientes.listarPorUltimaVisita(negocioId, texto, digitosParaTelefono, porPagina, desde)
                : clientes.listarPorNombre(negocioId, texto, digitosParaTelefono, porPagina, desde);

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
        UUID negocioId = usuarioActual.negocioId();
        return armarFicha(buscarDeMiNegocio(id, negocioId), negocioId);
    }

    @Transactional
    public FichaClienteResponse actualizarNotas(UUID id, ActualizarNotasRequest request) {
        UUID negocioId = usuarioActual.negocioId();
        Cliente cliente = buscarDeMiNegocio(id, negocioId);
        String notas = request.notas() == null ? "" : request.notas().strip();
        cliente.setNotas(notas.isEmpty() ? null : notas);
        clientes.save(cliente);
        return armarFicha(cliente, negocioId);
    }

    /** Si el cliente es de otra barbería se responde igual que si no existiera. */
    private Cliente buscarDeMiNegocio(UUID id, UUID negocioId) {
        return clientes.findByIdAndNegocioId(id, negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
    }

    private FichaClienteResponse armarFicha(Cliente cliente, UUID negocioId) {
        return FichaClienteResponse.de(cliente, atenciones.findHistorialDeCliente(negocioId, cliente.getId()));
    }
}
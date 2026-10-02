package com.innovax.barberia.cliente;

import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.cliente.dto.ActualizarNotasRequest;
import com.innovax.barberia.cliente.dto.ClienteListadoResponse;
import com.innovax.barberia.cliente.dto.ClienteResponse;
import com.innovax.barberia.cliente.dto.CrearClienteRequest;
import com.innovax.barberia.cliente.dto.FichaClienteResponse;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private static final int MAXIMO_RESULTADOS = 10;
    private static final int MAXIMO_LISTADO = 200;

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

    /** Listado del panel: sin texto trae todos (hasta un máximo), con texto filtra por nombre o teléfono. */
    @Transactional(readOnly = true)
    public List<ClienteListadoResponse> listar(String consulta) {
        String texto = consulta == null ? "" : consulta.trim();
        String digitos = texto.replaceAll("\\D", "");
        // Con 1 o 2 dígitos buscar por teléfono trae casi todo: ahí se busca solo por nombre
        String digitosParaTelefono = digitos.length() >= 3 ? digitos : "";
        return clientes.listar(usuarioActual.barberiaId(), texto, digitosParaTelefono, PageRequest.of(0, MAXIMO_LISTADO))
                .stream()
                .map(ClienteListadoResponse::de)
                .toList();
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
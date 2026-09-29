package com.innovax.barberia.cliente;

import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.cliente.dto.ClienteResponse;
import com.innovax.barberia.cliente.dto.CrearClienteRequest;
import com.innovax.barberia.common.ConflictoException;
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

    private final ClienteRepository clientes;
    private final BarberiaRepository barberias;
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
}
package com.innovax.barberia.cliente;

import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.auditoria.AccesoPacienteRepository;
import com.innovax.barberia.auditoria.AccionPaciente;
import com.innovax.barberia.auditoria.AuditoriaPacienteService;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.cliente.dto.AccesoPacienteResponse;
import com.innovax.barberia.cliente.dto.ExportacionPacienteResponse;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.listaespera.ListaEsperaRepository;
import com.innovax.barberia.notificacion.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Derechos del titular de los datos (issue #73, Ley 25.326): acceso (exportar) y supresión (anonimizar),
 * más el registro de quién accedió a la ficha. Los endpoints son solo del admin del negocio: el negocio es
 * el responsable de la base y Innovax el encargado del tratamiento.
 *
 * <p>Supresión = anonimizar, no borrar la fila: los turnos y cobros tienen que seguir en la caja del negocio,
 * pero sin nada que identifique al paciente. Si el negocio tiene que conservar la historia clínica por otra
 * ley (Ley 26.529), eso es una decisión suya antes de pedir la supresión.
 */
@Service
@RequiredArgsConstructor
public class PacienteDatosService {

    private final ClienteRepository clientes;
    private final AtencionRepository atenciones;
    private final NotificacionRepository notificaciones;
    private final ListaEsperaRepository listaEspera;
    private final AccesoPacienteRepository accesos;
    private final AuditoriaPacienteService auditoria;
    private final UsuarioActual usuarioActual;

    @Transactional(readOnly = true)
    public List<AccesoPacienteResponse> accesos(UUID id) {
        UUID negocioId = usuarioActual.negocioId();
        buscarDeMiNegocio(id, negocioId);
        return accesos.findTop100ByNegocioIdAndClienteIdOrderByCreadoEnDesc(negocioId, id).stream()
                .map(AccesoPacienteResponse::de)
                .toList();
    }

    @Transactional
    public ExportacionPacienteResponse exportar(UUID id) {
        UUID negocioId = usuarioActual.negocioId();
        Cliente cliente = buscarDeMiNegocio(id, negocioId);
        auditoria.registrar(cliente.getId(), AccionPaciente.EXPORTAR);
        return ExportacionPacienteResponse.de(cliente, atenciones.findHistorialDeCliente(negocioId, id), Instant.now());
    }

    @Transactional
    public void anonimizar(UUID id) {
        UUID negocioId = usuarioActual.negocioId();
        Cliente cliente = buscarDeMiNegocio(id, negocioId);
        if (cliente.isAnonimizado()) {
            throw new ConflictoException("paciente", "Los datos de este paciente ya fueron borrados");
        }
        if (atenciones.existsByClienteIdAndEstado(id, EstadoAtencion.RESERVADA)) {
            throw new ConflictoException("paciente",
                    "Tiene turnos reservados: cancelalos o completalos antes de borrar sus datos");
        }

        auditoria.registrar(cliente.getId(), AccionPaciente.ANONIMIZAR);

        // Primero lo que cuelga del paciente (updates en bloque), después la ficha
        atenciones.borrarNotasDeCliente(id);
        notificaciones.omitirPendientesDeCliente(id);
        notificaciones.borrarContenidoDeCliente(id);
        listaEspera.borrarNotasDeCliente(id);

        cliente.anonimizar(Instant.now());
        clientes.save(cliente);
    }

    /** Si el paciente es de otro negocio se responde igual que si no existiera. */
    private Cliente buscarDeMiNegocio(UUID id, UUID negocioId) {
        return clientes.findByIdAndNegocioId(id, negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
    }
}

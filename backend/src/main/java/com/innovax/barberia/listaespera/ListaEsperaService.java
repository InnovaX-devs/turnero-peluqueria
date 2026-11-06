package com.innovax.barberia.listaespera;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.auth.UsuarioRepository;
import com.innovax.barberia.cliente.Cliente;
import com.innovax.barberia.cliente.ClienteRepository;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.listaespera.dto.ListaEsperaRequests;
import com.innovax.barberia.listaespera.dto.ListaEsperaResponse;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.profesional.Profesional;
import com.innovax.barberia.profesional.ProfesionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Lista de espera (issue #71): clientes anotados para un día en que no había lugar. Cuando se cancela
 * o se mueve un turno de ese día, se marca el aviso en los anotados que les sirve ese lugar, para que
 * recepción les escriba.
 *
 * <p>Permisos: admin y recepción ven y anotan para todos; un profesional, solo para él (y ve también
 * los anotados "con cualquiera").
 */
@Service
@RequiredArgsConstructor
public class ListaEsperaService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ListaEsperaRepository lista;
    private final NegocioRepository negocios;
    private final ClienteRepository clientes;
    private final ProfesionalRepository profesionales;
    private final UsuarioRepository usuarios;
    private final UsuarioActual usuarioActual;

    @Transactional(readOnly = true)
    public List<ListaEsperaResponse> listar(LocalDate fecha) {
        UUID propio = usuarioActual.veTodoElNegocio() ? null : profesionalPropio();
        return lista.findEsperando(usuarioActual.negocioId(), fecha).stream()
                .filter(l -> propio == null || l.getProfesional() == null || l.getProfesional().getId().equals(propio))
                .map(ListaEsperaResponse::de)
                .toList();
    }

    @Transactional
    public ListaEsperaResponse crear(ListaEsperaRequests.Crear request) {
        Negocio negocio = negocios.findById(usuarioActual.negocioId())
                .orElseThrow(() -> new IllegalStateException("El negocio del token no existe"));
        Cliente cliente = clientes.findByIdAndNegocioId(request.clienteId(), negocio.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));

        Profesional profesional = null;
        if (!usuarioActual.veTodoElNegocio()) {
            // Un profesional anota solo para él, sin importar lo que venga en el request
            profesional = buscarProfesional(negocio, profesionalPropio());
        } else if (request.profesionalId() != null) {
            profesional = buscarProfesional(negocio, request.profesionalId());
        }

        ListaEspera l = new ListaEspera();
        l.setNegocio(negocio);
        l.setCliente(cliente);
        l.setProfesional(profesional);
        l.setFecha(request.fecha());
        String nota = request.nota() == null ? "" : request.nota().trim();
        l.setNota(nota.isEmpty() ? null : nota);
        l.setCreadoPor(usuarios.getReferenceById(usuarioActual.id()));
        return ListaEsperaResponse.de(lista.saveAndFlush(l));
    }

    /** Ya se le dio turno o no hace falta más: sale de la lista. */
    @Transactional
    public void resolver(UUID id) {
        ListaEspera l = lista.findById(id)
                .filter(x -> x.getNegocio().getId().equals(usuarioActual.negocioId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("No está en la lista de espera"));
        if (!usuarioActual.veTodoElNegocio() && l.getProfesional() != null) {
            usuarioActual.verificarAccesoAProfesional(l.getProfesional().getId());
        }
        l.setEstado(EstadoListaEspera.RESUELTO);
    }

    /**
     * Se liberó el lugar de un turno (cancelado, o movido a otro horario): avisa a los anotados de ese día
     * con ese profesional o con cualquiera. Se llama desde la misma transacción que libera el turno.
     *
     * @param inicioLiberado el horario que quedó libre (al reprogramar, el viejo)
     */
    @Transactional
    public void avisarLugarLiberado(Atencion turno, Instant inicioLiberado) {
        if (inicioLiberado.isBefore(Instant.now())) {
            return; // un lugar que ya pasó no le sirve a nadie
        }
        Negocio negocio = turno.getNegocio();
        ZoneId zona = ZoneId.of(negocio.getZonaHoraria());
        LocalDate fecha = inicioLiberado.atZone(zona).toLocalDate();
        Profesional profesional = turno.getProfesional();
        String detalle = "Se liberó " + HORA.format(inicioLiberado.atZone(zona))
                + " con " + profesional.getNombre() + " " + profesional.getApellido();
        Instant ahora = Instant.now();
        for (ListaEspera l : lista.findEsperando(negocio.getId(), fecha)) {
            if (l.getProfesional() == null || l.getProfesional().getId().equals(profesional.getId())) {
                l.setAvisoEn(ahora);
                l.setAvisoDetalle(detalle);
            }
        }
    }

    private Profesional buscarProfesional(Negocio negocio, UUID id) {
        return profesionales.findById(id)
                .filter(p -> p.getNegocio().getId().equals(negocio.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Barbero no encontrado"));
    }

    private UUID profesionalPropio() {
        return usuarioActual.profesionalId()
                .orElseThrow(() -> new AccessDeniedException("Tu usuario no está vinculado a un barbero"));
    }
}

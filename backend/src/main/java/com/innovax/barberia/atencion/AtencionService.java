package com.innovax.barberia.atencion;

import com.innovax.barberia.agenda.dto.TurnoAgendaResponse;
import com.innovax.barberia.atencion.dto.AtencionResponse;
import com.innovax.barberia.atencion.dto.CambiarEstadoTurnoRequest;
import com.innovax.barberia.atencion.dto.CrearReservaRequest;
import com.innovax.barberia.atencion.dto.MarcarLlegadaRequest;
import com.innovax.barberia.auth.Usuario;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.auth.UsuarioRepository;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.profesional.Profesional;
import com.innovax.barberia.profesional.ProfesionalRepository;
import com.innovax.barberia.cliente.Cliente;
import com.innovax.barberia.cliente.Dni;
import com.innovax.barberia.cliente.ClienteRepository;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.consultorio.AsignadorConsultorio;
import com.innovax.barberia.consultorio.Consultorio;
import com.innovax.barberia.disponibilidad.DisponibilidadService;
import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.servicio.ServicioRepository;
import com.innovax.barberia.sucursal.Sucursal;
import com.innovax.barberia.sucursal.SucursalRepository;
import com.innovax.barberia.notificacion.NotificacionService;
import com.innovax.barberia.listaespera.ListaEsperaService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Reserva online (página pública, sin login). Épica 1: evitar que dos personas se queden
 * con el mismo horario.
 *
 * <p>Hay dos capas de protección:
 * <ol>
 *   <li>Re-chequeo de disponibilidad al principio de la transacción (rápido, buena UX en el
 *       caso común, pero no alcanza solo: entre el chequeo y el guardado sigue habiendo una
 *       ventana de carrera).</li>
 *   <li>La restricción de la base ({@code excl_atencion_superpuesta}, EXCLUDE USING gist),
 *       que es la que de verdad impide que dos atenciones activas del mismo barbero se
 *       solapen aunque dos requests pasen el chequeo anterior al mismo tiempo. Si la base la
 *       dispara, se traduce a un 409 con el mismo mensaje que el chequeo rápido. Con consultorio
 *       compartido (issue #69), {@code excl_atencion_consultorio} hace lo mismo entre profesionales distintos
 *       que atienden en el mismo consultorio.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class AtencionService {

    private static final String MENSAJE_HORARIO_OCUPADO = "Ese horario se acaba de ocupar";

    private final AtencionRepository atenciones;
    private final ClienteRepository clientes;
    private final SucursalRepository sucursales;
    private final ServicioRepository servicios;
    private final ProfesionalRepository profesionales;
    private final DisponibilidadService disponibilidadService;
    private final UsuarioActual usuarioActual;
    private final UsuarioRepository usuarios;
    private final NotificacionService notificaciones;
    private final ListaEsperaService listaEspera;
    private final AsignadorConsultorio asignadorConsultorio;

    @Transactional
    public AtencionResponse reservarOnline(CrearReservaRequest request) {
        Sucursal sucursal = sucursales.findById(request.sucursalId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
        Servicio servicio = servicios.findById(request.servicioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
        Profesional profesional = profesionales.findById(request.profesionalId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesional no encontrado"));

        Negocio negocio = sucursal.getNegocio();
        validarPertenecenAlMismoNegocio(negocio, servicio, profesional);

        if (!servicio.isActivo()) {
            throw new DatosInvalidosException("servicioId", negocio.getRubro().servicio().eseCapitalizado() + " ya no está disponible");
        }
        if (!negocio.isReservasOnline()) {
            throw new DatosInvalidosException("negocio",
                    negocio.getRubro().negocio().esteCapitalizado() + " no acepta reservas online por ahora");
        }
        if (!profesional.isActivo() || !profesional.getSucursales().contains(sucursal) || !profesional.getServicios().contains(servicio)) {
            throw new DatosInvalidosException("profesional", "No atiende " + negocio.getRubro().servicio().ese() + " en esa sucursal");
        }

        // 1) Chequeo rápido dentro de la transacción: mismo criterio que la grilla pública.
        if (!disponibilidadService.estaDisponible(sucursal.getId(), servicio.getId(), profesional.getId(),
                request.fecha(), request.hora())) {
            throw new ConflictoException("horario", MENSAJE_HORARIO_OCUPADO);
        }

        ZoneId zonaHoraria = ZoneId.of(negocio.getZonaHoraria());
        Instant inicio = ZonedDateTime.of(request.fecha(), request.hora(), zonaHoraria).toInstant();
        Instant fin = inicio.plus(Duration.ofMinutes(servicio.getDuracionMin()));

        Cliente cliente = obtenerOCrearCliente(negocio, request);

        Atencion atencion = new Atencion();
        atencion.setNegocio(negocio);
        atencion.setSucursal(sucursal);
        atencion.setProfesional(profesional);
        atencion.setCliente(cliente);
        atencion.setServicio(servicio);
        atencion.setInicio(inicio);
        atencion.setFin(fin);
        atencion.setEstado(EstadoAtencion.RESERVADA);
        atencion.setOrigen(OrigenAtencion.ONLINE);
        atencion.setPrecioFinal(servicio.getPrecio());
        atencion.setNotas(request.notas());
        atencion.setConsultorio(asignadorConsultorio.paraTurno(profesional.getId(), sucursal.getId(), inicio, zonaHoraria));

        try {
            // 2) Flush ahora, dentro de esta misma llamada: si otra transacción reservó este
            // mismo horario justo en el medio, la EXCLUDE constraint lo va a rechazar acá.
            atenciones.saveAndFlush(atencion);
        } catch (DataIntegrityViolationException e) {
            // Mismo mensaje si lo ganó otro turno del profesional o de otro profesional en el mismo consultorio:
            // para quien reserva es lo mismo, el horario ya no está.
            if ((e.getMessage() != null && e.getMessage().contains("excl_atencion_superpuesta"))
                    || Consultorio.esChoqueDeTurnos(e)) {
                throw new ConflictoException("horario", MENSAJE_HORARIO_OCUPADO);
            }
            throw e;
        }

        notificaciones.crearConfirmacion(atencion);

        return AtencionResponse.de(atencion);
    }

    /**
     * Cambia el estado de un turno desde el detalle del panel (marcar completado, ausente o
     * cancelado). Solo se puede cambiar un turno que está RESERVADA: una vez completado,
     * cancelado o marcado ausente, no se puede volver a tocar desde acá.
     *
     * <p>Al completar, {@code precioFinal} y {@code metodoPago} del request permiten ajustar el
     * precio (por si se agregó algo o se hizo un descuento) y registrar cómo pagó; para los demás
     * estados se ignoran. Siempre se guarda quién hizo el cambio y cuándo.
     *
     * <p>La respuesta se arma acá adentro, todavía con la transacción abierta: si se armara
     * después (en el controller), las asociaciones lazy de {@code atencion} (barbero, servicio,
     * cliente) ya no tendrían sesión de Hibernate para cargarse y tirarían
     * {@code LazyInitializationException}.
     */
    @Transactional
    public TurnoAgendaResponse cambiarEstado(UUID turnoId, CambiarEstadoTurnoRequest request) {
        Atencion atencion = atenciones.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));

        // Aislamiento entre barberías: si no es de la barbería del usuario logueado, se responde
        // igual que si no existiera (no se revela que el id pertenece a otra barbería).
        if (!atencion.getNegocio().getId().equals(usuarioActual.negocioId())) {
            throw new RecursoNoEncontradoException("Turno no encontrado");
        }
        usuarioActual.verificarAccesoAProfesional(atencion.getProfesional().getId());

        if (atencion.getEstado() != EstadoAtencion.RESERVADA) {
            throw new ConflictoException("estado", "Este turno ya no está reservado");
        }

        EstadoAtencion nuevoEstado = request.estado();
        if (nuevoEstado == EstadoAtencion.RESERVADA) {
            throw new DatosInvalidosException("estado", "No es un estado válido para actualizar un turno");
        }

        if (nuevoEstado == EstadoAtencion.COMPLETADA) {
            if (request.metodoPago() == null) {
                throw new DatosInvalidosException("metodoPago", "Es obligatorio para marcar el turno como completado");
            }
            if (request.precioFinal() != null) {
                atencion.setPrecioFinal(request.precioFinal());
            }
            atencion.setMetodoPago(request.metodoPago());
        }

        atencion.setEstado(nuevoEstado);
        if (nuevoEstado == EstadoAtencion.CANCELADA) {
            listaEspera.avisarLugarLiberado(atencion, atencion.getInicio());
        }

        Usuario usuario = usuarios.findById(usuarioActual.id())
                .orElseThrow(() -> new IllegalStateException("El usuario del token no existe"));
        atencion.setEstadoCambiadoPor(usuario);
        atencion.setEstadoCambiadoEn(Instant.now());

        return TurnoAgendaResponse.de(atenciones.save(atencion));
    }

    /**
     * Sala de espera (issue #71): marca que el cliente llegó al local, o lo desmarca si se tocó por error.
     * Solo para turnos RESERVADA; no cambia el estado ni el horario, así que no afecta disponibilidad,
     * recordatorios ni la restricción de turnos superpuestos. Mismos permisos que cambiar el estado.
     */
    @Transactional
    public TurnoAgendaResponse marcarLlegada(UUID turnoId, MarcarLlegadaRequest request) {
        Atencion atencion = atenciones.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));
        if (!atencion.getNegocio().getId().equals(usuarioActual.negocioId())) {
            throw new RecursoNoEncontradoException("Turno no encontrado");
        }
        usuarioActual.verificarAccesoAProfesional(atencion.getProfesional().getId());

        if (atencion.getEstado() != EstadoAtencion.RESERVADA) {
            throw new ConflictoException("estado", "Este turno ya no está reservado");
        }

        if (request.llego()) {
            if (atencion.getLlegadaEn() == null) {
                atencion.setLlegadaEn(Instant.now()); // si ya estaba marcado se respeta la hora original
            }
        } else {
            atencion.setLlegadaEn(null);
        }
        return TurnoAgendaResponse.de(atenciones.save(atencion));
    }

    /**
     * En salud el paciente se busca por DNI (obligatorio): el teléfono es solo un dato de contacto y
     * puede repetirse entre pacientes. En los demás rubros se busca por teléfono, como siempre.
     */
    private Cliente obtenerOCrearCliente(Negocio negocio, CrearReservaRequest request) {
        String telefono = request.clienteTelefono();
        Cliente cliente;
        if (negocio.getRubro().identificaPorDni()) {
            String dni = Dni.normalizar(request.clienteDni());
            if (dni == null) {
                throw new DatosInvalidosException("clienteDni", "El DNI es obligatorio");
            }
            cliente = clientes.findByNegocioIdAndDni(negocio.getId(), dni)
                    .orElseGet(() -> {
                        Cliente nuevo = new Cliente();
                        nuevo.setNegocio(negocio);
                        nuevo.setDni(dni);
                        return nuevo;
                    });
            cliente.setTelefono(telefono);
            if (request.clienteFechaNacimiento() != null) {
                cliente.setFechaNacimiento(request.clienteFechaNacimiento());
            }
            if (request.clienteSexo() != null) {
                cliente.setSexo(request.clienteSexo());
            }
        } else {
            cliente = clientes.findByNegocioIdAndTelefono(negocio.getId(), telefono)
                    .orElseGet(() -> {
                        Cliente nuevo = new Cliente();
                        nuevo.setNegocio(negocio);
                        nuevo.setTelefono(telefono);
                        return nuevo;
                    });
        }
        cliente.setNombre(request.clienteNombre().trim());
        return clientes.save(cliente);
    }

    private void validarPertenecenAlMismoNegocio(Negocio negocio, Servicio servicio, Profesional profesional) {
        if (!servicio.getNegocio().getId().equals(negocio.getId())) {
            throw new DatosInvalidosException("servicioId", "No pertenece " + negocio.getRubro().negocio().al() + " de la sucursal");
        }
        if (!profesional.getNegocio().getId().equals(negocio.getId())) {
            throw new DatosInvalidosException("profesionalId", "No pertenece " + negocio.getRubro().negocio().al() + " de la sucursal");
        }
    }
}
package com.innovax.barberia.atencion;

import com.innovax.barberia.atencion.dto.AtencionResponse;
import com.innovax.barberia.atencion.dto.OpcionesRegistroResponse;
import com.innovax.barberia.atencion.dto.RegistrarCorteRequest;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.barbero.Barbero;
import com.innovax.barberia.barbero.BarberoRepository;
import com.innovax.barberia.cliente.Cliente;
import com.innovax.barberia.cliente.ClienteRepository;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.servicio.ServicioRepository;
import com.innovax.barberia.sucursal.Sucursal;
import com.innovax.barberia.sucursal.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Registro de cortes en el local (cliente sin turno). Con reservas_online = false es la
 * función principal: todo tiene valores por defecto para que el caso común sea rápido.
 */
@Service
@RequiredArgsConstructor
public class RegistroCorteService {

    /** Margen para diferencias de reloj entre el celular y el servidor. */
    private static final Duration TOLERANCIA_FUTURO = Duration.ofMinutes(5);
    private static final Set<EstadoAtencion> ACTIVAS = EnumSet.of(EstadoAtencion.RESERVADA, EstadoAtencion.COMPLETADA);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final UsuarioActual usuarioActual;
    private final BarberiaRepository barberias;
    private final BarberoRepository barberos;
    private final ServicioRepository servicios;
    private final SucursalRepository sucursales;
    private final ClienteRepository clientes;
    private final AtencionRepository atenciones;

    @Transactional(readOnly = true)
    public OpcionesRegistroResponse opciones() {
        Barberia barberia = barberiaActual();
        UUID propio = usuarioActual.barberoId().orElse(null);
        // El admin elige entre todos; el barbero solo se ve a sí mismo
        List<Barbero> visibles = barberos.findByBarberiaIdAndActivoTrueOrderByNombreAscApellidoAsc(barberia.getId())
                .stream()
                .filter(b -> usuarioActual.esAdmin() || b.getId().equals(propio))
                .toList();
        return OpcionesRegistroResponse.de(barberia, propio,
                servicios.findByBarberiaIdAndActivoTrueOrderByOrdenAscNombreAsc(barberia.getId()),
                visibles,
                sucursales.findByBarberiaIdAndActivaTrueOrderByNombreAsc(barberia.getId()));
    }

    @Transactional
    public AtencionResponse registrar(RegistrarCorteRequest request) {
        Barberia barberia = barberiaActual();
        ZoneId zona = ZoneId.of(barberia.getZonaHoraria());

        Barbero barbero = resolverBarbero(barberia, request.barberoId());
        Sucursal sucursal = resolverSucursal(barberia, barbero, request.sucursalId());
        Servicio servicio = servicios.findById(request.servicioId())
                .filter(s -> s.getBarberia().getId().equals(barberia.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
        Cliente cliente = null;
        if (request.clienteId() != null) {
            cliente = clientes.findById(request.clienteId())
                    .filter(c -> c.getBarberia().getId().equals(barberia.getId()))
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
        }

        Instant inicio = resolverInicio(zona, request.fecha(), request.hora());
        Instant fin = acomodarSuperposiciones(barbero, zona, inicio,
                inicio.plus(Duration.ofMinutes(servicio.getDuracionMin())));

        Atencion atencion = new Atencion();
        atencion.setBarberia(barberia);
        atencion.setSucursal(sucursal);
        atencion.setBarbero(barbero);
        atencion.setCliente(cliente);
        atencion.setServicio(servicio);
        atencion.setInicio(inicio);
        atencion.setFin(fin);
        atencion.setEstado(EstadoAtencion.COMPLETADA);
        atencion.setOrigen(OrigenAtencion.MANUAL);
        atencion.setPrecioFinal(request.precioFinal());
        atencion.setMetodoPago(request.metodoPago());
        atencion.setNotas(request.notas() == null || request.notas().isBlank() ? null : request.notas().trim());

        try {
            atenciones.saveAndFlush(atencion);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("excl_atencion_superpuesta")) {
                throw new ConflictoException("horario", "Ese barbero ya tiene otra atención en ese horario");
            }
            throw e;
        }
        return AtencionResponse.de(atencion);
    }

    /** Barbero logueado: siempre él mismo. Admin: el que elija (o él mismo si también corta). */
    private Barbero resolverBarbero(Barberia barberia, UUID barberoPedido) {
        UUID barberoId;
        if (!usuarioActual.esAdmin()) {
            barberoId = usuarioActual.barberoId()
                    .orElseThrow(() -> new AccessDeniedException("El usuario no está vinculado a un barbero"));
            if (barberoPedido != null) {
                usuarioActual.verificarAccesoABarbero(barberoPedido); // 403 si pide otro
            }
        } else if (barberoPedido != null) {
            barberoId = barberoPedido;
        } else {
            barberoId = usuarioActual.barberoId()
                    .orElseThrow(() -> new DatosInvalidosException("barberoId", "Elegí el barbero"));
        }
        return barberos.findById(barberoId)
                .filter(b -> b.getBarberia().getId().equals(barberia.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Barbero no encontrado"));
    }

    /** Si el barbero trabaja en una sola sucursal, no hace falta elegirla. */
    private Sucursal resolverSucursal(Barberia barberia, Barbero barbero, UUID sucursalId) {
        if (sucursalId == null) {
            List<Sucursal> activas = barbero.getSucursales().stream().filter(Sucursal::isActiva).toList();
            if (activas.size() != 1) {
                throw new DatosInvalidosException("sucursalId", "Elegí la sucursal");
            }
            return activas.get(0);
        }
        Sucursal sucursal = sucursales.findById(sucursalId)
                .filter(s -> s.getBarberia().getId().equals(barberia.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
        if (!barbero.getSucursales().contains(sucursal)) {
            throw new DatosInvalidosException("sucursalId", "Ese barbero no trabaja en esa sucursal");
        }
        return sucursal;
    }

    /** Sin fecha ni hora: ahora. Con fecha y hora: se permiten cortes pasados, no futuros. */
    private Instant resolverInicio(ZoneId zona, LocalDate fecha, LocalTime hora) {
        Instant ahora = Instant.now();
        if (fecha == null && hora == null) {
            return ahora;
        }
        if (fecha == null || hora == null) {
            throw new DatosInvalidosException("fecha", "Indicá la fecha y la hora");
        }
        
        return ZonedDateTime.of(fecha, hora, zona).toInstant();
    }

    /**
     * La base no permite dos atenciones activas superpuestas del mismo barbero. Un corte real
     * suele durar menos que la duración nominal del servicio, así que dos cortes seguidos
     * chocarían. Se acomodan así:
     * - una atención COMPLETADA que empezó antes termina cuando empieza esta;
     * - si hay una atención que empieza después, esta termina cuando empieza aquella;
     * - un turno RESERVADO que empezó antes y sigue pendiente es un conflicto real: 409.
     */
    private Instant acomodarSuperposiciones(Barbero barbero, ZoneId zona, Instant inicio, Instant fin) {
        Instant finAjustado = fin;
        for (Atencion otra : atenciones.findOcupadas(barbero.getId(), ACTIVAS, inicio, fin)) {
            if (otra.getInicio().equals(inicio)) {
                throw new ConflictoException("horario", "Ese barbero ya tiene una atención registrada a esa misma hora");
            }
            if (otra.getInicio().isAfter(inicio)) {
                if (otra.getInicio().isBefore(finAjustado)) {
                    finAjustado = otra.getInicio();
                }
            } else if (otra.getEstado() == EstadoAtencion.COMPLETADA) {
                otra.setFin(inicio);
            } else {
                throw new ConflictoException("horario", "Hay un turno reservado a las "
                        + HORA.format(otra.getInicio().atZone(zona))
                        + " que todavía no se marcó como atendido o ausente");
            }
        }
        // Se guardan los recortes antes de insertar el corte nuevo: si no, la base ve la superposición
        atenciones.flush();
        return finAjustado;
    }

    private Barberia barberiaActual() {
        return barberias.findById(usuarioActual.barberiaId())
                .orElseThrow(() -> new IllegalStateException("La barbería del token no existe"));
    }
}
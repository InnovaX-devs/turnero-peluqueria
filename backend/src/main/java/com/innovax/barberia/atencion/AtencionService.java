package com.innovax.barberia.atencion;

import com.innovax.barberia.atencion.dto.AtencionResponse;
import com.innovax.barberia.atencion.dto.CrearReservaRequest;
import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.barbero.Barbero;
import com.innovax.barberia.barbero.BarberoRepository;
import com.innovax.barberia.cliente.Cliente;
import com.innovax.barberia.cliente.ClienteRepository;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.disponibilidad.DisponibilidadService;
import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.servicio.ServicioRepository;
import com.innovax.barberia.sucursal.Sucursal;
import com.innovax.barberia.sucursal.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

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
 *       dispara, se traduce a un 409 con el mismo mensaje que el chequeo rápido.</li>
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
    private final BarberoRepository barberos;
    private final DisponibilidadService disponibilidadService;

    @Transactional
    public AtencionResponse reservarOnline(CrearReservaRequest request) {
        Sucursal sucursal = sucursales.findById(request.sucursalId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
        Servicio servicio = servicios.findById(request.servicioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));
        Barbero barbero = barberos.findById(request.barberoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Barbero no encontrado"));

        Barberia barberia = sucursal.getBarberia();
        validarPertenecenALaMismaBarberia(barberia, servicio, barbero);

        if (!barberia.isReservasOnline()) {
            throw new DatosInvalidosException("barberia", "Esta barbería no acepta reservas online por ahora");
        }
        if (!barbero.isActivo() || !barbero.getSucursales().contains(sucursal) || !barbero.getServicios().contains(servicio)) {
            throw new DatosInvalidosException("barbero", "No atiende ese servicio en esa sucursal");
        }

        // 1) Chequeo rápido dentro de la transacción: mismo criterio que la grilla pública.
        if (!disponibilidadService.estaDisponible(sucursal.getId(), servicio.getId(), barbero.getId(),
                request.fecha(), request.hora())) {
            throw new ConflictoException("horario", MENSAJE_HORARIO_OCUPADO);
        }

        ZoneId zonaHoraria = ZoneId.of(barberia.getZonaHoraria());
        Instant inicio = ZonedDateTime.of(request.fecha(), request.hora(), zonaHoraria).toInstant();
        Instant fin = inicio.plus(Duration.ofMinutes(servicio.getDuracionMin()));

        Cliente cliente = obtenerOCrearCliente(barberia, request.clienteNombre(), request.clienteTelefono());

        Atencion atencion = new Atencion();
        atencion.setBarberia(barberia);
        atencion.setSucursal(sucursal);
        atencion.setBarbero(barbero);
        atencion.setCliente(cliente);
        atencion.setServicio(servicio);
        atencion.setInicio(inicio);
        atencion.setFin(fin);
        atencion.setEstado(EstadoAtencion.RESERVADA);
        atencion.setOrigen(OrigenAtencion.ONLINE);
        atencion.setPrecioFinal(servicio.getPrecio());
        atencion.setNotas(request.notas());

        try {
            // 2) Flush ahora, dentro de esta misma llamada: si otra transacción reservó este
            // mismo horario justo en el medio, la EXCLUDE constraint lo va a rechazar acá.
            atenciones.saveAndFlush(atencion);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("excl_atencion_superpuesta")) {
                throw new ConflictoException("horario", MENSAJE_HORARIO_OCUPADO);
            }
            throw e;
        }

        return AtencionResponse.de(atencion);
    }

    private Cliente obtenerOCrearCliente(Barberia barberia, String nombre, String telefono) {
        Cliente cliente = clientes.findByBarberiaIdAndTelefono(barberia.getId(), telefono)
                .orElseGet(() -> {
                    Cliente nuevo = new Cliente();
                    nuevo.setBarberia(barberia);
                    nuevo.setTelefono(telefono);
                    return nuevo;
                });
        cliente.setNombre(nombre.trim());
        return clientes.save(cliente);
    }

    private void validarPertenecenALaMismaBarberia(Barberia barberia, Servicio servicio, Barbero barbero) {
        if (!servicio.getBarberia().getId().equals(barberia.getId())) {
            throw new DatosInvalidosException("servicioId", "No pertenece a la barbería de la sucursal");
        }
        if (!barbero.getBarberia().getId().equals(barberia.getId())) {
            throw new DatosInvalidosException("barberoId", "No pertenece a la barbería de la sucursal");
        }
    }
}
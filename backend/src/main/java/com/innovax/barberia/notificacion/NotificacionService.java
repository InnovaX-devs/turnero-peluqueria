package com.innovax.barberia.notificacion;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.common.ConflictoException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.notificacion.dto.NotificacionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Crea y procesa las notificaciones al cliente: la confirmación (al reservar) y el recordatorio
 * (X horas antes, según {@code barberia.recordatorioHorasAntes}).
 *
 * <p>El recordatorio se crea recién cuando le toca, no al reservar: así, si el turno se
 * cancela o se reprograma antes, no queda ningún recordatorio viejo con la hora equivocada.
 *
 * <p>Con un canal manual (link wa.me) el mensaje queda PENDIENTE hasta que una persona lo manda
 * desde el panel y lo marca. Con un canal automático el job lo manda y lo marca ENVIADA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    /** Tope de horas configurable; coincide con el CHECK de la migración V6. */
    private static final int MAX_HORAS_RECORDATORIO = 168;

    private final NotificacionRepository notificaciones;
    private final MensajeBuilder mensajes;
    private final CanalMensajeria canal;
    private final NotificacionLinks links;
    private final UsuarioActual usuarioActual;

    // ---------- Lo que dispara el sistema ----------

    /**
     * Confirmación al reservar. Se llama dentro de la transacción de la reserva; si algo falla
     * armando el mensaje, se registra el error pero la reserva no se cae.
     */
    @Transactional
    public void crearConfirmacion(Atencion atencion) {
        if (atencion.getCliente() == null
                || notificaciones.existsByAtencionIdAndTipo(atencion.getId(), TipoNotificacion.CONFIRMACION)) {
            return;
        }
        try {
            Instant ahora = Instant.now();
            Notificacion n = nueva(atencion, TipoNotificacion.CONFIRMACION,
                    mensajes.confirmacion(atencion), ahora);
            notificaciones.save(n);
            procesar(n, ahora);
        } catch (RuntimeException e) {
            log.warn("No se pudo crear la confirmación del turno {}", atencion.getId(), e);
        }
    }

    /**
     * Lo que ejecuta el job: crea los recordatorios que ya les toca y procesa todo lo pendiente.
     *
     * @return cuántas notificaciones cambiaron de estado
     */
    @Transactional
    public int procesarPendientes() {
        Instant ahora = Instant.now();
        generarRecordatorios(ahora);

        List<Notificacion> pendientes = notificaciones
                .findByEstadoAndProgramadaParaLessThanEqual(EstadoNotificacion.PENDIENTE, ahora);
        int cambios = 0;
        for (Notificacion n : pendientes) {
            if (procesar(n, ahora)) {
                cambios++;
            }
        }
        return cambios;
    }

    // ---------- Lo que usa el panel ----------

    /** Mensajes en el estado dado. El admin ve los de toda la barbería; un barbero, solo los de sus turnos. */
    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(EstadoNotificacion estado) {
        UUID barberoId = null;
        if (!usuarioActual.esAdmin()) {
            barberoId = usuarioActual.barberoId()
                    .orElseThrow(() -> new AccessDeniedException("Tu usuario no está vinculado a un barbero"));
        }
        return notificaciones.findParaPanel(usuarioActual.barberiaId(), estado, barberoId).stream()
                .map(this::respuesta)
                .toList();
    }

    /** Una persona ya mandó el mensaje (abrió el link y lo envió): se registra como ENVIADA. */
    @Transactional
    public NotificacionResponse marcarEnviada(UUID id) {
        Notificacion n = notificaciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje no encontrado"));

        // Aislamiento entre barberías: se responde igual que si no existiera.
        if (!n.getBarberia().getId().equals(usuarioActual.barberiaId())) {
            throw new RecursoNoEncontradoException("Mensaje no encontrado");
        }
        usuarioActual.verificarAccesoABarbero(n.getAtencion().getBarbero().getId());

        if (n.getEstado() != EstadoNotificacion.PENDIENTE) {
            throw new ConflictoException("estado", "Este mensaje ya no está pendiente");
        }
        if (n.getAtencion().getEstado() != EstadoAtencion.RESERVADA) {
            throw new ConflictoException("turno", "El turno ya no está reservado");
        }

        n.setEstado(EstadoNotificacion.ENVIADA);
        n.setEnviadaEn(Instant.now());
        n.setError(null);
        return respuesta(n);
    }

    // ---------- Internos ----------

    private void generarRecordatorios(Instant ahora) {
        Instant limite = ahora.plus(Duration.ofHours(MAX_HORAS_RECORDATORIO));
        List<Atencion> candidatos = notificaciones.findSinNotificacion(
                EstadoAtencion.RESERVADA, TipoNotificacion.RECORDATORIO, ahora, limite);

        for (Atencion a : candidatos) {
            Instant cuando = a.getInicio().minus(Duration.ofHours(a.getBarberia().getRecordatorioHorasAntes()));
            if (cuando.isAfter(ahora)) {
                continue; // todavía no toca
            }
            Notificacion n = nueva(a, TipoNotificacion.RECORDATORIO, mensajes.recordatorio(a), cuando);
            if (a.getCreatedAt().isAfter(cuando)) {
                // Se reservó dentro de la ventana del recordatorio: ya recibió la confirmación,
                // otro mensaje seguido sería redundante. Queda registrado como omitido.
                n.setEstado(EstadoNotificacion.OMITIDA);
                n.setError("Se reservó dentro de la ventana del recordatorio");
            }
            notificaciones.save(n);
        }
    }

    /**
     * Revisa una notificación pendiente.
     *
     * @return true si cambió de estado; false si sigue PENDIENTE esperando que una persona la mande
     */
    private boolean procesar(Notificacion n, Instant ahora) {
        Atencion atencion = n.getAtencion();
        if (atencion.getEstado() != EstadoAtencion.RESERVADA) {
            return cerrar(n, EstadoNotificacion.OMITIDA, "El turno ya no está reservado");
        }
        if (!atencion.getInicio().isAfter(ahora)) {
            return cerrar(n, EstadoNotificacion.OMITIDA, "El turno ya empezó");
        }

        ResultadoEnvio resultado = intentar(() -> canal.validar(n));
        if (resultado.exitoso() && canal.esAutomatico()) {
            resultado = intentar(() -> canal.enviar(n));
        }
        if (!resultado.exitoso()) {
            return cerrar(n, EstadoNotificacion.FALLIDA, resultado.error());
        }
        if (!canal.esAutomatico()) {
            return false; // sigue PENDIENTE hasta que una persona la mande desde el panel
        }
        n.setEstado(EstadoNotificacion.ENVIADA);
        n.setEnviadaEn(ahora);
        n.setError(null);
        return true;
    }

    private static boolean cerrar(Notificacion n, EstadoNotificacion estado, String error) {
        n.setEstado(estado);
        n.setError(recortar(error, 500));
        return true;
    }

    private static ResultadoEnvio intentar(Supplier<ResultadoEnvio> accion) {
        try {
            return accion.get();
        } catch (RuntimeException e) {
            return ResultadoEnvio.fallo(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    private Notificacion nueva(Atencion a, TipoNotificacion tipo, String mensaje, Instant programadaPara) {
        Notificacion n = new Notificacion();
        n.setBarberia(a.getBarberia());
        n.setAtencion(a);
        n.setTipo(tipo);
        n.setCanal(canal.tipo());
        n.setDestinatario(a.getCliente().getTelefono());
        n.setMensaje(mensaje);
        n.setProgramadaPara(programadaPara);
        return n;
    }

    private NotificacionResponse respuesta(Notificacion n) {
        String link = n.getCanal() == TipoCanal.WHATSAPP_LINK
                ? links.whatsApp(n.getDestinatario(), n.getMensaje())
                : null;
        return NotificacionResponse.de(n, link);
    }

    private static String recortar(String texto, int max) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
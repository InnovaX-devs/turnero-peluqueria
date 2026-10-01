package com.innovax.barberia.notificacion;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.EstadoAtencion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Crea y procesa las notificaciones al cliente: la confirmación (al reservar) y el recordatorio
 * (X horas antes, según {@code barberia.recordatorioHorasAntes}).
 *
 * <p>El recordatorio se crea recién cuando le toca, no al reservar: así, si el turno se
 * cancela o se reprograma antes, no queda ningún recordatorio viejo con la hora equivocada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    /** Tope de horas configurable; coincide con el CHECK de la migración V5. */
    private static final int MAX_HORAS_RECORDATORIO = 168;

    private final NotificacionRepository notificaciones;
    private final MensajeBuilder mensajes;
    private final CanalMensajeria canal;

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
     * @return cuántas notificaciones se procesaron
     */
    @Transactional
    public int procesarPendientes() {
        Instant ahora = Instant.now();
        generarRecordatorios(ahora);

        List<Notificacion> pendientes = notificaciones
                .findByEstadoAndProgramadaParaLessThanEqual(EstadoNotificacion.PENDIENTE, ahora);
        pendientes.forEach(n -> procesar(n, ahora));
        return pendientes.size();
    }

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

    private void procesar(Notificacion n, Instant ahora) {
        Atencion atencion = n.getAtencion();
        if (atencion.getEstado() != EstadoAtencion.RESERVADA) {
            n.setEstado(EstadoNotificacion.OMITIDA);
            n.setError("El turno ya no está reservado");
            return;
        }
        if (!atencion.getInicio().isAfter(ahora)) {
            n.setEstado(EstadoNotificacion.OMITIDA);
            n.setError("El turno ya empezó");
            return;
        }

        ResultadoEnvio resultado;
        try {
            resultado = canal.enviar(n);
        } catch (RuntimeException e) {
            resultado = ResultadoEnvio.fallo(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }

        if (resultado.exitoso()) {
            n.setEstado(EstadoNotificacion.ENVIADA);
            n.setEnviadaEn(ahora);
            n.setError(null);
        } else {
            n.setEstado(EstadoNotificacion.FALLIDA);
            n.setError(recortar(resultado.error(), 500));
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

    private static String recortar(String texto, int max) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
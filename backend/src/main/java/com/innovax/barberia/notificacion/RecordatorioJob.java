package com.innovax.barberia.notificacion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cada minuto crea los recordatorios que ya les toca y procesa las notificaciones pendientes. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecordatorioJob {

    private final NotificacionService notificaciones;

    @Scheduled(
            initialDelayString = "${app.notificaciones.demora-inicial-ms:30000}",
            fixedDelayString = "${app.notificaciones.intervalo-ms:60000}")
    public void ejecutar() {
        try {
            int procesadas = notificaciones.procesarPendientes();
            if (procesadas > 0) {
                log.info("Notificaciones procesadas: {}", procesadas);
            }
        } catch (RuntimeException e) {
            // Que un error no frene las próximas ejecuciones
            log.error("Falló el job de recordatorios", e);
        }
    }
}
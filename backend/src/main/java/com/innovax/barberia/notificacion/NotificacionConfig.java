package com.innovax.barberia.notificacion;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita los jobs programados (por ahora, el de recordatorios). */
@Configuration
@EnableScheduling
public class NotificacionConfig {
}
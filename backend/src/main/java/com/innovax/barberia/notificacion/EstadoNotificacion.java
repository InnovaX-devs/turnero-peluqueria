package com.innovax.barberia.notificacion;

public enum EstadoNotificacion {
    PENDIENTE,
    ENVIADA,
    FALLIDA,
    /** No correspondía enviarla (por ejemplo, el turno se canceló). */
    OMITIDA
}
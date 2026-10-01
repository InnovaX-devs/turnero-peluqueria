package com.innovax.barberia.notificacion;

import com.innovax.barberia.notificacion.dto.NotificacionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Mensajes al cliente desde el panel (con login): ver los pendientes y marcarlos como enviados. */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificaciones;

    @GetMapping
    public List<NotificacionResponse> listar(@RequestParam(defaultValue = "PENDIENTE") EstadoNotificacion estado) {
        return notificaciones.listar(estado);
    }

    @PostMapping("/{id}/enviada")
    public NotificacionResponse marcarEnviada(@PathVariable UUID id) {
        return notificaciones.marcarEnviada(id);
    }
}
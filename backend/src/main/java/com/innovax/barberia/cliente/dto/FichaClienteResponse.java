package com.innovax.barberia.cliente.dto;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.cliente.Cliente;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ficha del cliente: datos, notas, cantidad de ausencias e historial de atenciones
 * (de la más reciente a la más antigua).
 */
public record FichaClienteResponse(
        UUID id,
        String nombre,
        String telefono,
        String notas,
        Instant creadoEn,
        long ausencias,
        List<AtencionHistorial> historial
) {

    /** Una atención del historial. Incluye todos los estados para que se vean también ausencias y cancelaciones. */
    public record AtencionHistorial(
            UUID id,
            Instant inicio,
            EstadoAtencion estado,
            String barberoNombre,
            String servicioNombre,
            long precioFinal
    ) {
        public static AtencionHistorial de(Atencion a) {
            return new AtencionHistorial(
                    a.getId(),
                    a.getInicio(),
                    a.getEstado(),
                    a.getBarbero().getNombre() + " " + a.getBarbero().getApellido(),
                    a.getServicio().getNombre(),
                    a.getPrecioFinal());
        }
    }

    public static FichaClienteResponse de(Cliente cliente, List<Atencion> atenciones) {
        long ausencias = atenciones.stream().filter(a -> a.getEstado() == EstadoAtencion.AUSENTE).count();
        return new FichaClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getTelefono(),
                cliente.getNotas(),
                cliente.getCreatedAt(),
                ausencias,
                atenciones.stream().map(AtencionHistorial::de).toList());
    }
}
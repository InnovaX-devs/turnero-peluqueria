package com.innovax.barberia.atencion.gestion.dto;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.negocio.Rubro;

import java.time.Instant;
import java.util.UUID;

/**
 * Lo que ve el cliente en /turno/{token}: el detalle de su turno y qué puede hacer.
 * No expone ids internos del turno ni datos de otros clientes.
 */
public record TurnoPublicoResponse(
        UUID token,
        EstadoAtencion estado,
        Instant inicio,
        Instant fin,
        String clienteNombre,
        long precio,
        Negocio negocio,
        Sucursal sucursal,
        Servicio servicio,
        Profesional profesional,
        /** Minutos antes del turno en que se deja de poder cancelar o reprogramar. */
        int anticipacionCancelacionMin,
        /** Hasta cuándo se puede cancelar o reprogramar online. */
        Instant limiteModificacion,
        boolean puedeCancelar,
        /** Además de estar en plazo: reserva online prendida, y profesional y servicio todavía activos. */
        boolean puedeReprogramar) {

    public record Negocio(String nombre, String slug, Rubro rubro) {
    }

    public record Sucursal(UUID id, String nombre, String direccion, String ciudad, String telefono) {
    }

    public record Servicio(UUID id, String nombre, int duracionMin) {
    }

    public record Profesional(UUID id, String nombre) {
    }

    public static TurnoPublicoResponse de(Atencion a, Instant ahora) {
        var b = a.getNegocio();
        Instant limite = a.getInicio().minusSeconds(b.getAnticipacionCancelacionMin() * 60L);
        boolean puedeCancelar = a.getEstado() == EstadoAtencion.RESERVADA && ahora.isBefore(limite);
        boolean puedeReprogramar = puedeCancelar && b.isReservasOnline()
                && a.getProfesional().isActivo() && a.getServicio().isActivo();
        var s = a.getSucursal();
        return new TurnoPublicoResponse(
                a.getTokenGestion(),
                a.getEstado(),
                a.getInicio(),
                a.getFin(),
                a.getCliente() == null ? null : a.getCliente().getNombre(),
                a.getPrecioFinal(),
                new Negocio(b.getNombre(), b.getSlug(), b.getRubro()),
                new Sucursal(s.getId(), s.getNombre(), s.getDireccion(), s.getCiudad(), s.getTelefono()),
                new Servicio(a.getServicio().getId(), a.getServicio().getNombre(), a.getServicio().getDuracionMin()),
                new Profesional(a.getProfesional().getId(), a.getProfesional().getNombre() + " " + a.getProfesional().getApellido()),
                b.getAnticipacionCancelacionMin(),
                limite,
                puedeCancelar,
                puedeReprogramar);
    }
}

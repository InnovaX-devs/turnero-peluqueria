package com.innovax.barberia.agenda.dto;

import com.innovax.barberia.cobertura.Cobertura;
import com.innovax.barberia.cobertura.TipoPago;
import com.innovax.barberia.agenda.EstadoPago;
import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.MetodoPago;
import com.innovax.barberia.atencion.OrigenAtencion;

import java.time.Instant;
import java.util.UUID;

/**
 * Un turno tal como lo ve el panel: a diferencia de {@link com.innovax.barberia.atencion.dto.AtencionResponse}
 * (que es la respuesta pública de la reserva online), acá sí va el teléfono del cliente, porque
 * lo usa el barbero/admin para escribirle por WhatsApp desde el detalle del turno.
 */
public record TurnoAgendaResponse(
        UUID id,
        Instant inicio,
        Instant fin,
        EstadoAtencion estado,
        OrigenAtencion origen,
        EstadoPago estadoPago,
        MetodoPago metodoPago,
        UUID profesionalId,
        String profesionalNombre,
        UUID servicioId,
        String servicioNombre,
        long precioFinal,
        /** Null = corte anónimo (cargado a mano sin cliente asociado). */
        UUID clienteId,
        String clienteNombre,
        String clienteTelefono,
        /** DNI del paciente. Null fuera de salud (ahí el cliente no tiene DNI) o si el turno no tiene cliente. */
        String clienteDni,
        /** Nota de la ficha del cliente (ej. "degradé en 2, arriba tijera"), para tenerla a mano al atenderlo. */
        String clienteNotas,
        /** Nota de este turno en particular (la que dejó quien reservó). */
        String notas,
        /** Quién marcó el turno como completado/ausente/cancelado. Null = nunca se cambió. */
        String estadoCambiadoPorEmail,
        Instant estadoCambiadoEn,
        /** Cuándo llegó el cliente (sala de espera). Null = todavía no llegó. */
        Instant llegadaEn,
        /** Cargado a propósito encima de otro turno o fuera de horario. */
        boolean sobreturno,
        /** Consultorio donde se atiende (issue #69). Null = sin consultorio. */
        String consultorioNombre,
        /** Issue #67: cómo se paga (PARTICULAR fuera de salud). */
        TipoPago tipoPago,
        /** Obra social / prepaga del turno. Null = particular. */
        String coberturaNombre,
        /** Centavos. Solo COBERTURA_CON_COPAGO. */
        Long copago,
        String numeroAutorizacion,
        /** Cobertura que tiene cargada el paciente en su ficha (puede no ser la del turno). */
        UUID clienteCoberturaId,
        String clienteCoberturaNombre,
        String clienteNumeroAfiliado,
        String clientePlan
) {

    public static TurnoAgendaResponse de(Atencion a) {
        return new TurnoAgendaResponse(
                a.getId(),
                a.getInicio(),
                a.getFin(),
                a.getEstado(),
                a.getOrigen(),
                estadoPago(a),
                a.getMetodoPago(),
                a.getProfesional().getId(),
                a.getProfesional().getNombre() + " " + a.getProfesional().getApellido(),
                a.getServicio().getId(),
                a.getServicio().getNombre(),
                a.getPrecioFinal(),
                a.getCliente() != null ? a.getCliente().getId() : null,
                a.getCliente() != null ? a.getCliente().getNombre() : null,
                a.getCliente() != null ? a.getCliente().getTelefono() : null,
                a.getCliente() != null ? a.getCliente().getDni() : null,
                a.getCliente() != null ? a.getCliente().getNotas() : null,
                a.getNotas(),
                a.getEstadoCambiadoPor() != null ? a.getEstadoCambiadoPor().getEmail() : null,
                a.getEstadoCambiadoEn(),
                a.getLlegadaEn(),
                a.isSobreturno(),
                a.getConsultorio() != null ? a.getConsultorio().getNombre() : null,
                a.getTipoPago(),
                a.getCobertura() != null ? a.getCobertura().getNombre() : null,
                a.getCopago(),
                a.getNumeroAutorizacion(),
                coberturaCliente(a) != null ? coberturaCliente(a).getId() : null,
                coberturaCliente(a) != null ? coberturaCliente(a).getNombre() : null,
                a.getCliente() != null ? a.getCliente().getNumeroAfiliado() : null,
                a.getCliente() != null ? a.getCliente().getPlan() : null);
    }

    /** Pagado: se cobró algo, o lo cubre entero la obra social (no hay nada que cobrar). */
    private static EstadoPago estadoPago(Atencion a) {
        boolean cubiertoEntero = a.getEstado() == EstadoAtencion.COMPLETADA && a.getTipoPago() == TipoPago.COBERTURA;
        return a.getMetodoPago() != null || cubiertoEntero ? EstadoPago.PAGADO : EstadoPago.PENDIENTE;
    }

    private static Cobertura coberturaCliente(Atencion a) {
        return a.getCliente() != null ? a.getCliente().getCobertura() : null;
    }
}
package com.innovax.barberia.cliente.dto;

import com.innovax.barberia.atencion.Atencion;
import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.cliente.Cliente;
import com.innovax.barberia.cliente.Sexo;
import com.innovax.barberia.cobertura.TipoPago;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Todos los datos que el sistema tiene de un paciente (issue #73, derecho de acceso de la Ley 25.326).
 * Incluye las notas de consulta: por eso solo la puede pedir el admin.
 */
public record ExportacionPacienteResponse(
        Instant exportadoEn,
        Paciente paciente,
        List<Turno> turnos
) {

    public record Paciente(
            UUID id,
            String nombre,
            String telefono,
            String dni,
            LocalDate fechaNacimiento,
            Sexo sexo,
            String notas,
            String obraSocial,
            String numeroAfiliado,
            String plan,
            Instant creadoEn,
            Instant consentimientoEn,
            String consentimientoVersion
    ) {
    }

    public record Turno(
            UUID id,
            Instant inicio,
            EstadoAtencion estado,
            String profesional,
            String servicio,
            long precioFinal,
            TipoPago tipoPago,
            String obraSocial,
            Long copago,
            String notasDeConsulta
    ) {
        static Turno de(Atencion a) {
            return new Turno(
                    a.getId(),
                    a.getInicio(),
                    a.getEstado(),
                    a.getProfesional().getNombre() + " " + a.getProfesional().getApellido(),
                    a.getServicio().getNombre(),
                    a.getPrecioFinal(),
                    a.getTipoPago(),
                    a.getCobertura() != null ? a.getCobertura().getNombre() : null,
                    a.getCopago(),
                    a.getNotas());
        }
    }

    public static ExportacionPacienteResponse de(Cliente c, List<Atencion> atenciones, Instant ahora) {
        return new ExportacionPacienteResponse(
                ahora,
                new Paciente(
                        c.getId(),
                        c.getNombre(),
                        c.getTelefono(),
                        c.getDni(),
                        c.getFechaNacimiento(),
                        c.getSexo(),
                        c.getNotas(),
                        c.getCobertura() != null ? c.getCobertura().getNombre() : null,
                        c.getNumeroAfiliado(),
                        c.getPlan(),
                        c.getCreatedAt(),
                        c.getConsentimientoEn(),
                        c.getConsentimientoVersion()),
                atenciones.stream().map(Turno::de).toList());
    }
}

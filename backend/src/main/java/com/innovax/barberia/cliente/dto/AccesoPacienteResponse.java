package com.innovax.barberia.cliente.dto;

import com.innovax.barberia.auditoria.AccesoPaciente;
import com.innovax.barberia.auditoria.AccionPaciente;

import java.time.Instant;

/** Una fila del registro de accesos a un paciente (issue #73). */
public record AccesoPacienteResponse(
        String usuarioEmail,
        AccionPaciente accion,
        Instant fecha
) {

    public static AccesoPacienteResponse de(AccesoPaciente a) {
        return new AccesoPacienteResponse(a.getUsuarioEmail(), a.getAccion(), a.getCreadoEn());
    }
}

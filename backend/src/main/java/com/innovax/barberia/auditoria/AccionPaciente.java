package com.innovax.barberia.auditoria;

/** Qué se hizo con los datos de un paciente (issue #73). Los nombres se guardan tal cual en la base. */
public enum AccionPaciente {
    VER_FICHA,
    EDITAR_NOTAS,
    EDITAR_COBERTURA,
    EXPORTAR,
    ANONIMIZAR
}

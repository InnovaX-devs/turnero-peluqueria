-- =====================================================================
-- V18: auditoría de acceso a datos de pacientes (issue #73, Ley 25.326).
--
-- Una fila por cada vez que un usuario del panel abre la ficha de un paciente,
-- le edita notas o cobertura, exporta sus datos o los anonimiza.
--
-- A propósito SIN claves foráneas: el registro de auditoría tiene que sobrevivir
-- a las filas que describe (un usuario dado de baja, un paciente anonimizado, un
-- negocio borrado). Por eso guarda también un snapshot del email del usuario.
-- =====================================================================

CREATE TABLE acceso_paciente (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id    UUID         NOT NULL,
    usuario_id    UUID         NOT NULL,
    usuario_email VARCHAR(255) NOT NULL,
    cliente_id    UUID         NOT NULL,
    accion        VARCHAR(20)  NOT NULL,
    creado_en     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_acceso_paciente_accion
        CHECK (accion IN ('VER_FICHA', 'EDITAR_NOTAS', 'EDITAR_COBERTURA', 'EXPORTAR', 'ANONIMIZAR'))
);
CREATE INDEX idx_acceso_paciente_cliente ON acceso_paciente (cliente_id, creado_en DESC);
CREATE INDEX idx_acceso_paciente_negocio ON acceso_paciente (negocio_id, creado_en DESC);

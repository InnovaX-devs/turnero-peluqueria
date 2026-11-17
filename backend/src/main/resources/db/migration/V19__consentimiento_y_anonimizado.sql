-- =====================================================================
-- V19: consentimiento del paciente y supresión de datos (issue #73, Ley 25.326).
--
--   * consentimiento_en / consentimiento_version: cuándo y con qué versión de los
--     términos aceptó el tratamiento de sus datos al reservar online (solo salud).
--     NULL = no hay consentimiento registrado (pacientes anteriores a esta migración,
--     o cargados a mano desde el panel).
--   * anonimizado_en: se completa cuando el negocio borra los datos personales del
--     paciente a pedido suyo. La fila y sus turnos se conservan (caja y contabilidad),
--     pero sin ningún dato que identifique a la persona.
-- =====================================================================

ALTER TABLE cliente
    ADD COLUMN consentimiento_en      TIMESTAMPTZ,
    ADD COLUMN consentimiento_version VARCHAR(20),
    ADD COLUMN anonimizado_en         TIMESTAMPTZ;

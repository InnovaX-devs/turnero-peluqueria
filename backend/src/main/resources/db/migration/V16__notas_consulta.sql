-- =====================================================================
-- V16: notas de consulta (issue #72).
--   * Se reutiliza atencion.notas (no se crea otra columna ni otra tabla).
--   * notas_editadas_por / notas_editadas_en: auditoría de quién y cuándo
--     escribió o editó por última vez la nota. NULL = nadie la tocó desde el
--     panel (por ejemplo, la dejó el cliente al reservar online).
--   * Quién puede leerlas lo resuelve el backend (admin y el profesional que
--     atendió), no la base.
-- =====================================================================

ALTER TABLE atencion
    ADD COLUMN notas_editadas_por UUID        REFERENCES usuario (id),
    ADD COLUMN notas_editadas_en  TIMESTAMPTZ;

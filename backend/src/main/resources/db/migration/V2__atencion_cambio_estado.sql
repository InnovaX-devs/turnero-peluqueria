-- =====================================================================
-- V2: cambio de estado de un turno (issue #17)
--   * estado_cambiado_por / estado_cambiado_en: auditoría de quién y cuándo
--     marcó el turno como completado, ausente o cancelado.
-- =====================================================================

ALTER TABLE atencion
    ADD COLUMN estado_cambiado_por UUID        REFERENCES usuario (id),
    ADD COLUMN estado_cambiado_en  TIMESTAMPTZ;
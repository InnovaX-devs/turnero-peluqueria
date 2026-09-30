-- =====================================================================
-- V4: anticipación mínima para cancelar o reprogramar (issue #14)
--   Separada de la anticipación mínima para reservar: muchas barberías
--   dejan reservar con 1 hora pero piden avisar 2 o 3 horas antes para
--   cancelar. Las barberías existentes quedan con 2 horas.
-- =====================================================================

ALTER TABLE barberia
    ADD COLUMN anticipacion_cancelacion_min INTEGER NOT NULL DEFAULT 120,
    ADD CONSTRAINT chk_barberia_anticipacion_cancelacion
        CHECK (anticipacion_cancelacion_min BETWEEN 0 AND 10080);

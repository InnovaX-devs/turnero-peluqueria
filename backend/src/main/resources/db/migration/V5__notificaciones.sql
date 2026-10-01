-- =====================================================================
-- V5: notificaciones al cliente (confirmación al reservar y recordatorio)
--   * barberia.recordatorio_horas_antes: con cuánta anticipación se manda el
--     recordatorio. Default 24 h; las barberías existentes quedan con 24.
--   * notificacion: un registro por mensaje (pendiente, enviado, fallido u omitido).
--     UNIQUE (atencion_id, tipo) evita duplicar la confirmación o el recordatorio
--     de un mismo turno aunque el job corra dos veces.
-- =====================================================================

ALTER TABLE barberia
    ADD COLUMN recordatorio_horas_antes INTEGER NOT NULL DEFAULT 24,
    ADD CONSTRAINT chk_barberia_recordatorio_horas CHECK (recordatorio_horas_antes BETWEEN 1 AND 168);

CREATE TABLE notificacion (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    barberia_id    UUID          NOT NULL REFERENCES barberia (id),
    atencion_id    UUID          NOT NULL REFERENCES atencion (id) ON DELETE CASCADE,
    tipo           VARCHAR(20)   NOT NULL,
    canal          VARCHAR(20)   NOT NULL,
    estado         VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    destinatario   VARCHAR(255)  NOT NULL,                       -- teléfono o email
    mensaje        VARCHAR(2000) NOT NULL,
    programada_para TIMESTAMPTZ  NOT NULL,
    enviada_en     TIMESTAMPTZ,
    error          VARCHAR(500),                                  -- motivo si quedó FALLIDA u OMITIDA
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT chk_notificacion_tipo   CHECK (tipo   IN ('CONFIRMACION', 'RECORDATORIO')),
    CONSTRAINT chk_notificacion_canal  CHECK (canal  IN ('WHATSAPP_LINK', 'EMAIL')),
    CONSTRAINT chk_notificacion_estado CHECK (estado IN ('PENDIENTE', 'ENVIADA', 'FALLIDA', 'OMITIDA')),
    CONSTRAINT uq_notificacion_atencion_tipo UNIQUE (atencion_id, tipo)
);
CREATE INDEX idx_notificacion_pendientes ON notificacion (programada_para) WHERE estado = 'PENDIENTE';
CREATE INDEX idx_notificacion_barberia   ON notificacion (barberia_id, created_at DESC);
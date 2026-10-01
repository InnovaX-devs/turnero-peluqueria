-- =====================================================================
-- V5: tokens para restablecer la contraseña del personal por email
--   * Se guarda solo el SHA-256 del token (hex): si se filtra la base,
--     los links que andan dando vueltas no sirven.
--   * Un token es de un solo uso (usado_en) y vence a la hora (expira_en).
--   * Si se borra el usuario, se borran sus tokens.
-- =====================================================================

CREATE TABLE token_recuperacion (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expira_en   TIMESTAMPTZ NOT NULL,
    usado_en    TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_token_recuperacion_usuario ON token_recuperacion (usuario_id);
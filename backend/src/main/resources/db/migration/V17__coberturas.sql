-- =====================================================================
-- V17: obras sociales y prepagas (issue #67).
--
--   * cobertura: las que trabaja cada negocio (PAMI, OSDE, ...). Baja lógica con
--     activa = false, para no perder las atenciones que ya la usan.
--   * profesional_cobertura: qué coberturas atiende cada profesional. A los
--     particulares los atienden todos.
--   * cliente: cobertura del paciente (sin cobertura = particular).
--   * atencion: cómo se paga. Las atenciones existentes quedan como PARTICULAR.
--     cobertura_id se guarda también en la atención para que el reporte de
--     facturación no cambie si el paciente cambia de obra social más adelante.
-- =====================================================================

CREATE TABLE cobertura (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id  UUID         NOT NULL REFERENCES negocio (id),
    nombre      VARCHAR(80)  NOT NULL,
    activa      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_cobertura_nombre CHECK (btrim(nombre) <> '')
);
-- No puede haber dos coberturas activas con el mismo nombre en un negocio
CREATE UNIQUE INDEX uq_cobertura_nombre_activa ON cobertura (negocio_id, lower(nombre)) WHERE activa;

CREATE TABLE profesional_cobertura (
    profesional_id UUID NOT NULL REFERENCES profesional (id) ON DELETE CASCADE,
    cobertura_id   UUID NOT NULL REFERENCES cobertura (id)   ON DELETE CASCADE,
    PRIMARY KEY (profesional_id, cobertura_id)
);
CREATE INDEX idx_profesional_cobertura_cobertura ON profesional_cobertura (cobertura_id);

ALTER TABLE cliente
    ADD COLUMN cobertura_id    UUID REFERENCES cobertura (id),
    ADD COLUMN numero_afiliado VARCHAR(40),
    ADD COLUMN plan            VARCHAR(60);

ALTER TABLE atencion
    ADD COLUMN tipo_pago           VARCHAR(25) NOT NULL DEFAULT 'PARTICULAR',
    ADD COLUMN cobertura_id        UUID REFERENCES cobertura (id),
    ADD COLUMN copago              BIGINT,
    ADD COLUMN numero_autorizacion VARCHAR(60),
    ADD CONSTRAINT chk_atencion_tipo_pago
        CHECK (tipo_pago IN ('PARTICULAR', 'COBERTURA', 'COBERTURA_CON_COPAGO')),
    -- particular <=> sin cobertura
    ADD CONSTRAINT chk_atencion_cobertura
        CHECK ((tipo_pago = 'PARTICULAR') = (cobertura_id IS NULL)),
    -- el copago (en centavos) solo existe en COBERTURA_CON_COPAGO
    ADD CONSTRAINT chk_atencion_copago
        CHECK (copago IS NULL OR (tipo_pago = 'COBERTURA_CON_COPAGO' AND copago >= 0));

CREATE INDEX idx_atencion_cobertura ON atencion (negocio_id, cobertura_id, inicio) WHERE cobertura_id IS NOT NULL;

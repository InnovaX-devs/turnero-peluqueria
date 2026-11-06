-- =====================================================================
-- V11: lista de espera (issue #71). Clientes que quieren turno un día en que no hay lugar.
-- Tabla nueva: no toca ninguna existente.
--
-- profesional_id NULL = con cualquier profesional.
-- aviso_en / aviso_detalle: se completan cuando se cancela o se mueve un turno de ese día
-- (y profesional), para que recepción vea que se liberó un lugar.
-- =====================================================================

CREATE TABLE lista_espera (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id     UUID          NOT NULL REFERENCES negocio (id),
    cliente_id     UUID          NOT NULL REFERENCES cliente (id),
    profesional_id UUID          REFERENCES profesional (id),
    fecha          DATE          NOT NULL,
    nota           VARCHAR(500),
    estado         VARCHAR(20)   NOT NULL DEFAULT 'ESPERANDO',
    aviso_en       TIMESTAMPTZ,
    aviso_detalle  VARCHAR(200),
    creado_por     UUID          REFERENCES usuario (id),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT chk_lista_espera_estado CHECK (estado IN ('ESPERANDO', 'RESUELTO'))
);
CREATE INDEX idx_lista_espera_negocio_fecha ON lista_espera (negocio_id, fecha);

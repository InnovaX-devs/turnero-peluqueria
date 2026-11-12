-- =====================================================================
-- V14: matrícula y especialidades del profesional (issue #68).
--
-- Pensado para el rubro SALUD: el paciente puede reservar buscando una especialidad
-- ("Kinesiología") sin conocer el nombre del profesional. En los demás rubros las
-- columnas/tablas existen pero no se usan (matrícula null, sin especialidades).
-- =====================================================================

ALTER TABLE profesional
    ADD COLUMN matricula VARCHAR(30);

-- Especialidades propias de cada negocio (cada consultorio arma su lista)
CREATE TABLE especialidad (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id UUID        NOT NULL REFERENCES negocio (id),
    nombre     VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_especialidad_nombre CHECK (btrim(nombre) <> '')
);
-- Sin repetidos por negocio, sin importar mayúsculas ("kinesiología" = "Kinesiología")
CREATE UNIQUE INDEX uq_especialidad_nombre ON especialidad (negocio_id, lower(nombre));

-- N a N: un profesional puede tener varias especialidades y una especialidad varios profesionales.
-- Borrar una especialidad la saca de los profesionales (no hay historial que dependa de ella).
CREATE TABLE profesional_especialidad (
    profesional_id  UUID NOT NULL REFERENCES profesional (id)  ON DELETE CASCADE,
    especialidad_id UUID NOT NULL REFERENCES especialidad (id) ON DELETE CASCADE,
    PRIMARY KEY (profesional_id, especialidad_id)
);
CREATE INDEX idx_profesional_especialidad_especialidad ON profesional_especialidad (especialidad_id);

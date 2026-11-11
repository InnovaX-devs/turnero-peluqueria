-- =====================================================================
-- V13: datos personales del cliente / paciente (issue #66).
--
-- En los rubros que no son salud el cliente se sigue identificando por teléfono. En SALUD se
-- identifica por DNI, así que dos pacientes pueden compartir teléfono (ej. madre e hijo) si
-- tienen distinto DNI.
--
-- Por eso la unicidad (negocio, teléfono) pasa a valer solo para los clientes SIN DNI, que son
-- todos los de los rubros no-salud (en SALUD el DNI es obligatorio). La unicidad (negocio, DNI)
-- vale cuando el DNI no es null.
-- =====================================================================

ALTER TABLE cliente
    ADD COLUMN dni              VARCHAR(8),
    ADD COLUMN fecha_nacimiento DATE,
    ADD COLUMN sexo             VARCHAR(10),
    ADD CONSTRAINT chk_cliente_dni  CHECK (dni ~ '^[0-9]{7,8}$'),
    ADD CONSTRAINT chk_cliente_sexo CHECK (sexo IN ('FEMENINO', 'MASCULINO', 'OTRO'));

ALTER TABLE cliente DROP CONSTRAINT uq_cliente_telefono;

CREATE UNIQUE INDEX uq_cliente_telefono_sin_dni ON cliente (negocio_id, telefono) WHERE dni IS NULL;
CREATE UNIQUE INDEX uq_cliente_dni              ON cliente (negocio_id, dni)      WHERE dni IS NOT NULL;

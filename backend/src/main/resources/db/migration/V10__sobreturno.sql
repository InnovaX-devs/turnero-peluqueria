-- =====================================================================
-- V10: sobreturnos (issue #71).
--
-- Un sobreturno es un turno que recepción o el admin cargan a propósito encima de otro
-- (o fuera del horario del profesional). Para permitirlo, la regla que impide turnos
-- superpuestos deja afuera a los sobreturnos. Entre turnos NORMALES la regla queda
-- exactamente igual que antes: mismas columnas, mismos estados, mismo nombre (el código
-- reconoce el error por el nombre "excl_atencion_superpuesta").
--
-- Todo corre en una sola transacción (Flyway + Postgres): nunca queda la tabla sin regla.
-- Los datos existentes ya cumplían la regla anterior, que es más estricta, así que cumplen
-- la nueva.
-- =====================================================================

ALTER TABLE atencion ADD COLUMN sobreturno BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE atencion DROP CONSTRAINT excl_atencion_superpuesta;
ALTER TABLE atencion ADD CONSTRAINT excl_atencion_superpuesta EXCLUDE USING gist (
    profesional_id WITH =,
    tstzrange(inicio, fin) WITH &&
) WHERE (estado IN ('RESERVADA', 'COMPLETADA') AND NOT sobreturno);

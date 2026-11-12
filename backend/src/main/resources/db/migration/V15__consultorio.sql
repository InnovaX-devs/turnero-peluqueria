-- =====================================================================
-- V15: consultorios como recurso compartido (issue #69).
--
-- Pensado para el rubro SALUD: varios profesionales usan los mismos consultorios en
-- distintos horarios ("los martes de 9 a 13 atiende en el Consultorio 2"). Dos
-- profesionales no pueden usar el mismo consultorio a la misma hora.
--
-- Todo es opcional: un horario sin consultorio funciona exactamente igual que antes, y en
-- los demás rubros las columnas quedan en null. Ningún dato existente cambia.
--
-- Hay dos reglas en la base (además de las validaciones del backend, que dan mensajes claros):
--   * excl_horario_consultorio: dos horarios no pueden ocupar el mismo consultorio el mismo
--     día de la semana en tramos superpuestos.
--   * excl_atencion_consultorio: dos turnos activos de profesionales DISTINTOS no pueden
--     estar en el mismo consultorio a la misma hora. Entre turnos del mismo profesional
--     manda excl_atencion_superpuesta (que permite sobreturnos), igual que antes.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Consultorio (de una sucursal)
-- ---------------------------------------------------------------------
CREATE TABLE consultorio (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sucursal_id UUID        NOT NULL REFERENCES sucursal (id),
    nombre      VARCHAR(80) NOT NULL,
    activo      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_consultorio_nombre CHECK (btrim(nombre) <> ''),
    -- Para que las FK de abajo puedan exigir que el consultorio sea de la misma sucursal
    CONSTRAINT uq_consultorio_id_sucursal UNIQUE (id, sucursal_id)
);
-- Sin repetidos por sucursal, sin importar mayúsculas ("consultorio 2" = "Consultorio 2")
CREATE UNIQUE INDEX uq_consultorio_nombre ON consultorio (sucursal_id, lower(nombre));

-- ---------------------------------------------------------------------
-- Horario laboral: consultorio opcional
-- ---------------------------------------------------------------------
ALTER TABLE horario_laboral ADD COLUMN consultorio_id UUID;

-- FK compuesta: el consultorio tiene que ser de la sucursal del horario.
-- Con consultorio_id null no se chequea (MATCH SIMPLE): horario sin consultorio.
ALTER TABLE horario_laboral ADD CONSTRAINT fk_horario_consultorio
    FOREIGN KEY (consultorio_id, sucursal_id) REFERENCES consultorio (id, sucursal_id);

-- Dos horarios no pueden usar el mismo consultorio el mismo día en tramos superpuestos.
-- Las horas se llevan a una fecha fija para poder usar un rango ([inicio, fin), medio abierto:
-- un tramo que termina 13:00 y otro que empieza 13:00 no chocan).
ALTER TABLE horario_laboral ADD CONSTRAINT excl_horario_consultorio EXCLUDE USING gist (
    consultorio_id WITH =,
    dia_semana     WITH =,
    tsrange(DATE '2000-01-01' + hora_inicio, DATE '2000-01-01' + hora_fin) WITH &&
) WHERE (consultorio_id IS NOT NULL);

-- ---------------------------------------------------------------------
-- Atención: en qué consultorio se atiende (se copia del horario al dar el turno)
-- ---------------------------------------------------------------------
ALTER TABLE atencion ADD COLUMN consultorio_id UUID;

ALTER TABLE atencion ADD CONSTRAINT fk_atencion_consultorio
    FOREIGN KEY (consultorio_id, sucursal_id) REFERENCES consultorio (id, sucursal_id);

-- No puede haber dos turnos activos simultáneos en el mismo consultorio con profesionales distintos.
-- Aplica también a los sobreturnos: un sobreturno puede pisar un turno del mismo profesional,
-- pero nunca meterse en el consultorio de otro.
ALTER TABLE atencion ADD CONSTRAINT excl_atencion_consultorio EXCLUDE USING gist (
    consultorio_id WITH =,
    profesional_id WITH <>,
    tstzrange(inicio, fin) WITH &&
) WHERE (estado IN ('RESERVADA', 'COMPLETADA') AND consultorio_id IS NOT NULL);

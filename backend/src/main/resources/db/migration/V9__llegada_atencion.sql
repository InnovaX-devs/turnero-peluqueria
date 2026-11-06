-- =====================================================================
-- V9: sala de espera (issue #71). Hora en que el cliente llegó al local.
-- NULL = todavía no llegó (o el turno es anterior a esta versión). No cambia ningún
-- estado ni la restricción de turnos superpuestos: "en sala" = RESERVADA + llegada_en.
-- =====================================================================

ALTER TABLE atencion ADD COLUMN llegada_en TIMESTAMPTZ;

-- =====================================================================
-- V12: rubro del negocio (issue #65). Define los términos que se muestran en el
-- panel, la página pública y los mensajes (barbero / profesional, cliente / paciente...).
--
-- Los negocios existentes quedan como BARBERIA, así que no cambia nada de lo que ven hoy.
-- =====================================================================

ALTER TABLE negocio
    ADD COLUMN rubro VARCHAR(20) NOT NULL DEFAULT 'BARBERIA',
    ADD CONSTRAINT chk_negocio_rubro CHECK (rubro IN ('BARBERIA', 'PELUQUERIA', 'ESTETICA', 'SALUD'));

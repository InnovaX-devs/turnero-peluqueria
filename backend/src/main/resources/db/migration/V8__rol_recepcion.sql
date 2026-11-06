-- =====================================================================
-- V8: rol RECEPCION (issue #70). Secretaría / recepción: da turnos para todos los
-- profesionales, pero no ve configuración, caja ni reportes.
-- Solo se amplía la restricción: los usuarios ADMIN y PROFESIONAL existentes no cambian.
-- =====================================================================

ALTER TABLE usuario DROP CONSTRAINT chk_usuario_rol;
ALTER TABLE usuario ADD CONSTRAINT chk_usuario_rol CHECK (rol IN ('ADMIN', 'PROFESIONAL', 'RECEPCION'));

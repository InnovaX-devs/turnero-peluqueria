-- =====================================================================
-- V7: dominio genérico (issue #64). barbería -> negocio, barbero -> profesional.
--
-- Solo cambian nombres: los ids y los datos quedan igual, así que los links de
-- reserva (/b/{slug}) y de gestión de turno (/turno/{token}) ya enviados siguen
-- andando. El único dato que cambia es el rol BARBERO -> PROFESIONAL.
-- =====================================================================

-- ---------- Tablas ----------
ALTER TABLE barberia         RENAME TO negocio;
ALTER TABLE barbero          RENAME TO profesional;
ALTER TABLE barbero_sucursal RENAME TO profesional_sucursal;
ALTER TABLE barbero_servicio RENAME TO profesional_servicio;

-- ---------- Columnas barberia_id -> negocio_id ----------
ALTER TABLE usuario      RENAME COLUMN barberia_id TO negocio_id;
ALTER TABLE sucursal     RENAME COLUMN barberia_id TO negocio_id;
ALTER TABLE profesional  RENAME COLUMN barberia_id TO negocio_id;
ALTER TABLE servicio     RENAME COLUMN barberia_id TO negocio_id;
ALTER TABLE cliente      RENAME COLUMN barberia_id TO negocio_id;
ALTER TABLE atencion     RENAME COLUMN barberia_id TO negocio_id;
ALTER TABLE notificacion RENAME COLUMN barberia_id TO negocio_id;

-- ---------- Columnas barbero_id -> profesional_id ----------
-- (la restricción de exclusión excl_atencion_superpuesta se actualiza sola)
ALTER TABLE profesional_sucursal RENAME COLUMN barbero_id TO profesional_id;
ALTER TABLE profesional_servicio RENAME COLUMN barbero_id TO profesional_id;
ALTER TABLE horario_laboral      RENAME COLUMN barbero_id TO profesional_id;
ALTER TABLE bloqueo              RENAME COLUMN barbero_id TO profesional_id;
ALTER TABLE atencion             RENAME COLUMN barbero_id TO profesional_id;

-- ---------- Rol BARBERO -> PROFESIONAL ----------
ALTER TABLE usuario DROP CONSTRAINT chk_usuario_rol;
UPDATE usuario SET rol = 'PROFESIONAL' WHERE rol = 'BARBERO';
ALTER TABLE usuario ADD CONSTRAINT chk_usuario_rol CHECK (rol IN ('ADMIN', 'PROFESIONAL'));

-- ---------- Restricciones e índices ----------
-- Muchos los nombró Postgres solo (barbero_pkey, usuario_barberia_id_fkey, ...), así que se
-- renombran todos los que tengan "barberia" o "barbero" en el nombre, sin depender de cada uno.
DO $$
DECLARE
    r RECORD;
BEGIN
    -- Restricciones (renombrar una PK/UNIQUE/EXCLUDE renombra también su índice)
    FOR r IN
        SELECT c.conname, t.relname AS tabla
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = current_schema() AND c.conname LIKE '%barber%'
    LOOP
        EXECUTE format('ALTER TABLE %I RENAME CONSTRAINT %I TO %I', r.tabla, r.conname,
                       replace(replace(r.conname, 'barberia', 'negocio'), 'barbero', 'profesional'));
    END LOOP;

    -- Índices sueltos (los que no son de una restricción)
    FOR r IN
        SELECT i.relname AS indice
        FROM pg_class i
        JOIN pg_namespace n ON n.oid = i.relnamespace
        WHERE n.nspname = current_schema() AND i.relkind = 'i' AND i.relname LIKE '%barber%'
    LOOP
        EXECUTE format('ALTER INDEX %I RENAME TO %I', r.indice,
                       replace(replace(r.indice, 'barberia', 'negocio'), 'barbero', 'profesional'));
    END LOOP;
END $$;

-- ---------- Verificación ----------
-- Si quedó algún nombre viejo (por ejemplo una columna agregada en otra rama), la migración
-- falla acá en vez de romper la aplicación en runtime.
DO $$
DECLARE
    pendientes TEXT;
BEGIN
    SELECT string_agg(nombre, ', ') INTO pendientes FROM (
        SELECT table_name AS nombre
        FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name LIKE '%barber%'
        UNION ALL
        SELECT table_name || '.' || column_name
        FROM information_schema.columns
        WHERE table_schema = current_schema() AND column_name LIKE '%barber%'
        UNION ALL
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_namespace n ON n.oid = c.connamespace
        WHERE n.nspname = current_schema() AND c.conname LIKE '%barber%'
        UNION ALL
        SELECT i.relname
        FROM pg_class i
        JOIN pg_namespace n ON n.oid = i.relnamespace
        WHERE n.nspname = current_schema() AND i.relkind = 'i' AND i.relname LIKE '%barber%'
    ) x;

    IF pendientes IS NOT NULL THEN
        RAISE EXCEPTION 'V7: quedaron nombres sin renombrar: %', pendientes;
    END IF;
END $$;

-- =====================================================================
-- Datos de prueba SOLO para desarrollo (se carga con el perfil dev).
-- Es "repeatable": Flyway lo vuelve a correr cada vez que cambia este
-- archivo, por eso todo es idempotente (ON CONFLICT / DELETE previo).
-- =====================================================================

-- Barbería
INSERT INTO barberia (id, nombre, slug, reservas_online)
VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 'Jade', 'jade', TRUE)
ON CONFLICT (id) DO NOTHING;

-- Sucursal
INSERT INTO sucursal (id, barberia_id, nombre, direccion, ciudad, telefono)
VALUES ('bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001',
        'Jade Centro', 'Sobremonte 240', 'Río Cuarto', '+5493584000000')
ON CONFLICT (id) DO NOTHING;

-- Barberos
INSERT INTO barbero (id, barberia_id, nombre, apellido) VALUES
    ('cccccccc-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Lucas',  'Sanchez'),
    ('cccccccc-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 'Andres', 'Biassi'),
    ('cccccccc-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000001', 'Chapa',  'Perez')
ON CONFLICT (id) DO NOTHING;

-- Servicios (precios en centavos)
INSERT INTO servicio (id, barberia_id, nombre, duracion_min, precio, orden) VALUES
    ('dddddddd-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Corte',         30, 1500000, 1),
    ('dddddddd-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 'Barba',         15,  500000, 2),
    ('dddddddd-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000001', 'Corte y Barba', 45, 1700000, 3)
ON CONFLICT (id) DO NOTHING;

-- Todos los barberos trabajan en la sucursal y hacen todos los servicios
INSERT INTO barbero_sucursal (barbero_id, sucursal_id)
SELECT b.id, 'bbbbbbbb-0000-0000-0000-000000000001'
FROM barbero b
WHERE b.barberia_id = 'aaaaaaaa-0000-0000-0000-000000000001'
ON CONFLICT DO NOTHING;

INSERT INTO barbero_servicio (barbero_id, servicio_id)
SELECT b.id, s.id
FROM barbero b
JOIN servicio s ON s.barberia_id = b.barberia_id
WHERE b.barberia_id = 'aaaaaaaa-0000-0000-0000-000000000001'
ON CONFLICT DO NOTHING;

-- Horarios: lunes a sábado, 10 a 13 y 16 a 21 (se regeneran completos)
DELETE FROM horario_laboral
WHERE barbero_id IN (SELECT id FROM barbero WHERE barberia_id = 'aaaaaaaa-0000-0000-0000-000000000001');

INSERT INTO horario_laboral (barbero_id, sucursal_id, dia_semana, hora_inicio, hora_fin)
SELECT b.id, 'bbbbbbbb-0000-0000-0000-000000000001', d.dia, t.inicio, t.fin
FROM barbero b
CROSS JOIN generate_series(1, 6) AS d(dia)
CROSS JOIN (VALUES (TIME '10:00', TIME '13:00'),
                   (TIME '16:00', TIME '21:00')) AS t(inicio, fin)
WHERE b.barberia_id = 'aaaaaaaa-0000-0000-0000-000000000001';

-- Clientes de ejemplo
INSERT INTO cliente (id, barberia_id, nombre, telefono, notas) VALUES
    ('eeeeeeee-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001',
     'Juan Gomez', '+5493584111111', 'Degradé en 2, arriba tijera'),
    ('eeeeeeee-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001',
     'Martin Lopez', '+5493584222222', NULL)
ON CONFLICT (id) DO NOTHING;

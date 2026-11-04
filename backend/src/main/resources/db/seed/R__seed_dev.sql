-- =====================================================================
-- Datos de prueba SOLO para desarrollo (se carga con el perfil dev).
-- Es "repeatable": Flyway lo vuelve a correr cada vez que cambia este
-- archivo, por eso todo es idempotente (ON CONFLICT / DELETE previo).
-- =====================================================================

-- Barbería
INSERT INTO negocio (id, nombre, slug, reservas_online)
VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 'Jade', 'jade', TRUE)
ON CONFLICT (id) DO NOTHING;

-- Sucursal
INSERT INTO sucursal (id, negocio_id, nombre, direccion, ciudad, telefono)
VALUES ('bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001',
        'Jade Centro', 'Sobremonte 240', 'Río Cuarto', '+5493584000000')
ON CONFLICT (id) DO NOTHING;

-- Barberos
INSERT INTO profesional (id, negocio_id, nombre, apellido) VALUES
    ('cccccccc-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Lucas',  'Sanchez'),
    ('cccccccc-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 'Andres', 'Biassi'),
    ('cccccccc-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000001', 'Chapa',  'Perez')
ON CONFLICT (id) DO NOTHING;

-- Servicios (precios en centavos)
INSERT INTO servicio (id, negocio_id, nombre, duracion_min, precio, orden) VALUES
    ('dddddddd-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Corte',         30, 1500000, 1),
    ('dddddddd-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 'Barba',         15,  500000, 2),
    ('dddddddd-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000001', 'Corte y Barba', 45, 1700000, 3)
ON CONFLICT (id) DO NOTHING;

-- Todos los barberos trabajan en la sucursal y hacen todos los servicios
INSERT INTO profesional_sucursal (profesional_id, sucursal_id)
SELECT b.id, 'bbbbbbbb-0000-0000-0000-000000000001'
FROM profesional b
WHERE b.negocio_id = 'aaaaaaaa-0000-0000-0000-000000000001'
ON CONFLICT DO NOTHING;

INSERT INTO profesional_servicio (profesional_id, servicio_id)
SELECT b.id, s.id
FROM profesional b
JOIN servicio s ON s.negocio_id = b.negocio_id
WHERE b.negocio_id = 'aaaaaaaa-0000-0000-0000-000000000001'
ON CONFLICT DO NOTHING;

-- Horarios: lunes a sábado, 10 a 13 y 16 a 21 (se regeneran completos)
DELETE FROM horario_laboral
WHERE profesional_id IN (SELECT id FROM profesional WHERE negocio_id = 'aaaaaaaa-0000-0000-0000-000000000001');

INSERT INTO horario_laboral (profesional_id, sucursal_id, dia_semana, hora_inicio, hora_fin)
SELECT b.id, 'bbbbbbbb-0000-0000-0000-000000000001', d.dia, t.inicio, t.fin
FROM profesional b
CROSS JOIN generate_series(1, 6) AS d(dia)
CROSS JOIN (VALUES (TIME '10:00', TIME '13:00'),
                   (TIME '16:00', TIME '21:00')) AS t(inicio, fin)
WHERE b.negocio_id = 'aaaaaaaa-0000-0000-0000-000000000001';

-- Clientes de ejemplo
INSERT INTO cliente (id, negocio_id, nombre, telefono, notas) VALUES
    ('eeeeeeee-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001',
     'Juan Gomez', '+5493584111111', 'Degradé en 2, arriba tijera'),
    ('eeeeeeee-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001',
     'Martin Lopez', '+5493584222222', NULL)
ON CONFLICT (id) DO NOTHING;

-- Usuarios de prueba (contraseñas en texto plano SOLO para dev):
--   admin@jade.com  / admin123    -> ADMIN
--   lucas@jade.com  / barbero123  -> PROFESIONAL, vinculado a Lucas Sanchez
INSERT INTO usuario (id, negocio_id, email, password_hash, rol) VALUES
    ('ffffffff-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001',
     'admin@jade.com', '$2a$10$N1CuK5AI91KPYwxruZLIBecKcS/txrG3UUxP.1e57qOf7kep0NYmC', 'ADMIN'),
    ('ffffffff-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001',
     'lucas@jade.com', '$2a$10$o4mJUbVgyMIGlMgaG/QYF.b8K5X5DDZkA4tXY1POsfWIcuBtSSe6K', 'PROFESIONAL')
ON CONFLICT (id) DO NOTHING;

UPDATE profesional SET usuario_id = 'ffffffff-0000-0000-0000-000000000002'
WHERE id = 'cccccccc-0000-0000-0000-000000000001';

-- =====================================================================
-- V1: modelo de datos inicial
-- Convenciones:
--   * IDs UUID generados por la base (gen_random_uuid)
--   * Fechas y horas puntuales en TIMESTAMPTZ (se guardan en UTC)
--   * Montos en BIGINT, en centavos ($15.000,00 = 1500000)
--   * Enums como VARCHAR con CHECK (se mapean con @Enumerated(STRING))
--   * dia_semana: 1 = lunes ... 7 = domingo (igual que java.time.DayOfWeek)
-- =====================================================================

-- Necesaria para la restricción que impide turnos superpuestos
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------------
-- Barbería (cada cliente del sistema)
-- ---------------------------------------------------------------------
CREATE TABLE barberia (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre                   VARCHAR(120) NOT NULL,
    slug                     VARCHAR(80)  NOT NULL UNIQUE,
    logo_url                 VARCHAR(500),
    reservas_online          BOOLEAN      NOT NULL DEFAULT FALSE,
    zona_horaria             VARCHAR(60)  NOT NULL DEFAULT 'America/Argentina/Cordoba',
    anticipacion_minima_min  INTEGER      NOT NULL DEFAULT 60,
    anticipacion_maxima_dias INTEGER      NOT NULL DEFAULT 30,
    intervalo_slots_min      INTEGER      NOT NULL DEFAULT 30,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_barberia_slug       CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT chk_barberia_intervalo  CHECK (intervalo_slots_min IN (15, 30, 60)),
    CONSTRAINT chk_barberia_anticipo   CHECK (anticipacion_minima_min >= 0 AND anticipacion_maxima_dias > 0)
);

-- ---------------------------------------------------------------------
-- Usuario (login del personal; los clientes que reservan no tienen cuenta)
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    barberia_id   UUID         NOT NULL REFERENCES barberia (id),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol           VARCHAR(20)  NOT NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_usuario_rol CHECK (rol IN ('ADMIN', 'BARBERO'))
);
CREATE INDEX idx_usuario_barberia ON usuario (barberia_id);

-- ---------------------------------------------------------------------
-- Sucursal
-- ---------------------------------------------------------------------
CREATE TABLE sucursal (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    barberia_id UUID         NOT NULL REFERENCES barberia (id),
    nombre      VARCHAR(120) NOT NULL,
    direccion   VARCHAR(255) NOT NULL,
    ciudad      VARCHAR(120) NOT NULL,
    telefono    VARCHAR(20),
    activa      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_sucursal_barberia ON sucursal (barberia_id);

-- ---------------------------------------------------------------------
-- Barbero
-- ---------------------------------------------------------------------
CREATE TABLE barbero (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    barberia_id UUID         NOT NULL REFERENCES barberia (id),
    usuario_id  UUID         UNIQUE REFERENCES usuario (id),
    nombre      VARCHAR(80)  NOT NULL,
    apellido    VARCHAR(80)  NOT NULL,
    foto_url    VARCHAR(500),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_barbero_barberia ON barbero (barberia_id);

-- ---------------------------------------------------------------------
-- Servicio
-- ---------------------------------------------------------------------
CREATE TABLE servicio (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    barberia_id  UUID         NOT NULL REFERENCES barberia (id),
    nombre       VARCHAR(120) NOT NULL,
    duracion_min INTEGER      NOT NULL,
    precio       BIGINT       NOT NULL,
    activo       BOOLEAN      NOT NULL DEFAULT TRUE,
    orden        INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_servicio_duracion CHECK (duracion_min > 0),
    CONSTRAINT chk_servicio_precio   CHECK (precio >= 0)
);
CREATE INDEX idx_servicio_barberia ON servicio (barberia_id);

-- ---------------------------------------------------------------------
-- Relaciones N:M
-- ---------------------------------------------------------------------
CREATE TABLE barbero_sucursal (
    barbero_id  UUID NOT NULL REFERENCES barbero (id)  ON DELETE CASCADE,
    sucursal_id UUID NOT NULL REFERENCES sucursal (id) ON DELETE CASCADE,
    PRIMARY KEY (barbero_id, sucursal_id)
);

CREATE TABLE barbero_servicio (
    barbero_id  UUID NOT NULL REFERENCES barbero (id)  ON DELETE CASCADE,
    servicio_id UUID NOT NULL REFERENCES servicio (id) ON DELETE CASCADE,
    PRIMARY KEY (barbero_id, servicio_id)
);

-- ---------------------------------------------------------------------
-- Horario laboral semanal (hora local de la barbería)
-- ---------------------------------------------------------------------
CREATE TABLE horario_laboral (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    barbero_id  UUID    NOT NULL REFERENCES barbero (id)  ON DELETE CASCADE,
    sucursal_id UUID    NOT NULL REFERENCES sucursal (id) ON DELETE CASCADE,
    dia_semana  INTEGER NOT NULL,
    hora_inicio TIME    NOT NULL,
    hora_fin    TIME    NOT NULL,

    CONSTRAINT chk_horario_dia   CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT chk_horario_rango CHECK (hora_fin > hora_inicio)
);
CREATE INDEX idx_horario_barbero_dia ON horario_laboral (barbero_id, dia_semana);

-- ---------------------------------------------------------------------
-- Bloqueo (almuerzo, franco, vacaciones)
-- ---------------------------------------------------------------------
CREATE TABLE bloqueo (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    barbero_id UUID         NOT NULL REFERENCES barbero (id) ON DELETE CASCADE,
    inicio     TIMESTAMPTZ  NOT NULL,
    fin        TIMESTAMPTZ  NOT NULL,
    motivo     VARCHAR(200),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_bloqueo_rango CHECK (fin > inicio)
);
CREATE INDEX idx_bloqueo_barbero_inicio ON bloqueo (barbero_id, inicio);

-- ---------------------------------------------------------------------
-- Cliente
-- ---------------------------------------------------------------------
CREATE TABLE cliente (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    barberia_id UUID          NOT NULL REFERENCES barberia (id),
    nombre      VARCHAR(120)  NOT NULL,
    telefono    VARCHAR(20)   NOT NULL,
    notas       VARCHAR(2000),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_cliente_telefono  UNIQUE (barberia_id, telefono),
    CONSTRAINT chk_cliente_telefono CHECK (telefono ~ '^\+[1-9][0-9]{7,14}$')  -- formato E.164
);
CREATE INDEX idx_cliente_barberia ON cliente (barberia_id);

-- ---------------------------------------------------------------------
-- Atención: un turno reservado o un corte registrado a mano
-- ---------------------------------------------------------------------
CREATE TABLE atencion (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    barberia_id   UUID          NOT NULL REFERENCES barberia (id),
    sucursal_id   UUID          NOT NULL REFERENCES sucursal (id),
    barbero_id    UUID          NOT NULL REFERENCES barbero (id),
    cliente_id    UUID          REFERENCES cliente (id),          -- NULL = corte anónimo
    servicio_id   UUID          NOT NULL REFERENCES servicio (id),
    inicio        TIMESTAMPTZ   NOT NULL,
    fin           TIMESTAMPTZ   NOT NULL,
    estado        VARCHAR(20)   NOT NULL,
    origen        VARCHAR(20)   NOT NULL,
    precio_final  BIGINT        NOT NULL,
    metodo_pago   VARCHAR(20),                                     -- se completa al cobrar
    token_gestion UUID          NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    notas         VARCHAR(2000),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT chk_atencion_rango  CHECK (fin > inicio),
    CONSTRAINT chk_atencion_precio CHECK (precio_final >= 0),
    CONSTRAINT chk_atencion_estado CHECK (estado IN ('RESERVADA', 'COMPLETADA', 'CANCELADA', 'AUSENTE')),
    CONSTRAINT chk_atencion_origen CHECK (origen IN ('ONLINE', 'MANUAL')),
    CONSTRAINT chk_atencion_pago   CHECK (metodo_pago IS NULL
                                          OR metodo_pago IN ('EFECTIVO', 'TRANSFERENCIA', 'MERCADOPAGO', 'TARJETA')),

    -- Un barbero no puede tener dos atenciones activas superpuestas.
    -- Las canceladas y ausentes no cuentan, así liberan el horario.
    CONSTRAINT excl_atencion_superpuesta EXCLUDE USING gist (
        barbero_id WITH =,
        tstzrange(inicio, fin) WITH &&
    ) WHERE (estado IN ('RESERVADA', 'COMPLETADA'))
);
CREATE INDEX idx_atencion_barbero_inicio  ON atencion (barbero_id, inicio);
CREATE INDEX idx_atencion_barberia_inicio ON atencion (barberia_id, inicio);
CREATE INDEX idx_atencion_cliente         ON atencion (cliente_id);

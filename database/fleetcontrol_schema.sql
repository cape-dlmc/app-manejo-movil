-- =====================================================================
-- FleetControl - Script de creación de la base de datos
-- Motor: PostgreSQL 14 o superior
-- Basado en la sección 10 del microproyecto empresarial UX
-- =====================================================================

-- Opcional: crear la base de datos (ejecutar como superusuario)
-- CREATE DATABASE fleetcontrol ENCODING 'UTF8';
-- \c fleetcontrol

BEGIN;

-- ---------------------------------------------------------------------
-- 10.1 Usuarios: cuentas de acceso
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id_usuario      BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(100)  NOT NULL,
    correo          VARCHAR(150)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255)  NOT NULL,            -- nunca texto plano (RNF-03)
    rol             VARCHAR(20)   NOT NULL
                    CHECK (rol IN ('ADMIN', 'CONDUCTOR')),
    estado          BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- 10.2 Conductores: datos específicos de cada conductor
-- Relación 1 a 1 con usuarios: una persona, una sola cuenta
-- ---------------------------------------------------------------------
CREATE TABLE conductores (
    id_conductor    BIGSERIAL PRIMARY KEY,
    id_usuario      BIGINT        NOT NULL UNIQUE
                    REFERENCES usuarios (id_usuario),
    telefono        VARCHAR(30),
    identificacion  VARCHAR(30)   NOT NULL UNIQUE,
    estado          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVO'
                    CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

-- ---------------------------------------------------------------------
-- 10.3 Vehículos
-- ---------------------------------------------------------------------
CREATE TABLE vehiculos (
    id_vehiculo     BIGSERIAL PRIMARY KEY,
    placa           VARCHAR(10)   NOT NULL UNIQUE,
    marca           VARCHAR(50)   NOT NULL,
    modelo          VARCHAR(50)   NOT NULL,
    anio            SMALLINT      NOT NULL
                    CHECK (anio BETWEEN 1990 AND 2100),
    estado          VARCHAR(25)   NOT NULL DEFAULT 'DISPONIBLE'
                    CHECK (estado IN ('DISPONIBLE', 'EN_SERVICIO',
                                      'MANTENIMIENTO', 'INACTIVO')),
    observaciones   TEXT
);

-- ---------------------------------------------------------------------
-- 10.4 Viajes
-- ---------------------------------------------------------------------
CREATE TABLE viajes (
    id_viaje            BIGSERIAL PRIMARY KEY,
    codigo              VARCHAR(30)   NOT NULL UNIQUE,
    id_vehiculo         BIGINT        NOT NULL
                        REFERENCES vehiculos (id_vehiculo),
    id_conductor        BIGINT        NOT NULL
                        REFERENCES conductores (id_conductor),
    tipo_servicio       VARCHAR(30)   NOT NULL
                        CHECK (tipo_servicio IN ('INTERMUNICIPAL', 'PUERTA_A_PUERTA')),
    origen              VARCHAR(150)  NOT NULL,
    destino             VARCHAR(150)  NOT NULL,
    fecha_salida        TIMESTAMP     NOT NULL,
    fecha_finalizacion  TIMESTAMP,
    valor_cobrado       NUMERIC(12,2) NOT NULL CHECK (valor_cobrado > 0),
    estado              VARCHAR(25)   NOT NULL DEFAULT 'PROGRAMADO'
                        CHECK (estado IN ('PROGRAMADO', 'EN_CURSO',
                                          'COMPLETADO', 'CANCELADO')),
    observaciones       TEXT,
    CONSTRAINT chk_fechas_viaje
        CHECK (fecha_finalizacion IS NULL OR fecha_finalizacion >= fecha_salida)
);

-- ---------------------------------------------------------------------
-- 10.5 Catálogo de categorías
-- ---------------------------------------------------------------------
CREATE TABLE categorias_movimiento (
    id_categoria    BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(60)   NOT NULL UNIQUE,
    tipo            VARCHAR(15)   NOT NULL
                    CHECK (tipo IN ('INGRESO', 'EGRESO')),
    descripcion     TEXT,
    activa          BOOLEAN       NOT NULL DEFAULT TRUE
);

-- ---------------------------------------------------------------------
-- 10.6 Movimientos financieros (egresos y otros movimientos)
-- Los ingresos ordinarios de un viaje salen de viajes.valor_cobrado,
-- para evitar duplicados (RN-14).
-- ---------------------------------------------------------------------
CREATE TABLE movimientos_financieros (
    id_movimiento         BIGSERIAL PRIMARY KEY,
    id_categoria          BIGINT        NOT NULL
                          REFERENCES categorias_movimiento (id_categoria),
    id_vehiculo           BIGINT        NOT NULL
                          REFERENCES vehiculos (id_vehiculo),        -- RN-02
    id_viaje              BIGINT
                          REFERENCES viajes (id_viaje),              -- opcional
    id_usuario_registra   BIGINT        NOT NULL
                          REFERENCES usuarios (id_usuario),
    tipo                  VARCHAR(15)   NOT NULL
                          CHECK (tipo IN ('INGRESO', 'EGRESO')),
    fecha_movimiento      TIMESTAMP     NOT NULL,
    valor                 NUMERIC(12,2) NOT NULL CHECK (valor > 0),  -- RN-01
    -- TRUE: se pagó con dinero recaudado en el viaje (se descuenta de la entrega).
    -- FALSE: lo pagó la empresa (no se descuenta, sección 12.2).
    pagado_con_recaudo    BOOLEAN       NOT NULL DEFAULT TRUE,
    descripcion           TEXT          NOT NULL,
    estado_revision       VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE'
                          CHECK (estado_revision IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')),
    id_usuario_revisa     BIGINT
                          REFERENCES usuarios (id_usuario),
    fecha_revision        TIMESTAMP,
    observacion_revision  TEXT,

    -- RN-04: nadie aprueba o rechaza sus propios gastos
    CONSTRAINT chk_no_autoaprobacion
        CHECK (id_usuario_revisa IS NULL
               OR id_usuario_revisa <> id_usuario_registra),

    -- Un movimiento revisado debe tener revisor y fecha de revisión
    CONSTRAINT chk_revision_completa
        CHECK (estado_revision = 'PENDIENTE'
               OR (id_usuario_revisa IS NOT NULL AND fecha_revision IS NOT NULL)),

    -- Un rechazo exige observación (módulo 7)
    CONSTRAINT chk_rechazo_con_motivo
        CHECK (estado_revision <> 'RECHAZADO'
               OR (observacion_revision IS NOT NULL
                   AND length(trim(observacion_revision)) > 0))
);

-- ---------------------------------------------------------------------
-- 10.7 Comprobantes: soportes de los movimientos
-- ---------------------------------------------------------------------
CREATE TABLE comprobantes (
    id_comprobante      BIGSERIAL PRIMARY KEY,
    id_movimiento       BIGINT        NOT NULL
                        REFERENCES movimientos_financieros (id_movimiento)
                        ON DELETE RESTRICT,
    nombre_archivo      VARCHAR(255)  NOT NULL,
    ruta_archivo        VARCHAR(500)  NOT NULL,   -- almacenamiento privado
    tipo_archivo        VARCHAR(50)   NOT NULL,
    fecha_carga         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario_carga    BIGINT        NOT NULL
                        REFERENCES usuarios (id_usuario)
);

-- ---------------------------------------------------------------------
-- 10.8 Entregas de dinero a la empresa
-- ---------------------------------------------------------------------
CREATE TABLE entregas_dinero (
    id_entrega          BIGSERIAL PRIMARY KEY,
    id_conductor        BIGINT        NOT NULL
                        REFERENCES conductores (id_conductor),  -- quien entrega
    id_viaje            BIGINT
                        REFERENCES viajes (id_viaje),           -- opcional
    fecha_entrega       TIMESTAMP     NOT NULL,
    valor_entregado     NUMERIC(12,2) NOT NULL CHECK (valor_entregado > 0),  -- RN-01
    id_usuario_recibe   BIGINT
                        REFERENCES usuarios (id_usuario),       -- quien recibe (RN-06)
    referencia          VARCHAR(100),
    observaciones       TEXT,
    estado              VARCHAR(20)   NOT NULL DEFAULT 'REGISTRADA'
                        CHECK (estado IN ('REGISTRADA', 'VERIFICADA'))
);

-- ---------------------------------------------------------------------
-- 10.9 Historial de auditoría (no guarda contraseñas ni datos secretos)
-- ---------------------------------------------------------------------
CREATE TABLE historial_auditoria (
    id_auditoria    BIGSERIAL PRIMARY KEY,
    id_usuario      BIGINT        NOT NULL
                    REFERENCES usuarios (id_usuario),
    entidad         VARCHAR(60)   NOT NULL,
    id_registro     BIGINT        NOT NULL,
    accion          VARCHAR(30)   NOT NULL,
    fecha_accion    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detalle         TEXT
);

-- ---------------------------------------------------------------------
-- Índices para consultas y filtros (RF-12)
-- ---------------------------------------------------------------------
CREATE INDEX idx_viajes_vehiculo       ON viajes (id_vehiculo);
CREATE INDEX idx_viajes_conductor_fecha ON viajes (id_conductor, fecha_salida);
CREATE INDEX idx_viajes_estado         ON viajes (estado);
CREATE INDEX idx_mov_vehiculo          ON movimientos_financieros (id_vehiculo);
CREATE INDEX idx_mov_viaje             ON movimientos_financieros (id_viaje);
CREATE INDEX idx_mov_revision          ON movimientos_financieros (estado_revision);
CREATE INDEX idx_mov_fecha             ON movimientos_financieros (fecha_movimiento);
CREATE INDEX idx_comprobantes_mov      ON comprobantes (id_movimiento);
CREATE INDEX idx_entregas_conductor    ON entregas_dinero (id_conductor);
CREATE INDEX idx_entregas_viaje        ON entregas_dinero (id_viaje);
CREATE INDEX idx_auditoria_entidad     ON historial_auditoria (entidad, id_registro);

-- ---------------------------------------------------------------------
-- Datos iniciales: categorías de movimiento (sección 10.5)
-- ---------------------------------------------------------------------
INSERT INTO categorias_movimiento (nombre, tipo, descripcion, activa) VALUES
    ('Combustible',              'EGRESO', 'Gasolina o diésel del vehículo', TRUE),
    ('Peajes',                   'EGRESO', 'Pagos de peajes en la ruta',     TRUE),
    ('Parqueadero',              'EGRESO', 'Parqueo del vehículo',           TRUE),
    ('Lavado',                   'EGRESO', 'Lavado del vehículo',            TRUE),
    ('Mantenimiento',            'EGRESO', 'Mantenimiento preventivo',       TRUE),
    ('Reparaciones',             'EGRESO', 'Reparaciones del vehículo',      TRUE),
    ('Otros gastos autorizados', 'EGRESO', 'Otros gastos aprobados por la empresa', TRUE);

COMMIT;

-- =====================================================================
-- Verificación rápida: debe listar las 9 tablas del modelo
-- =====================================================================
-- \dt

CREATE TABLE IF NOT EXISTS usuario (
  id_usuario        BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nombre_usuario    VARCHAR(100) NOT NULL,
  nombre            VARCHAR(100) NOT NULL,
  email             VARCHAR(100) NOT NULL UNIQUE,
  password          VARCHAR(255) NOT NULL,
  is_verified       BOOLEAN NOT NULL DEFAULT FALSE,
  fecha_creacion    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  confirmation_token VARCHAR(255),
  stripe_customer_id VARCHAR(255) NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- ESPACIOS DE TRABAJO
-- =========================
CREATE TABLE IF NOT EXISTS espacio_trabajo (
  id_espacio       BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nombre           VARCHAR(100) NOT NULL,
  descripcion      TEXT,
  fecha_creacion   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_usuario_duenio BIGINT NOT NULL,
  CONSTRAINT fk_espacio_usuario_duenio
    FOREIGN KEY (id_usuario_duenio) REFERENCES usuario(id_usuario)
      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- TABLEROS
-- =========================
CREATE TABLE IF NOT EXISTS tablero (
  id_tablero         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nombre             VARCHAR(100) NOT NULL,
  descripcion        TEXT,
  background         VARCHAR(255),
  fecha_creacion     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_usuario_creador BIGINT NOT NULL,
  id_espacio         BIGINT NULL,
  CONSTRAINT fk_tablero_usuario_creador
    FOREIGN KEY (id_usuario_creador) REFERENCES usuario(id_usuario)
      ON DELETE CASCADE,
  CONSTRAINT fk_tablero_espacio
    FOREIGN KEY (id_espacio) REFERENCES espacio_trabajo(id_espacio)
      ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- MIEMBROS DE TABLEROS
-- =========================
CREATE TABLE IF NOT EXISTS miembro_tablero (
  id_usuario BIGINT NOT NULL,
  id_tablero BIGINT NOT NULL,
  rol        VARCHAR(20) NOT NULL DEFAULT 'lector',
  PRIMARY KEY (id_usuario, id_tablero),
  CONSTRAINT fk_miembro_usuario
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
      ON DELETE CASCADE,
  CONSTRAINT fk_miembro_tablero
    FOREIGN KEY (id_tablero) REFERENCES tablero(id_tablero)
      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- RELACIÓN ESPACIOS <-> TABLEROS (links adicionales)
-- =========================
CREATE TABLE IF NOT EXISTS workspace_board_link (
  id               BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  workspace_id     BIGINT NOT NULL,
  board_id         BIGINT NOT NULL,
  UNIQUE KEY uq_workspace_board (workspace_id, board_id),
  CONSTRAINT fk_wb_workspace
    FOREIGN KEY (workspace_id) REFERENCES espacio_trabajo(id_espacio)
      ON DELETE CASCADE,
  CONSTRAINT fk_wb_board
    FOREIGN KEY (board_id) REFERENCES tablero(id_tablero)
      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- LISTAS
-- =========================
CREATE TABLE IF NOT EXISTS lista (
  id_lista   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nombre     VARCHAR(100) NOT NULL,
  orden      INT NOT NULL,
  id_tablero BIGINT NOT NULL,
  CONSTRAINT fk_lista_tablero
    FOREIGN KEY (id_tablero) REFERENCES tablero(id_tablero)
      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- TARJETAS
-- =========================
CREATE TABLE IF NOT EXISTS tarjeta (
  id_tarjeta   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  titulo       VARCHAR(100) NOT NULL,
  descripcion  TEXT,
  creada_en    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  comienza_en  DATETIME NULL,
  expira_en    DATETIME NULL,
  card_order   INT NOT NULL,
  id_lista     BIGINT NOT NULL,
  CONSTRAINT fk_tarjeta_lista
    FOREIGN KEY (id_lista) REFERENCES lista(id_lista)
      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- COMENTARIOS
-- =========================
CREATE TABLE IF NOT EXISTS comentario (
  id_comentario BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  contenido     TEXT NOT NULL,
  fecha         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  id_usuario    BIGINT NOT NULL,
  id_tarjeta    BIGINT NOT NULL,
  CONSTRAINT fk_comentario_usuario
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
      ON DELETE CASCADE,
  CONSTRAINT fk_comentario_tarjeta
    FOREIGN KEY (id_tarjeta) REFERENCES tarjeta(id_tarjeta)
      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- ETIQUETAS
-- =========================
CREATE TABLE IF NOT EXISTS etiqueta (
  id_etiqueta BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nombre      VARCHAR(50) NOT NULL,
  color       VARCHAR(20),
  id_tablero  BIGINT NOT NULL,
  CONSTRAINT fk_etiqueta_tablero
    FOREIGN KEY (id_tablero) REFERENCES tablero(id_tablero)
      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- TARJETA_ETIQUETA (N..N)
-- =========================
CREATE TABLE IF NOT EXISTS tarjeta_etiqueta (
  id_tarjeta  BIGINT NOT NULL,
  id_etiqueta BIGINT NOT NULL,
  PRIMARY KEY (id_tarjeta, id_etiqueta),
  CONSTRAINT fk_te_tarjeta
    FOREIGN KEY (id_tarjeta) REFERENCES tarjeta(id_tarjeta)
      ON DELETE CASCADE,
  CONSTRAINT fk_te_etiqueta
    FOREIGN KEY (id_etiqueta) REFERENCES etiqueta(id_etiqueta)
      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- ARCHIVOS (adjuntos)
-- =========================
CREATE TABLE IF NOT EXISTS archivo (
  id_archivo BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nombre     VARCHAR(100) NOT NULL,
  url        TEXT NOT NULL,
  id_tarjeta BIGINT NOT NULL,
  CONSTRAINT fk_archivo_tarjeta
    FOREIGN KEY (id_tarjeta) REFERENCES tarjeta(id_tarjeta)
      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- HISTORIAL DE MOVIMIENTOS
-- =========================
CREATE TABLE IF NOT EXISTS historial_movimiento (
  id                BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  id_tarjeta        BIGINT NOT NULL,
  id_lista_origen   BIGINT NULL,
  id_lista_destino  BIGINT NOT NULL,
  fecha_movimiento  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_historial_tarjeta
    FOREIGN KEY (id_tarjeta) REFERENCES tarjeta(id_tarjeta)
      ON DELETE CASCADE,
  CONSTRAINT fk_historial_lista_origen
    FOREIGN KEY (id_lista_origen) REFERENCES lista(id_lista)
      ON DELETE SET NULL,
  CONSTRAINT fk_historial_lista_destino
    FOREIGN KEY (id_lista_destino) REFERENCES lista(id_lista)
      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- TOKENS DE REFRESCO
-- =========================
CREATE TABLE IF NOT EXISTS tokens_refresco (
  id              BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  token           VARCHAR(100) NOT NULL UNIQUE,
  fecha_expiracion TIMESTAMP NOT NULL,
  id_duenio       BIGINT NOT NULL,
  CONSTRAINT fk_tokens_usuario
    FOREIGN KEY (id_duenio) REFERENCES usuario(id_usuario)
      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- INVITACIONES
-- =========================
CREATE TABLE IF NOT EXISTS invitacion (
  id                   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  token                VARCHAR(255) NOT NULL UNIQUE,
  invitee_email        VARCHAR(255) NOT NULL,
  id_tablero           BIGINT NOT NULL,
  id_usuario_invitador BIGINT NOT NULL,
  expires_at           DATETIME NULL,
  fecha_creacion       DATETIME NOT NULL,
  estado               ENUM('PENDIENTE','ACEPTADA','RECHAZADA') NOT NULL DEFAULT 'PENDIENTE',
  rol                  VARCHAR(20) NOT NULL DEFAULT 'lector',
  CONSTRAINT fk_invitacion_tablero
    FOREIGN KEY (id_tablero) REFERENCES tablero(id_tablero)
      ON DELETE CASCADE,
  CONSTRAINT fk_invitacion_invitador
    FOREIGN KEY (id_usuario_invitador) REFERENCES usuario(id_usuario)
      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- NOTIFICACIONES
-- =========================
CREATE TABLE IF NOT EXISTS notificaciones (
  id                BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  descripcion       VARCHAR(255) NOT NULL,
  id_usuario_origen BIGINT NOT NULL,
  id_usuario_destino BIGINT NOT NULL,
  fecha_creacion    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notif_origen
    FOREIGN KEY (id_usuario_origen) REFERENCES usuario(id_usuario)
      ON DELETE CASCADE,
  CONSTRAINT fk_notif_destino
    FOREIGN KEY (id_usuario_destino) REFERENCES usuario(id_usuario)
      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- SUSCRIPCIONES
-- =========================
CREATE TABLE IF NOT EXISTS suscripcion (
  id_suscripcion  BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  id_usuario      BIGINT NOT NULL,
  plan_nombre     VARCHAR(50) NOT NULL,               -- 'free', 'pro', etc.
  is_active       BOOLEAN NOT NULL DEFAULT FALSE,
  fecha_inicio    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_expiracion TIMESTAMP NULL,
  last_updated    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_suscripcion_usuario
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

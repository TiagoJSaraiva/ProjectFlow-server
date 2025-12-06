SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS usuarios (
    id CHAR(36) NOT NULL PRIMARY KEY DEFAULT (UUID()),
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    metadados JSON NULL,
    data_criado TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    email_verificado TINYINT(1) NOT NULL DEFAULT 0,
    data_verificado TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS projetos (
    id CHAR(36) NOT NULL PRIMARY KEY DEFAULT (UUID()),
    nome VARCHAR(100) NOT NULL,
    descricao TEXT NULL,
    data_criado TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_alteracao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ultimo_acesso TIMESTAMP NULL,
    id_usuario CHAR(36) NOT NULL,
    CONSTRAINT fk_projeto_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS diagram_view_states (
    id CHAR(36) NOT NULL PRIMARY KEY DEFAULT (UUID()),
    projeto_id CHAR(36) NOT NULL UNIQUE,
    zoom DOUBLE NOT NULL DEFAULT 1,
    offset_x DOUBLE NOT NULL DEFAULT 0,
    offset_y DOUBLE NOT NULL DEFAULT 0,
    grid_size DOUBLE NOT NULL DEFAULT 32,
    last_saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_viewstate_projeto FOREIGN KEY (projeto_id) REFERENCES projetos(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS diagram_nodes (
    id CHAR(36) NOT NULL PRIMARY KEY DEFAULT (UUID()),
    projeto_id CHAR(36) NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    nome VARCHAR(20) NOT NULL,
    descricao TEXT NULL,
    pos_x DOUBLE NOT NULL,
    pos_y DOUBLE NOT NULL,
    largura DOUBLE NULL,
    altura DOUBLE NULL,
    raio DOUBLE NULL,
    rotacao DOUBLE NOT NULL DEFAULT 0,
    texto JSON NULL,
    metadata JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_node_projeto FOREIGN KEY (projeto_id) REFERENCES projetos(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS diagram_connections (
    id CHAR(36) NOT NULL PRIMARY KEY DEFAULT (UUID()),
    projeto_id CHAR(36) NOT NULL,
    from_node_id CHAR(36) NOT NULL,
    to_node_id CHAR(36) NOT NULL,
    bend_point JSON NULL,
    estilo JSON NULL,
    metadata JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_connection_projeto FOREIGN KEY (projeto_id) REFERENCES projetos(id) ON DELETE CASCADE,
    CONSTRAINT fk_connection_from_node FOREIGN KEY (from_node_id) REFERENCES diagram_nodes(id) ON DELETE CASCADE,
    CONSTRAINT fk_connection_to_node FOREIGN KEY (to_node_id) REFERENCES diagram_nodes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email);
CREATE INDEX IF NOT EXISTS idx_projetos_usuario ON projetos(id_usuario);
CREATE INDEX IF NOT EXISTS idx_nodes_projeto ON diagram_nodes(projeto_id);
CREATE INDEX IF NOT EXISTS idx_nodes_tipo ON diagram_nodes(tipo);
CREATE INDEX IF NOT EXISTS idx_connections_projeto ON diagram_connections(projeto_id);
CREATE INDEX IF NOT EXISTS idx_connections_nodes ON diagram_connections(from_node_id, to_node_id);

DROP TRIGGER IF EXISTS trg_nodes_updated_at;
CREATE TRIGGER trg_nodes_updated_at
BEFORE UPDATE ON diagram_nodes
FOR EACH ROW
SET NEW.updated_at = CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_connections_updated_at;
CREATE TRIGGER trg_connections_updated_at
BEFORE UPDATE ON diagram_connections
FOR EACH ROW
SET NEW.updated_at = CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id CHAR(36) NOT NULL PRIMARY KEY DEFAULT (UUID()),
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used TINYINT(1) NOT NULL DEFAULT 0,
    user_id CHAR(36) NOT NULL,
    CONSTRAINT fk_email_token_usuario FOREIGN KEY (user_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX IF NOT EXISTS idx_email_verification_user ON email_verification_tokens(user_id);
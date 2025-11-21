-- schema.sql
-- Este arquivo é executado automaticamente pelo Spring Boot se spring.jpa.hibernate.ddl-auto=create

-- Tabela de usuários
CREATE TABLE IF NOT EXISTS usuarios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL,
    metadados JSONB,
    data_criado TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    email_verificado BOOLEAN NOT NULL DEFAULT FALSE,
    data_verificado TIMESTAMP
);

-- Tabela de projetos
CREATE TABLE IF NOT EXISTS projetos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    data_criado TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_alteracao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario UUID NOT NULL,
    CONSTRAINT fk_projeto_usuario 
        FOREIGN KEY (id_usuario) 
        REFERENCES usuarios(id) 
        ON DELETE CASCADE
);

-- Tabela de elementos (formas no canvas)
CREATE TABLE IF NOT EXISTS elementos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    conteudo TEXT,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('CIRCULO', 'RETANGULO', 'TRIANGULO', 'TEXTO')),
    coordenadas JSONB,
    relacoes JSONB,
    data_criado TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_alteracao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_projeto UUID NOT NULL,
    CONSTRAINT fk_elemento_projeto 
        FOREIGN KEY (id_projeto) 
        REFERENCES projetos(id) 
        ON DELETE CASCADE
);

-- Índices para melhor performance
CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email);
CREATE INDEX IF NOT EXISTS idx_projetos_usuario ON projetos(id_usuario);
CREATE INDEX IF NOT EXISTS idx_elementos_projeto ON elementos(id_projeto);
CREATE INDEX IF NOT EXISTS idx_elementos_tipo ON elementos(tipo);

-- Tabela tokens de verificação de email
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    user_id UUID NOT NULL,
    CONSTRAINT fk_email_token_usuario FOREIGN KEY (user_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_email_verification_user ON email_verification_tokens(user_id);
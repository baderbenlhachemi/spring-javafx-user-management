CREATE TABLE roles (
    id SERIAL NOT NULL,
    name VARCHAR(20),
    CONSTRAINT roles_pkey PRIMARY KEY (id),
    CONSTRAINT chk_roles_name CHECK (name IN ('ROLE_USER', 'ROLE_ADMIN'))
);

CREATE TABLE users (
    enabled BOOLEAN NOT NULL,
    role_id INTEGER,
    birth_date TIMESTAMP(6),
    created_at TIMESTAMP(6),
    id BIGSERIAL NOT NULL,
    last_login TIMESTAMP(6),
    avatar VARCHAR(255),
    city VARCHAR(255),
    company VARCHAR(255),
    country VARCHAR(255),
    email VARCHAR(255),
    first_name VARCHAR(255),
    job_position VARCHAR(255),
    last_name VARCHAR(255),
    mobile VARCHAR(255),
    password VARCHAR(255),
    username VARCHAR(255),
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE INDEX idx_users_role_id ON users (role_id);
CREATE INDEX idx_users_created_at ON users (created_at);

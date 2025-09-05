CREATE TABLE tb_users
(
    id            UUID PRIMARY KEY,
    user_name     VARCHAR(50) UNIQUE NOT NULL,
    full_name     TEXT               NOT NULL,
    phone         VARCHAR(20) UNIQUE NOT NULL,
    password_hash TEXT               NOT NULL,
    status        VARCHAR(30)        NOT NULL,
    role          VARCHAR(20)        NOT NULL,
    created_at    TIMESTAMP          NOT NULL,
    updated_at     TIMESTAMP          NOT NULL
);
CREATE TABLE tb_refresh_token
(
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    user_id     UUID         NOT NULL UNIQUE,
    CONSTRAINT fk_refresh_token_on_user
        FOREIGN KEY (user_id) REFERENCES tb_users (id) ON DELETE CASCADE
);
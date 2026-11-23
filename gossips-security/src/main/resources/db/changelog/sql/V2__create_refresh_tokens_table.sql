-- Refresh-токены: отзываемые, с ротацией при обмене.
-- В БД хранится только SHA-256 хеш токена — утечка БД не равна утечке токенов.
CREATE TABLE "refresh_tokens" (
    "id"         UUID                     PRIMARY KEY,
    "user_id"    UUID                     NOT NULL REFERENCES "users" ("id") ON DELETE CASCADE,
    "token_hash" VARCHAR(64)              NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    "expires_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "revoked_at" TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX "refresh_tokens_token_hash_unique" ON "refresh_tokens" ("token_hash");
CREATE INDEX "refresh_tokens_user_id_idx" ON "refresh_tokens" ("user_id");

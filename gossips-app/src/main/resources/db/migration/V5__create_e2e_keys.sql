-- Директория публичных ключей E2E: один ключ идентичности на пользователя.
-- Приватные ключи на сервере никогда не хранятся — только то, что клиент опубликовал.
CREATE TABLE "user_keys" (
    "username"    VARCHAR(64)              PRIMARY KEY,
    "public_key"  TEXT                     NOT NULL,  -- base64, публичный ключ (X25519)
    "fingerprint" VARCHAR(64)              NOT NULL,  -- отпечаток ключа (sha256, base64url) — для safety number
    "created_at"  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    "updated_at"  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Кто чей ключ верифицировал. Привязка к отпечатку:
-- смена ключа собеседником автоматически делает верификацию недействительной.
CREATE TABLE "key_verifications" (
    "verifier_username" VARCHAR(64)              NOT NULL,
    "verified_username" VARCHAR(64)              NOT NULL,
    "fingerprint"       VARCHAR(64)              NOT NULL,
    "verified_at"       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY ("verifier_username", "verified_username")
);

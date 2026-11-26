-- Профили пользователей. username — натуральный ключ из JWT (sub), синхронизация с security не нужна:
-- профиль создаётся лениво первым обновлением, до этого читаются дефолты.
CREATE TABLE "profiles" (
    "username"     VARCHAR(64)              PRIMARY KEY,
    "display_name" VARCHAR(64)              NOT NULL,
    "bio"          VARCHAR(500),
    "created_at"   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    "updated_at"   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Пользователи сервиса. Пароль хранится только в виде BCrypt-хеша.
-- Идентификаторы в кавычках и нижнем регистре — единая конвенция проекта
-- (иначе jOOQ codegen через H2 поднимет регистр имён).
CREATE TABLE "users" (
    "id"            UUID                     PRIMARY KEY,
    "username"      VARCHAR(64)              NOT NULL,
    "password_hash" VARCHAR(100)             NOT NULL,
    "created_at"    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

ALTER TABLE "users"
    ADD CONSTRAINT users_username_unique UNIQUE ("username");

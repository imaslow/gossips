-- Идентификаторы в кавычках: jOOQ DDLDatabase парсит миграции через H2,
-- которая по умолчанию приводит имена к верхнему регистру, а PostgreSQL хранит их в нижнем.
CREATE TABLE "messages" (
    "id"         UUID PRIMARY KEY,
    "username"   VARCHAR(64)              NOT NULL,
    "content"    TEXT                     NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

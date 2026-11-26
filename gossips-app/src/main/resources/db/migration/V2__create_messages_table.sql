-- Идентификаторы в кавычках: jOOQ DDLDatabase парсит миграции через H2,
-- которая по умолчанию приводит имена к верхнему регистру, а PostgreSQL хранит их в нижнем.

-- Сообщения диалогов. Каждое сообщение обязано принадлежать диалогу
-- (глобального чата в продукте нет). read_at NULL — собеседник ещё не прочитал;
-- encrypted = true — content это E2E-ciphertext (base64), сервер не видит plaintext.
CREATE TABLE "messages" (
    "id"              UUID                     PRIMARY KEY,
    "username"        VARCHAR(64)              NOT NULL,
    "content"         TEXT                     NOT NULL,
    "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    "conversation_id" UUID                     NOT NULL REFERENCES "conversations" ("id"),
    "read_at"         TIMESTAMP WITH TIME ZONE,
    "encrypted"       BOOLEAN                  NOT NULL DEFAULT false
);

CREATE INDEX "messages_conversation_id_idx" ON "messages" ("conversation_id");

-- Частичный индекс только по непрочитанным — обслуживает историю и счётчики списка диалогов.
CREATE INDEX "messages_unread_idx" ON "messages" ("conversation_id", "username")
    WHERE "read_at" IS NULL;

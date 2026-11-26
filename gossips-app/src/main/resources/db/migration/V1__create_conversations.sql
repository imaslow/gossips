-- Диалоги 1-на-1. Участники — строки в conversation_members (ровно 2 на диалог).
-- Уникальность пары обеспечивается на уровне приложения (findByPair + create).
CREATE TABLE "conversations" (
    "id"         UUID                     PRIMARY KEY,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE "conversation_members" (
    "conversation_id" UUID                     NOT NULL REFERENCES "conversations" ("id") ON DELETE CASCADE,
    "username"        VARCHAR(64)              NOT NULL,
    "joined_at"       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY ("conversation_id", "username")
);

CREATE INDEX "conversation_members_username_idx" ON "conversation_members" ("username");

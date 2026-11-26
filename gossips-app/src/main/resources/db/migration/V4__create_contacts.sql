-- Контакты (ростер): односторонний список пользователя для подсказок при создании диалога.
CREATE TABLE "contacts" (
    "owner_username"   VARCHAR(64)              NOT NULL,
    "contact_username" VARCHAR(64)              NOT NULL,
    "created_at"       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY ("owner_username", "contact_username")
);

CREATE INDEX "contacts_contact_username_idx" ON "contacts" ("contact_username");

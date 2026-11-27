-- Групповые беседы: тип диалога, название (только для групп) и автор создания.
-- pair-диалоги не меняются семантически: ровно 2 участника, уникальность пары — на уровне приложения.
ALTER TABLE "conversations"
    ADD COLUMN "type"       VARCHAR(16) NOT NULL DEFAULT 'pair',
    ADD COLUMN "title"      VARCHAR(128),
    ADD COLUMN "created_by" VARCHAR(64);

ALTER TABLE "conversations"
    ADD CONSTRAINT "conversations_type_check" CHECK ("type" IN ('pair', 'group'));

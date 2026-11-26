#!/bin/sh
# Срабатывает на первый старт контейнера (пустой volume).
# app (сообщения + профили) живёт в отдельной БД, чтобы не смешиваться
# с Liquibase-историей security и будущими миграциями.
set -e
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    -c "CREATE DATABASE gossips_app;"

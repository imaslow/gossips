package org.your.gossips.gossipsapp.adapter.out.persistence;

import java.util.List;

/**
 * Порт для доступа к хранилищу сообщений чата.
 * Реализации (jOOQ, тестовые заглушки) подменяются без изменения вызывающего кода.
 */
public interface MessagePersistence {

    /**
     * Сохраняет сообщение в чат.
     * Идентификатор присваивается реализацией, время создания — из хранилища.
     *
     * @param message сообщение с заполненными {@code username} и {@code content}
     * @return сохранённое сообщение с присвоенным id и временем создания
     */
    StoredMessage insert(StoredMessage message);

    /**
     * Возвращает всю историю сообщений чата.
     *
     * @return все сообщения в порядке возрастания времени создания (старые первыми)
     */
    List<StoredMessage> findAll();
}

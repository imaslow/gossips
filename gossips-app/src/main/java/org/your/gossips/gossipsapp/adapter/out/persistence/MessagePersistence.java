package org.your.gossips.gossipsapp.adapter.out.persistence;

import org.your.gossips.gossipsapp.adapter.out.model.StoredMessage;
import org.your.gossips.gossipsapp.adapter.out.page.MessagePage;

import java.util.UUID;

/**
 * Порт для доступа к хранилищу сообщений диалогов.
 * Реализации (jOOQ, тестовые заглушки) подменяются без изменения вызывающего кода.
 */
public interface MessagePersistence {

    /**
     * Сохраняет сообщение в диалог.
     * Идентификатор присваивается реализацией, время создания — из хранилища.
     *
     * @param message сообщение с заполненными {@code conversationId}, {@code username} и {@code content}
     * @return сохранённое сообщение с присвоенным id и временем создания
     */
    StoredMessage insert(StoredMessage message);

    /**
     * Возвращает страницу истории диалога, новые первыми.
     *
     * @param conversationId диалог
     * @param beforeId       курсор — вернуть сообщения старше этого id; {@code null} — самая свежая страница
     * @param limit          размер страницы
     * @return страница + курсор следующей страницы ({@code null}, если история исчерпана)
     */
    MessagePage findPage(UUID conversationId, UUID beforeId, int limit);

    /**
     * Помечает все непрочитанные сообщения диалога, отправленные не самим читателем, прочитанными.
     *
     * @return число помеченных сообщений
     */
    int markRead(UUID conversationId, String readerUsername);
}

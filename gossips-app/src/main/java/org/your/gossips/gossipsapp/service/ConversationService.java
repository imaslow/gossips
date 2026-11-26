package org.your.gossips.gossipsapp.service;

import org.your.gossips.common.dto.Conversation;
import org.your.gossips.common.dto.Message;
import org.your.gossips.common.dto.MessagesPage;

import java.util.List;
import java.util.UUID;

/**
 * Бизнес-логика диалогов 1-на-1.
 */
public interface ConversationService {

    /**
     * Создаёт (или возвращает существующий) диалог между двумя пользователями.
     *
     * @throws IllegalArgumentException если peer совпадает с username
     */
    Conversation create(String username, String peer);

    /**
     * Все диалоги пользователя, от новых к старым.
     */
    List<Conversation> findAll(String username);

    /**
     * Страница истории диалога. Доступ только участникам.
     *
     * @throws org.your.gossips.gossipsapp.exception.AccessDeniedException если username не участник диалога
     */
    MessagesPage getMessages(String username, UUID conversationId, UUID beforeId, Integer limit);

    /**
     * Сохраняет сообщение в диалог от указанного автора. Доступ только участникам.
     *
     * @param encrypted {@code true} — content это E2E-ciphertext, сервер его не расшифровывает
     * @throws org.your.gossips.gossipsapp.exception.AccessDeniedException если username не участник диалога
     */
    Message sendMessage(String username, UUID conversationId, String content, boolean encrypted);

    /**
     * Помечает все сообщения собеседника в диалоге прочитанными. Доступ только участникам.
     *
     * @throws org.your.gossips.gossipsapp.exception.AccessDeniedException если username не участник диалога
     */
    void markRead(String username, UUID conversationId);
}

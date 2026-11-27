package org.your.gossips.gossipsapp.service;

import org.your.gossips.common.dto.Conversation;
import org.your.gossips.common.dto.ConversationMember;
import org.your.gossips.common.dto.Message;
import org.your.gossips.common.dto.MessagesPage;

import java.util.List;
import java.util.UUID;

/**
 * Бизнес-логика диалогов 1-на-1 и групповых бесед.
 */
public interface ConversationService {

    /**
     * Создаёт (или возвращает существующий) диалог между двумя пользователями.
     *
     * @throws IllegalArgumentException если peer совпадает с username
     */
    Conversation create(String username, String peer);

    /**
     * Создаёт групповую беседу. Создатель добавляется к участникам автоматически.
     *
     * @param usernames логины участников (без создателя), не меньше двух
     * @param title     название группы
     * @return созданная беседа
     * @throws IllegalArgumentException если участников меньше двух, среди них есть создатель или дубликаты
     */
    Conversation createGroup(String username, List<String> usernames, String title);

    /**
     * Участники диалога с отображаемыми именами. Доступ только участникам.
     *
     * @throws org.your.gossips.gossipsapp.exception.AccessDeniedException если username не участник диалога
     */
    List<ConversationMember> getMembers(String username, UUID conversationId);

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

    /**
     * Эфемерный сигнал «печатает»: доставляется остальным участникам через realtime, не хранится.
     *
     * @throws org.your.gossips.gossipsapp.exception.AccessDeniedException если username не участник диалога
     */
    void signalTyping(String username, UUID conversationId);
}

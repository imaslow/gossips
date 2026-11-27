package org.your.gossips.gossipsgateway.service;

import org.your.gossips.common.dto.Conversation;
import org.your.gossips.common.dto.ConversationCreateRequest;
import org.your.gossips.common.dto.ConversationGroupCreateRequest;
import org.your.gossips.common.dto.ConversationMember;
import org.your.gossips.common.dto.Message;
import org.your.gossips.common.dto.MessageRequest;
import org.your.gossips.common.dto.MessagesPage;

import java.util.List;
import java.util.UUID;

/**
 * Диалоги 1-на-1 и групповые беседы: проксирование в app-сервис от имени аутентифицированного пользователя.
 */
public interface ConversationService {

    Conversation create(String username, ConversationCreateRequest request);

    Conversation createGroup(String username, ConversationGroupCreateRequest request);

    List<Conversation> findAll(String username);

    List<ConversationMember> getMembers(String username, UUID conversationId);

    MessagesPage getMessages(String username, UUID conversationId, UUID beforeId, Integer limit);

    /**
     * Отправляет сообщение. Автор — всегда владелец токена: клиентский username в запросе игнорируется.
     */
    Message sendMessage(String username, UUID conversationId, MessageRequest request);

    void markRead(String username, UUID conversationId);

    /** Эфемерный сигнал «печатает» — рассылается остальным участникам через realtime. */
    void signalTyping(String username, UUID conversationId);
}

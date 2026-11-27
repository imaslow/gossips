package org.your.gossips.gossipsapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.your.gossips.common.dto.Conversation;
import org.your.gossips.common.dto.ConversationMember;
import org.your.gossips.common.dto.Message;
import org.your.gossips.common.dto.MessagesPage;
import org.your.gossips.gossipsapp.adapter.out.model.ConversationSummary;
import org.your.gossips.gossipsapp.adapter.out.model.StoredConversation;
import org.your.gossips.gossipsapp.adapter.out.model.StoredMessage;
import org.your.gossips.gossipsapp.adapter.out.model.StoredProfile;
import org.your.gossips.gossipsapp.adapter.realtime.PushNotifier;
import org.your.gossips.gossipsapp.adapter.out.page.MessagePage;
import org.your.gossips.gossipsapp.adapter.out.persistence.ConversationPersistence;
import org.your.gossips.gossipsapp.adapter.out.persistence.MessagePersistence;
import org.your.gossips.gossipsapp.adapter.out.persistence.ProfilePersistence;
import org.your.gossips.gossipsapp.config.PaginationProperties;
import org.your.gossips.gossipsapp.exception.AccessDeniedException;
import org.your.gossips.gossipsapp.mapper.ConversationMapper;
import org.your.gossips.gossipsapp.mapper.MessageMapper;
import org.your.gossips.gossipsapp.service.ConversationService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.your.gossips.gossipsapp.util.PaginationUtils.normalizeLimit;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationPersistence conversationPersistence;
    private final MessagePersistence messagePersistence;
    private final ProfilePersistence profilePersistence;
    private final PushNotifier pushNotifier;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final PaginationProperties paginationProperties;

    @Override
    public Conversation create(String username, String peer) {
        if (peer.equals(username)) {
            throw new IllegalArgumentException("cannot create conversation with yourself");
        }
        StoredConversation conversation = conversationPersistence.findByPair(username, peer)
                .orElseGet(() -> conversationPersistence.createForPair(username, peer));
        // Собеседник узнаёт о новом диалоге только через realtime — иначе у него
        // переписка появится после reload или первого сообщения
        for (String member : conversationPersistence.findMemberUsernames(conversation.id())) {
            if (!member.equals(username)) {
                pushNotifier.pushConversationUpdated(member, conversation.id());
            }
        }
        // lastMessage/unreadCount не заполняем: ответ create — про идентификатор диалога,
        // сводки с историей клиент получает списком
        return conversationMapper.toDto(new ConversationSummary(
                conversation.id(), peer, displayNameOf(peer), conversation.type(), conversation.title(),
                conversation.createdAt(), null, null, false, 0));
    }

    @Override
    public Conversation createGroup(String username, List<String> usernames, String title) {
        // Свойство множества + сохранение порядка: дедупликация без сюрпризов в списке участников
        List<String> others = new ArrayList<>(new LinkedHashSet<>(usernames));
        if (others.contains(username)) {
            throw new IllegalArgumentException("creator must not be in the member list");
        }
        if (others.size() < 2) {
            throw new IllegalArgumentException("group requires at least 2 members besides the creator");
        }
        List<String> members = new ArrayList<>();
        members.add(username);
        members.addAll(others);
        StoredConversation conversation = conversationPersistence.createGroup(username, members, title);
        // Остальные участники узнают о беседе через realtime
        for (String member : conversationPersistence.findMemberUsernames(conversation.id())) {
            if (!member.equals(username)) {
                pushNotifier.pushConversationUpdated(member, conversation.id());
            }
        }
        return conversationMapper.toDto(new ConversationSummary(
                conversation.id(), null, null, conversation.type(), conversation.title(),
                conversation.createdAt(), null, null, false, 0));
    }

    @Override
    public List<ConversationMember> getMembers(String username, UUID conversationId) {
        requireMember(conversationId, username);
        return conversationPersistence.findMemberSummaries(conversationId).stream()
                .map(conversationMapper::toDto)
                .toList();
    }

    @Override
    public List<Conversation> findAll(String username) {
        return conversationPersistence.findAllSummariesFor(username).stream()
                .map(conversationMapper::toDto)
                .toList();
    }

    @Override
    public MessagesPage getMessages(String username, UUID conversationId, UUID beforeId, Integer limit) {
        requireMember(conversationId, username);
        MessagePage page = messagePersistence.findPage(conversationId, beforeId,
                normalizeLimit(limit, paginationProperties.defaultLimit(), paginationProperties.maxLimit()));
        MessagesPage response = new MessagesPage();
        response.setMessages(page.messages().stream().map(messageMapper::toDto).toList());
        response.setNextBeforeId(page.nextBeforeId() != null ? page.nextBeforeId().toString() : null);
        return response;
    }

    @Override
    public Message sendMessage(String username, UUID conversationId, String content, boolean encrypted) {
        requireMember(conversationId, username);
        StoredMessage stored = messagePersistence.insert(
                new StoredMessage(null, username, content, null, conversationId, null, encrypted));
        Message message = messageMapper.toDto(stored);
        // Realtime-доставка всем участникам, кроме автора (у автора сообщение уже на экране)
        for (String member : conversationPersistence.findMemberUsernames(conversationId)) {
            if (!member.equals(username)) {
                pushNotifier.pushMessage(member, conversationId, message);
            }
        }
        return message;
    }

    @Override
    public void markRead(String username, UUID conversationId) {
        requireMember(conversationId, username);
        messagePersistence.markRead(conversationId, username);
        // Авторы сообщений обновляют статусы «прочитано» без перезахода
        for (String member : conversationPersistence.findMemberUsernames(conversationId)) {
            if (!member.equals(username)) {
                pushNotifier.pushRead(member, conversationId, username);
            }
        }
    }

    @Override
    public void signalTyping(String username, UUID conversationId) {
        requireMember(conversationId, username);
        for (String member : conversationPersistence.findMemberUsernames(conversationId)) {
            if (!member.equals(username)) {
                pushNotifier.pushTyping(member, conversationId, username);
            }
        }
    }

    private void requireMember(UUID conversationId, String username) {
        if (!conversationPersistence.isMember(conversationId, username)) {
            throw new AccessDeniedException("Not a member of this conversation");
        }
    }

    /**
     * Отображаемое имя пользователя; при отсутствии профиля — сам логин.
     */
    private String displayNameOf(String username) {
        return profilePersistence.findByUsername(username)
                .map(StoredProfile::displayName)
                .orElse(username);
    }
}

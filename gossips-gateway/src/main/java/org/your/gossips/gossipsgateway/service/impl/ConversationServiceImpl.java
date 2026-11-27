package org.your.gossips.gossipsgateway.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.your.gossips.common.dto.Conversation;
import org.your.gossips.common.dto.ConversationCreateRequest;
import org.your.gossips.common.dto.ConversationGroupCreateRequest;
import org.your.gossips.common.dto.ConversationMember;
import org.your.gossips.common.dto.Message;
import org.your.gossips.common.dto.MessageRequest;
import org.your.gossips.common.dto.MessagesPage;
import org.your.gossips.gossipsgateway.adapter.out.AppServiceClient;
import org.your.gossips.gossipsgateway.service.ConversationService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final AppServiceClient appClient;

    @Override
    public Conversation create(String username, ConversationCreateRequest request) {
        return appClient.createConversation(username, request);
    }

    @Override
    public Conversation createGroup(String username, ConversationGroupCreateRequest request) {
        return appClient.createGroupConversation(username, request);
    }

    @Override
    public List<Conversation> findAll(String username) {
        return appClient.getConversations(username);
    }

    @Override
    public List<ConversationMember> getMembers(String username, UUID conversationId) {
        return appClient.getConversationMembers(username, conversationId);
    }

    @Override
    public MessagesPage getMessages(String username, UUID conversationId, UUID beforeId, Integer limit) {
        return appClient.getConversationMessages(username, conversationId, beforeId, limit);
    }

    @Override
    public Message sendMessage(String username, UUID conversationId, MessageRequest request) {
        // Автор — всегда владелец токена: клиентский username игнорируем
        request.setUsername(username);
        return appClient.sendConversationMessage(username, conversationId, request);
    }

    @Override
    public void markRead(String username, UUID conversationId) {
        appClient.markConversationRead(username, conversationId);
    }

    @Override
    public void signalTyping(String username, UUID conversationId) {
        appClient.signalTyping(username, conversationId);
    }
}

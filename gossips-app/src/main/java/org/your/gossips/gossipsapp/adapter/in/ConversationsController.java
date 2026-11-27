package org.your.gossips.gossipsapp.adapter.in;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.your.gossips.common.dto.Conversation;
import org.your.gossips.common.dto.ConversationCreateRequest;
import org.your.gossips.common.dto.ConversationGroupCreateRequest;
import org.your.gossips.common.dto.ConversationMember;
import org.your.gossips.common.dto.Message;
import org.your.gossips.common.dto.MessageRequest;
import org.your.gossips.common.dto.MessagesPage;
import org.your.gossips.gossipsapp.api.ConversationsApi;
import org.your.gossips.gossipsapp.service.ConversationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ConversationsController implements ConversationsApi {

    private final ConversationService conversationService;

    @Override
    public ResponseEntity<Conversation> createConversation(String xUsername, ConversationCreateRequest conversationCreateRequest) {
        return ResponseEntity.ok(conversationService.create(xUsername, conversationCreateRequest.getUsername()));
    }

    @Override
    public ResponseEntity<Conversation> createGroupConversation(String xUsername,
                                                              ConversationGroupCreateRequest conversationGroupCreateRequest) {
        return ResponseEntity.ok(conversationService.createGroup(
                xUsername, conversationGroupCreateRequest.getUsernames(), conversationGroupCreateRequest.getTitle()));
    }

    @Override
    public ResponseEntity<List<ConversationMember>> getConversationMembers(String xUsername, UUID conversationId) {
        return ResponseEntity.ok(conversationService.getMembers(xUsername, conversationId));
    }

    @Override
    public ResponseEntity<List<Conversation>> getConversations(String xUsername) {
        return ResponseEntity.ok(conversationService.findAll(xUsername));
    }

    @Override
    public ResponseEntity<MessagesPage> getConversationMessages(String xUsername, UUID conversationId, UUID beforeId, Integer limit) {
        return ResponseEntity.ok(conversationService.getMessages(xUsername, conversationId, beforeId, limit));
    }

    @Override
    public ResponseEntity<Message> sendConversationMessage(String xUsername, UUID conversationId, MessageRequest messageRequest) {
        return ResponseEntity.ok(conversationService.sendMessage(
                xUsername, conversationId, messageRequest.getContent(),
                Boolean.TRUE.equals(messageRequest.getEncrypted())));
    }

    @Override
    public ResponseEntity<Void> markConversationRead(String xUsername, UUID conversationId) {
        conversationService.markRead(xUsername, conversationId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> signalTyping(String xUsername, UUID conversationId) {
        conversationService.signalTyping(xUsername, conversationId);
        return ResponseEntity.noContent().build();
    }
}

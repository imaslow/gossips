package org.your.gossips.gossipsgateway.adapter.out;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.your.gossips.common.ApiPaths;
import org.your.gossips.common.dto.Contact;
import org.your.gossips.common.dto.ContactAddRequest;
import org.your.gossips.common.dto.Conversation;
import org.your.gossips.common.dto.ConversationCreateRequest;
import org.your.gossips.common.dto.ConversationGroupCreateRequest;
import org.your.gossips.common.dto.ConversationMember;
import org.your.gossips.common.dto.Message;
import org.your.gossips.common.dto.MessageRequest;
import org.your.gossips.common.dto.MessagesPage;
import org.your.gossips.common.dto.Profile;
import org.your.gossips.common.dto.ProfileUpdateRequest;
import org.your.gossips.common.dto.PublicKeyInfo;
import org.your.gossips.common.dto.PublicKeyUpsertRequest;
import org.your.gossips.common.dto.VerificationInfo;
import org.your.gossips.common.dto.KeyVerifyRequest;
import org.your.gossips.gossipsgateway.common.exception.ServiceUnavailableException;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class AppServiceClient {

    private final RestClient appRestClient;

    public Profile getProfile(String username) {
        return execute(() -> appRestClient.get()
                .uri(ApiPaths.Profiles.ME)
                .header("X-Username", username)
                .retrieve()
                .body(Profile.class));
    }

    public Profile updateProfile(String username, ProfileUpdateRequest request) {
        return execute(() -> appRestClient.put()
                .uri(ApiPaths.Profiles.ME)
                .header("X-Username", username)
                .body(request)
                .retrieve()
                .body(Profile.class));
    }

    public Conversation createConversation(String username, ConversationCreateRequest request) {
        return execute(() -> appRestClient.post()
                .uri(ApiPaths.Conversations.BASE)
                .header("X-Username", username)
                .body(request)
                .retrieve()
                .body(Conversation.class));
    }

    public Conversation createGroupConversation(String username, ConversationGroupCreateRequest request) {
        return execute(() -> appRestClient.post()
                .uri(ApiPaths.Conversations.GROUP)
                .header("X-Username", username)
                .body(request)
                .retrieve()
                .body(Conversation.class));
    }

    public List<Conversation> getConversations(String username) {
        return execute(() -> {
            Conversation[] conversations = appRestClient.get()
                    .uri(ApiPaths.Conversations.BASE)
                    .header("X-Username", username)
                    .retrieve()
                    .body(Conversation[].class);
            return conversations == null ? List.of() : Arrays.asList(conversations);
        });
    }

    public List<ConversationMember> getConversationMembers(String username, UUID conversationId) {
        return execute(() -> {
            ConversationMember[] members = appRestClient.get()
                    .uri(ApiPaths.Conversations.MEMBERS, conversationId)
                    .header("X-Username", username)
                    .retrieve()
                    .body(ConversationMember[].class);
            return members == null ? List.of() : Arrays.asList(members);
        });
    }

    public MessagesPage getConversationMessages(String username, UUID conversationId, UUID beforeId, Integer limit) {
        return execute(() -> appRestClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path(ApiPaths.Conversations.MESSAGES);
                    if (beforeId != null) {
                        builder = builder.queryParam("beforeId", beforeId);
                    }
                    if (limit != null) {
                        builder = builder.queryParam("limit", limit);
                    }
                    return builder.build(conversationId);
                })
                .header("X-Username", username)
                .retrieve()
                .body(MessagesPage.class));
    }

    public Message sendConversationMessage(String username, UUID conversationId, MessageRequest messageRequest) {
        return execute(() -> appRestClient.post()
                .uri(ApiPaths.Conversations.MESSAGES, conversationId)
                .header("X-Username", username)
                .body(messageRequest)
                .retrieve()
                .body(Message.class));
    }

    public List<Contact> getContacts(String username) {
        return execute(() -> {
            Contact[] contacts = appRestClient.get()
                    .uri(ApiPaths.Contacts.BASE)
                    .header("X-Username", username)
                    .retrieve()
                    .body(Contact[].class);
            return contacts == null ? List.of() : Arrays.asList(contacts);
        });
    }

    public Contact addContact(String username, ContactAddRequest request) {
        return execute(() -> appRestClient.post()
                .uri(ApiPaths.Contacts.BASE)
                .header("X-Username", username)
                .body(request)
                .retrieve()
                .body(Contact.class));
    }

    public void removeContact(String username, String contactUsername) {
        execute(() -> appRestClient.delete()
                .uri(ApiPaths.Contacts.BY_USERNAME, contactUsername)
                .header("X-Username", username)
                .retrieve()
                .toBodilessEntity());
    }

    public void markConversationRead(String username, UUID conversationId) {
        execute(() -> appRestClient.post()
                .uri(ApiPaths.Conversations.READ, conversationId)
                .header("X-Username", username)
                .retrieve()
                .toBodilessEntity());
    }

    public void signalTyping(String username, UUID conversationId) {
        execute(() -> appRestClient.post()
                .uri(ApiPaths.Conversations.TYPING, conversationId)
                .header("X-Username", username)
                .retrieve()
                .toBodilessEntity());
    }

    public PublicKeyInfo publishKey(String username, PublicKeyUpsertRequest request) {
        return execute(() -> appRestClient.put()
                .uri(ApiPaths.Keys.ME)
                .header("X-Username", username)
                .body(request)
                .retrieve()
                .body(PublicKeyInfo.class));
    }

    public PublicKeyInfo getOwnKey(String username) {
        return execute(() -> appRestClient.get()
                .uri(ApiPaths.Keys.ME)
                .header("X-Username", username)
                .retrieve()
                .body(PublicKeyInfo.class));
    }

    public PublicKeyInfo getUserKey(String username, String keyOwner) {
        return execute(() -> appRestClient.get()
                .uri(ApiPaths.Keys.USER, keyOwner)
                .header("X-Username", username)
                .retrieve()
                .body(PublicKeyInfo.class));
    }

    public VerificationInfo verifyKey(String username, String keyOwner, KeyVerifyRequest request) {
        return execute(() -> appRestClient.put()
                .uri(ApiPaths.Keys.VERIFY, keyOwner)
                .header("X-Username", username)
                .body(request)
                .retrieve()
                .body(VerificationInfo.class));
    }

    public void unverifyKey(String username, String keyOwner) {
        execute(() -> appRestClient.delete()
                .uri(ApiPaths.Keys.VERIFY, keyOwner)
                .header("X-Username", username)
                .retrieve()
                .toBodilessEntity());
    }

    private <T> T execute(Supplier<T> call) {
        try {
            return call.get();
        } catch (ResourceAccessException e) {
            // Только сбой сети — 503. HTTP-ошибки downstream (4xx/5xx)
            // пробрасываются наверх и маппятся GlobalExceptionHandler'ом gateway.
            throw new ServiceUnavailableException("app", e);
        }
    }
}

package org.your.gossips.gossipsgateway.adapter.in;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.your.gossips.common.dto.AuthResponse;
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
import org.your.gossips.common.dto.RefreshTokenRequest;
import org.your.gossips.common.dto.VerificationInfo;
import org.your.gossips.common.dto.KeyVerifyRequest;
import org.your.gossips.common.dto.UserCredentials;
import org.your.gossips.gossipsgateway.api.GatewayAuthApi;
import org.your.gossips.gossipsgateway.service.AccessService;
import org.your.gossips.gossipsgateway.service.AuthService;
import org.your.gossips.gossipsgateway.service.ContactService;
import org.your.gossips.gossipsgateway.service.ConversationService;
import org.your.gossips.gossipsgateway.service.KeyService;
import org.your.gossips.gossipsgateway.service.ProfileService;

import java.util.List;
import java.util.UUID;

/**
 * Точка входа gateway. JWT-гейт и вся логика — в {@link AccessService} и доменных сервисах,
 * здесь только снятие identity с токена и делегирование.
 */
@RestController
@RequiredArgsConstructor
public class GatewayController implements GatewayAuthApi {

    private final AccessService accessService;
    private final AuthService authService;
    private final ConversationService conversationService;
    private final ContactService contactService;
    private final KeyService keyService;
    private final ProfileService profileService;

    @Override
    public ResponseEntity<AuthResponse> gatewayRegister(UserCredentials userCredentials) {
        return ResponseEntity.ok(authService.register(userCredentials));
    }

    @Override
    public ResponseEntity<AuthResponse> gatewayLogin(UserCredentials userCredentials) {
        return ResponseEntity.ok(authService.login(userCredentials));
    }

    @Override
    public ResponseEntity<AuthResponse> gatewayRefresh(RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authService.refresh(refreshTokenRequest));
    }

    @Override
    public ResponseEntity<Void> gatewayLogout(RefreshTokenRequest refreshTokenRequest) {
        authService.logout(refreshTokenRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Conversation> gatewayCreateConversation(ConversationCreateRequest conversationCreateRequest,
                                                                String authorization) {
        return ResponseEntity.ok(conversationService.create(
                accessService.requireUsername(authorization), conversationCreateRequest));
    }

    @Override
    public ResponseEntity<Conversation> gatewayCreateGroupConversation(ConversationGroupCreateRequest conversationGroupCreateRequest,
                                                                       String authorization) {
        return ResponseEntity.ok(conversationService.createGroup(
                accessService.requireUsername(authorization), conversationGroupCreateRequest));
    }

    @Override
    public ResponseEntity<List<Conversation>> gatewayGetConversations(String authorization) {
        return ResponseEntity.ok(conversationService.findAll(accessService.requireUsername(authorization)));
    }

    @Override
    public ResponseEntity<List<ConversationMember>> gatewayGetConversationMembers(UUID conversationId, String authorization) {
        return ResponseEntity.ok(conversationService.getMembers(
                accessService.requireUsername(authorization), conversationId));
    }

    @Override
    public ResponseEntity<MessagesPage> gatewayGetConversationMessages(UUID conversationId, String authorization,
                                                                       UUID beforeId, Integer limit) {
        return ResponseEntity.ok(conversationService.getMessages(
                accessService.requireUsername(authorization), conversationId, beforeId, limit));
    }

    @Override
    public ResponseEntity<Message> gatewaySendConversationMessage(UUID conversationId, MessageRequest messageRequest,
                                                                String authorization) {
        return ResponseEntity.ok(conversationService.sendMessage(
                accessService.requireUsername(authorization), conversationId, messageRequest));
    }

    @Override
    public ResponseEntity<Void> gatewayMarkConversationRead(UUID conversationId, String authorization) {
        conversationService.markRead(accessService.requireUsername(authorization), conversationId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> gatewaySignalTyping(UUID conversationId, String authorization) {
        conversationService.signalTyping(accessService.requireUsername(authorization), conversationId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PublicKeyInfo> gatewayPublishMyKey(PublicKeyUpsertRequest publicKeyUpsertRequest,
                                                           String authorization) {
        return ResponseEntity.ok(keyService.publish(
                accessService.requireUsername(authorization), publicKeyUpsertRequest));
    }

    @Override
    public ResponseEntity<PublicKeyInfo> gatewayGetMyKey(String authorization) {
        return ResponseEntity.ok(keyService.getOwn(accessService.requireUsername(authorization)));
    }

    @Override
    public ResponseEntity<PublicKeyInfo> gatewayGetUserKey(String username, String authorization) {
        return ResponseEntity.ok(keyService.getFor(
                accessService.requireUsername(authorization), username));
    }

    @Override
    public ResponseEntity<VerificationInfo> gatewayVerifyUserKey(String username, KeyVerifyRequest keyVerifyRequest,
                                                                 String authorization) {
        return ResponseEntity.ok(keyService.verify(
                accessService.requireUsername(authorization), username, keyVerifyRequest));
    }

    @Override
    public ResponseEntity<Void> gatewayUnverifyUserKey(String username, String authorization) {
        keyService.unverify(accessService.requireUsername(authorization), username);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<Contact>> gatewayGetContacts(String authorization) {
        return ResponseEntity.ok(contactService.findAll(accessService.requireUsername(authorization)));
    }

    @Override
    public ResponseEntity<Contact> gatewayAddContact(ContactAddRequest contactAddRequest, String authorization) {
        return ResponseEntity.ok(contactService.add(
                accessService.requireUsername(authorization), contactAddRequest));
    }

    @Override
    public ResponseEntity<Void> gatewayRemoveContact(String username, String authorization) {
        contactService.remove(accessService.requireUsername(authorization), username);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Profile> gatewayGetMyProfile(String authorization) {
        return ResponseEntity.ok(profileService.get(accessService.requireUsername(authorization)));
    }

    @Override
    public ResponseEntity<Profile> gatewayUpdateMyProfile(ProfileUpdateRequest profileUpdateRequest,
                                                          String authorization) {
        return ResponseEntity.ok(profileService.update(
                accessService.requireUsername(authorization), profileUpdateRequest));
    }
}

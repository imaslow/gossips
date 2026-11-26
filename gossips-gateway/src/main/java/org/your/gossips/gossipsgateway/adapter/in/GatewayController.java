package org.your.gossips.gossipsgateway.adapter.in;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.your.gossips.common.dto.AuthResponse;
import org.your.gossips.common.dto.Contact;
import org.your.gossips.common.dto.ContactAddRequest;
import org.your.gossips.common.dto.Conversation;
import org.your.gossips.common.dto.ConversationCreateRequest;
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
import org.your.gossips.common.security.JwtService;
import org.your.gossips.gossipsgateway.adapter.out.AppServiceClient;
import org.your.gossips.gossipsgateway.adapter.out.SecurityServiceClient;
import org.your.gossips.gossipsgateway.api.GatewayAuthApi;
import org.your.gossips.gossipsgateway.common.exception.UnauthorizedException;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GatewayController implements GatewayAuthApi {

    private final SecurityServiceClient securityClient;
    private final AppServiceClient appClient;
    private final JwtService jwtService;

    @Override
    public ResponseEntity<AuthResponse> gatewayRegister(UserCredentials userCredentials) {
        return ResponseEntity.ok(securityClient.register(userCredentials));
    }

    @Override
    public ResponseEntity<AuthResponse> gatewayLogin(UserCredentials userCredentials) {
        return ResponseEntity.ok(securityClient.login(userCredentials));
    }

    @Override
    public ResponseEntity<AuthResponse> gatewayRefresh(RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(securityClient.refresh(refreshTokenRequest));
    }

    @Override
    public ResponseEntity<Void> gatewayLogout(RefreshTokenRequest refreshTokenRequest) {
        securityClient.logout(refreshTokenRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Conversation> gatewayCreateConversation(ConversationCreateRequest conversationCreateRequest, String authorization) {
        if (!jwtService.isTokenValid(authorization)) {
            throw new UnauthorizedException();
        }
        return ResponseEntity.ok(
                appClient.createConversation(jwtService.extractUsername(authorization), conversationCreateRequest));
    }

    @Override
    public ResponseEntity<List<Conversation>> gatewayGetConversations(String authorization) {
        if (!jwtService.isTokenValid(authorization)) {
            throw new UnauthorizedException();
        }
        return ResponseEntity.ok(appClient.getConversations(jwtService.extractUsername(authorization)));
    }

    @Override
    public ResponseEntity<MessagesPage> gatewayGetConversationMessages(UUID conversationId, String authorization, UUID beforeId, Integer limit) {
        if (!jwtService.isTokenValid(authorization)) {
            throw new UnauthorizedException();
        }
        return ResponseEntity.ok(appClient.getConversationMessages(
                jwtService.extractUsername(authorization), conversationId, beforeId, limit));
    }

    @Override
    public ResponseEntity<Message> gatewaySendConversationMessage(UUID conversationId, MessageRequest messageRequest,
                                                                  String authorization) {
        if (!jwtService.isTokenValid(authorization)) {
            throw new UnauthorizedException();
        }
        String username = jwtService.extractUsername(authorization);
        // Автор — всегда владелец токена: клиентский username игнорируем
        messageRequest.setUsername(username);
        return ResponseEntity.ok(appClient.sendConversationMessage(username, conversationId, messageRequest));
    }

    @Override
    public ResponseEntity<Void> gatewayMarkConversationRead(UUID conversationId, String authorization) {
        if (!jwtService.isTokenValid(authorization)) {
            throw new UnauthorizedException();
        }
        appClient.markConversationRead(jwtService.extractUsername(authorization), conversationId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PublicKeyInfo> gatewayPublishMyKey(PublicKeyUpsertRequest publicKeyUpsertRequest, String authorization) {
        if (!jwtService.isTokenValid(authorization)) {
            throw new UnauthorizedException();
        }
        return ResponseEntity.ok(
                appClient.publishKey(jwtService.extractUsername(authorization), publicKeyUpsertRequest));
    }

    @Override
    public ResponseEntity<PublicKeyInfo> gatewayGetMyKey(String authorization) {
        if (!jwtService.isTokenValid(authorization)) {
            throw new UnauthorizedException();
        }
        return ResponseEntity.ok(appClient.getOwnKey(jwtService.extractUsername(authorization)));
    }

    @Override
    public ResponseEntity<PublicKeyInfo> gatewayGetUserKey(String username, String authorization) {
        if (!jwtService.isTokenValid(authorization)) {
            throw new UnauthorizedException();
        }
        return ResponseEntity.ok(
                appClient.getUserKey(jwtService.extractUsername(authorization), username));
    }

    @Override
    public ResponseEntity<VerificationInfo> gatewayVerifyUserKey(String username, KeyVerifyRequest keyVerifyRequest,
                                                                 String authorization) {
        if (!jwtService.isTokenValid(authorization)) {
            throw new UnauthorizedException();
        }
        return ResponseEntity.ok(appClient.verifyKey(
                jwtService.extractUsername(authorization), username, keyVerifyRequest));
    }

    @Override
    public ResponseEntity<Void> gatewayUnverifyUserKey(String username, String authorization) {
        if (!jwtService.isTokenValid(authorization)) {
            throw new UnauthorizedException();
        }
        appClient.unverifyKey(jwtService.extractUsername(authorization), username);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<Contact>> gatewayGetContacts(String authorization) {
        if (!jwtService.isTokenValid(authorization)) {
            throw new UnauthorizedException();
        }
        return ResponseEntity.ok(appClient.getContacts(jwtService.extractUsername(authorization)));
    }

    @Override
    public ResponseEntity<Contact> gatewayAddContact(ContactAddRequest contactAddRequest, String authorization) {
        if (!jwtService.isTokenValid(authorization)) {
            throw new UnauthorizedException();
        }
        return ResponseEntity.ok(
                appClient.addContact(jwtService.extractUsername(authorization), contactAddRequest));
    }

    @Override
    public ResponseEntity<Void> gatewayRemoveContact(String username, String authorization) {
        if (!jwtService.isTokenValid(authorization)) {
            throw new UnauthorizedException();
        }
        appClient.removeContact(jwtService.extractUsername(authorization), username);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Profile> gatewayGetMyProfile(String authorization) {
        if (!jwtService.isTokenValid(authorization)) {
            throw new UnauthorizedException();
        }
        return ResponseEntity.ok(appClient.getProfile(jwtService.extractUsername(authorization)));
    }

    @Override
    public ResponseEntity<Profile> gatewayUpdateMyProfile(ProfileUpdateRequest profileUpdateRequest, String authorization) {
        if (!jwtService.isTokenValid(authorization)) {
            throw new UnauthorizedException();
        }
        return ResponseEntity.ok(
                appClient.updateProfile(jwtService.extractUsername(authorization), profileUpdateRequest));
    }
}

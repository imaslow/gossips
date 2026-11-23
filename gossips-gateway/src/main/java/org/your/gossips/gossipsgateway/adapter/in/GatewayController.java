package org.your.gossips.gossipsgateway.adapter.in;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.your.gossips.common.dto.AuthResponse;
import org.your.gossips.common.dto.Message;
import org.your.gossips.common.dto.MessageRequest;
import org.your.gossips.common.dto.RefreshTokenRequest;
import org.your.gossips.common.dto.UserCredentials;
import org.your.gossips.common.security.JwtService;
import org.your.gossips.gossipsgateway.adapter.out.AppServiceClient;
import org.your.gossips.gossipsgateway.adapter.out.SecurityServiceClient;
import org.your.gossips.gossipsgateway.api.GatewayAuthApi;

import java.util.List;

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
    public ResponseEntity<List<Message>> gatewayGetMessages(String authorization) {
        if (!jwtService.isTokenValid(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(appClient.getMessages(authorization));
    }

    @Override
    public ResponseEntity<Message> gatewaySendMessage(MessageRequest messageRequest, String authorization) {
        if (!jwtService.isTokenValid(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(appClient.sendMessage(authorization, messageRequest));
    }
}

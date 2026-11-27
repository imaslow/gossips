package org.your.gossips.gossipssecurity.adapter.in;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.your.gossips.common.dto.AuthResponse;
import org.your.gossips.common.dto.RefreshTokenRequest;
import org.your.gossips.common.dto.UserCredentials;
import org.your.gossips.gossipssecurity.api.SecurityControllerApi;
import org.your.gossips.gossipssecurity.service.AuthService;
import org.your.gossips.gossipssecurity.service.TokenService;

/**
 * Точка входа auth API. Вся логика — в {@link AuthService} и {@link TokenService}.
 */
@RestController
@RequiredArgsConstructor
public class SecurityController implements SecurityControllerApi {

    private final AuthService authService;
    private final TokenService tokenService;

    @Override
    public ResponseEntity<AuthResponse> registerUser(UserCredentials userCredentials) {
        return ResponseEntity.ok(authService.register(userCredentials));
    }

    @Override
    public ResponseEntity<AuthResponse> loginUser(UserCredentials userCredentials) {
        return ResponseEntity.ok(authService.login(userCredentials));
    }

    @Override
    public ResponseEntity<AuthResponse> refreshTokens(RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(tokenService.rotate(refreshTokenRequest.getRefreshToken()));
    }

    @Override
    public ResponseEntity<Void> logout(RefreshTokenRequest refreshTokenRequest) {
        tokenService.revoke(refreshTokenRequest.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}

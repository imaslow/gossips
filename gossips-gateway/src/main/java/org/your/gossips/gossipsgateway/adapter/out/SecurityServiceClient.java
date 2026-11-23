package org.your.gossips.gossipsgateway.adapter.out;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.your.gossips.common.ApiPaths;
import org.your.gossips.common.dto.AuthResponse;
import org.your.gossips.common.dto.RefreshTokenRequest;
import org.your.gossips.common.dto.UserCredentials;
import org.your.gossips.gossipsgateway.common.exception.ServiceUnavailableException;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class SecurityServiceClient {

    private final RestClient securityRestClient;

    public AuthResponse register(UserCredentials userCredentials) {
        return execute(() -> securityRestClient.post()
                .uri(ApiPaths.Auth.REGISTER)
                .body(userCredentials)
                .retrieve()
                .body(AuthResponse.class));
    }

    public AuthResponse login(UserCredentials userCredentials) {
        return execute(() -> securityRestClient.post()
                .uri(ApiPaths.Auth.LOGIN)
                .body(userCredentials)
                .retrieve()
                .body(AuthResponse.class));
    }

    public AuthResponse refresh(RefreshTokenRequest refreshTokenRequest) {
        return execute(() -> securityRestClient.post()
                .uri(ApiPaths.Auth.REFRESH)
                .body(refreshTokenRequest)
                .retrieve()
                .body(AuthResponse.class));
    }

    public void logout(RefreshTokenRequest refreshTokenRequest) {
        execute(() -> securityRestClient.post()
                .uri(ApiPaths.Auth.LOGOUT)
                .body(refreshTokenRequest)
                .retrieve()
                .toBodilessEntity());
    }

    private <T> T execute(Supplier<T> call) {
        try {
            return call.get();
        } catch (ResourceAccessException e) {
            // Только сбой сети — 503. HTTP-ошибки downstream (401/409/...)
            // пробрасываются наверх и маппятся GlobalExceptionHandler'ом gateway.
            throw new ServiceUnavailableException("security", e);
        }
    }
}

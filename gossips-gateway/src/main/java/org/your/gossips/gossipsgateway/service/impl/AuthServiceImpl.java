package org.your.gossips.gossipsgateway.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.your.gossips.common.dto.AuthResponse;
import org.your.gossips.common.dto.RefreshTokenRequest;
import org.your.gossips.common.dto.UserCredentials;
import org.your.gossips.gossipsgateway.adapter.out.SecurityServiceClient;
import org.your.gossips.gossipsgateway.service.AuthService;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SecurityServiceClient securityClient;

    @Override
    public AuthResponse register(UserCredentials userCredentials) {
        return securityClient.register(userCredentials);
    }

    @Override
    public AuthResponse login(UserCredentials userCredentials) {
        return securityClient.login(userCredentials);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest refreshTokenRequest) {
        return securityClient.refresh(refreshTokenRequest);
    }

    @Override
    public void logout(RefreshTokenRequest refreshTokenRequest) {
        securityClient.logout(refreshTokenRequest);
    }
}

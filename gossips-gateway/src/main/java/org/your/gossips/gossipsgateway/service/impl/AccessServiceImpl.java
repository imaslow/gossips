package org.your.gossips.gossipsgateway.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.your.gossips.common.security.JwtService;
import org.your.gossips.gossipsgateway.common.exception.UnauthorizedException;
import org.your.gossips.gossipsgateway.service.AccessService;

/**
 * Локальная валидация JWT (подпись проверяется тем же секретом, что и в security).
 */
@Service
@RequiredArgsConstructor
public class AccessServiceImpl implements AccessService {

    private final JwtService jwtService;

    @Override
    public String requireUsername(String authorization) {
        if (authorization == null || !jwtService.isTokenValid(authorization)) {
            throw new UnauthorizedException();
        }
        return jwtService.extractUsername(authorization);
    }
}

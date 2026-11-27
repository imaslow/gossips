package org.your.gossips.gossipssecurity.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.your.gossips.common.dto.AuthResponse;
import org.your.gossips.common.security.JwtProperties;
import org.your.gossips.common.security.JwtService;
import org.your.gossips.gossipssecurity.adapter.out.model.StoredRefreshToken;
import org.your.gossips.gossipssecurity.adapter.out.model.StoredUser;
import org.your.gossips.gossipssecurity.adapter.out.persistence.RefreshTokenPersistence;
import org.your.gossips.gossipssecurity.adapter.out.persistence.UserPersistence;
import org.your.gossips.gossipssecurity.common.exception.InvalidRefreshTokenException;
import org.your.gossips.gossipssecurity.service.TokenService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenPersistence refreshTokenPersistence;
    private final UserPersistence userPersistence;

    @Override
    public AuthResponse issueTokens(UUID userId, String username) {
        return buildResponse(userId, username, generateRefreshToken());
    }

    @Override
    public AuthResponse rotate(String refreshToken) {
        StoredRefreshToken stored = requireActive(refreshToken);
        refreshTokenPersistence.revoke(stored.id());
        StoredUser user = userPersistence.findById(stored.userId())
                .orElseThrow(InvalidRefreshTokenException::new);
        return buildResponse(user.id(), user.username(), generateRefreshToken());
    }

    @Override
    public void revoke(String refreshToken) {
        StoredRefreshToken stored = requireActive(refreshToken);
        refreshTokenPersistence.revoke(stored.id());
    }

    private AuthResponse buildResponse(UUID userId, String username, String rawRefreshToken) {
        refreshTokenPersistence.insert(new StoredRefreshToken(
                null,
                userId,
                hash(rawRefreshToken),
                null,
                OffsetDateTime.now().plus(jwtProperties.refreshExpiration()),
                null
        ));

        AuthResponse response = new AuthResponse();
        response.setToken(jwtService.generateToken(username));
        response.setRefreshToken(rawRefreshToken);
        response.setUsername(username);
        return response;
    }

    private StoredRefreshToken requireActive(String refreshToken) {
        return refreshTokenPersistence.findByHash(hash(refreshToken))
                .filter(StoredRefreshToken::isActive)
                .orElseThrow(InvalidRefreshTokenException::new);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

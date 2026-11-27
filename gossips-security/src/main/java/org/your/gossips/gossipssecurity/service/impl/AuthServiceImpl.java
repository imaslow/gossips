package org.your.gossips.gossipssecurity.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.your.gossips.common.dto.AuthResponse;
import org.your.gossips.common.dto.UserCredentials;
import org.your.gossips.gossipssecurity.adapter.out.model.StoredUser;
import org.your.gossips.gossipssecurity.adapter.out.persistence.UserPersistence;
import org.your.gossips.gossipssecurity.common.exception.UserAlreadyExistsException;
import org.your.gossips.gossipssecurity.service.AuthService;
import org.your.gossips.gossipssecurity.service.TokenService;

/**
 * Регистрация и вход. Пароли хешируются BCrypt, сырые никуда дальше не уходят.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserPersistence userPersistence;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    public AuthResponse register(UserCredentials userCredentials) {
        userPersistence.findByUsername(userCredentials.getUsername())
                .ifPresent(user -> {
                    throw new UserAlreadyExistsException(user.username());
                });

        String passwordHash = passwordEncoder.encode(userCredentials.getPassword());
        StoredUser saved = userPersistence.insert(new StoredUser(null, userCredentials.getUsername(), passwordHash, null));
        return tokenService.issueTokens(saved.id(), saved.username());
    }

    @Override
    public AuthResponse login(UserCredentials userCredentials) {
        StoredUser user = userPersistence.findByUsername(userCredentials.getUsername())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!passwordEncoder.matches(userCredentials.getPassword(), user.passwordHash())) {
            throw new BadCredentialsException("Invalid password");
        }
        return tokenService.issueTokens(user.id(), user.username());
    }
}

package org.your.gossips.gossipssecurity.adapter.in;

import org.your.gossips.gossipssecurity.api.SecurityControllerApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RestController;
import org.your.gossips.common.dto.AuthResponse;
import org.your.gossips.common.dto.RefreshTokenRequest;
import org.your.gossips.common.dto.UserCredentials;
import org.your.gossips.gossipssecurity.adapter.out.persistence.model.StoredUser;
import org.your.gossips.gossipssecurity.adapter.out.persistence.UserPersistence;
import org.your.gossips.gossipssecurity.application.TokenService;
import org.your.gossips.gossipssecurity.common.exception.UserAlreadyExistsException;

@RestController
@RequiredArgsConstructor
public class SecurityController implements SecurityControllerApi {

    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserPersistence userPersistence;

    @Override
    public ResponseEntity<AuthResponse> registerUser(UserCredentials userCredentials) {
        userPersistence.findByUsername(userCredentials.getUsername())
                .ifPresent(user -> {
                    throw new UserAlreadyExistsException(user.username());
                });

        String passwordHash = passwordEncoder.encode(userCredentials.getPassword());
        StoredUser saved = userPersistence.insert(new StoredUser(null, userCredentials.getUsername(), passwordHash, null));
        return ResponseEntity.ok(tokenService.issueTokens(saved.id(), saved.username()));
    }

    @Override
    public ResponseEntity<AuthResponse> loginUser(UserCredentials userCredentials) {
        StoredUser user = userPersistence.findByUsername(userCredentials.getUsername())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!passwordEncoder.matches(userCredentials.getPassword(), user.passwordHash())) {
            throw new BadCredentialsException("Invalid password");
        }
        return ResponseEntity.ok(tokenService.issueTokens(user.id(), user.username()));
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

package org.your.gossips.gossipsapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.your.gossips.common.dto.KeyVerifyRequest;
import org.your.gossips.common.dto.PublicKeyInfo;
import org.your.gossips.common.dto.PublicKeyUpsertRequest;
import org.your.gossips.common.dto.VerificationInfo;
import org.your.gossips.gossipsapp.adapter.out.model.StoredKey;
import org.your.gossips.gossipsapp.adapter.out.model.StoredKeyVerification;
import org.your.gossips.gossipsapp.adapter.out.persistence.KeyPersistence;
import org.your.gossips.gossipsapp.exception.KeyNotFoundException;
import org.your.gossips.gossipsapp.mapper.KeyMapper;
import org.your.gossips.gossipsapp.service.KeyService;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class KeyServiceImpl implements KeyService {

    private final KeyPersistence keyPersistence;
    private final KeyMapper keyMapper;

    @Override
    public PublicKeyInfo publish(String username, PublicKeyUpsertRequest request) {
        StoredKey stored = keyPersistence.upsert(new StoredKey(
                username,
                request.getPublicKey(),
                request.getFingerprint(),
                OffsetDateTime.now(),
                OffsetDateTime.now()));
        return keyMapper.toDto(stored);
    }

    @Override
    public PublicKeyInfo getOwn(String username) {
        PublicKeyInfo dto = keyMapper.toDto(requireKey(username));
        dto.setVerified(false);
        return dto;
    }

    @Override
    public PublicKeyInfo getFor(String caller, String username) {
        StoredKey key = requireKey(username);
        PublicKeyInfo dto = keyMapper.toDto(key);
        dto.setVerified(isVerifiedWith(caller, username, key.fingerprint()));
        return dto;
    }

    @Override
    public VerificationInfo verify(String caller, String username, KeyVerifyRequest request) {
        StoredKeyVerification saved = keyPersistence.upsertVerification(
                caller, username, request.getFingerprint());
        return keyMapper.toDto(saved);
    }

    @Override
    public void unverify(String caller, String username) {
        keyPersistence.deleteVerification(caller, username);
    }

    private StoredKey requireKey(String username) {
        return keyPersistence.findByUsername(username)
                .orElseThrow(() -> new KeyNotFoundException(username));
    }

    /**
     * Верификация действительна, только если caller верифицировал ровно этот отпечаток:
     * смена ключа собеседником делает старую верификацию недействительной.
     */
    private boolean isVerifiedWith(String caller, String username, String fingerprint) {
        return keyPersistence.findVerification(caller, username)
                .map(verification -> verification.fingerprint().equals(fingerprint))
                .orElse(false);
    }
}

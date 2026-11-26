package org.your.gossips.gossipsapp.adapter.out.model;

import java.time.OffsetDateTime;

/**
 * Запись верификации ключа: verifier подтвердил отпечаток ключа verified.
 * Зеркалит таблицу key_verifications.
 */
public record StoredKeyVerification(
        String verifierUsername,
        String verifiedUsername,
        String fingerprint,
        OffsetDateTime verifiedAt) {
}

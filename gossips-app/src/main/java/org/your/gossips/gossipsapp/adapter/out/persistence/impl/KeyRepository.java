package org.your.gossips.gossipsapp.adapter.out.persistence.impl;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.your.gossips.gossipsapp.adapter.out.model.StoredKey;
import org.your.gossips.gossipsapp.adapter.out.model.StoredKeyVerification;
import org.your.gossips.gossipsapp.adapter.out.persistence.KeyPersistence;
import org.your.gossips.gossipsapp.jooq.tables.records.KeyVerificationsRecord;
import org.your.gossips.gossipsapp.jooq.tables.records.UserKeysRecord;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.your.gossips.gossipsapp.jooq.tables.KeyVerifications.KEY_VERIFICATIONS;
import static org.your.gossips.gossipsapp.jooq.tables.UserKeys.USER_KEYS;

@Repository
@RequiredArgsConstructor
public class KeyRepository implements KeyPersistence {

    private final DSLContext dsl;

    @Override
    public StoredKey upsert(StoredKey key) {
        Optional<UserKeysRecord> existing = dsl.selectFrom(USER_KEYS)
                .where(USER_KEYS.USERNAME.eq(key.username()))
                .fetchOptional();
        if (existing.isPresent()) {
            return dsl.update(USER_KEYS)
                    .set(USER_KEYS.PUBLIC_KEY, key.publicKey())
                    .set(USER_KEYS.FINGERPRINT, key.fingerprint())
                    .set(USER_KEYS.UPDATED_AT, OffsetDateTime.now())
                    .where(USER_KEYS.USERNAME.eq(key.username()))
                    .returning()
                    .fetchOneInto(StoredKey.class);
        }
        return dsl.insertInto(USER_KEYS)
                .set(USER_KEYS.USERNAME, key.username())
                .set(USER_KEYS.PUBLIC_KEY, key.publicKey())
                .set(USER_KEYS.FINGERPRINT, key.fingerprint())
                .returning()
                .fetchOneInto(StoredKey.class);
    }

    @Override
    public Optional<StoredKey> findByUsername(String username) {
        return dsl.selectFrom(USER_KEYS)
                .where(USER_KEYS.USERNAME.eq(username))
                .fetchOptionalInto(StoredKey.class);
    }

    @Override
    public Optional<StoredKeyVerification> findVerification(String verifierUsername, String verifiedUsername) {
        return dsl.selectFrom(KEY_VERIFICATIONS)
                .where(KEY_VERIFICATIONS.VERIFIER_USERNAME.eq(verifierUsername))
                .and(KEY_VERIFICATIONS.VERIFIED_USERNAME.eq(verifiedUsername))
                .fetchOptionalInto(StoredKeyVerification.class);
    }

    @Override
    public StoredKeyVerification upsertVerification(String verifierUsername, String verifiedUsername, String fingerprint) {
        Optional<KeyVerificationsRecord> existing = dsl.selectFrom(KEY_VERIFICATIONS)
                .where(KEY_VERIFICATIONS.VERIFIER_USERNAME.eq(verifierUsername))
                .and(KEY_VERIFICATIONS.VERIFIED_USERNAME.eq(verifiedUsername))
                .fetchOptional();
        if (existing.isPresent()) {
            return dsl.update(KEY_VERIFICATIONS)
                    .set(KEY_VERIFICATIONS.FINGERPRINT, fingerprint)
                    .set(KEY_VERIFICATIONS.VERIFIED_AT, OffsetDateTime.now())
                    .where(KEY_VERIFICATIONS.VERIFIER_USERNAME.eq(verifierUsername))
                    .and(KEY_VERIFICATIONS.VERIFIED_USERNAME.eq(verifiedUsername))
                    .returning()
                    .fetchOneInto(StoredKeyVerification.class);
        }
        KeyVerificationsRecord record = dsl.insertInto(KEY_VERIFICATIONS)
                .set(KEY_VERIFICATIONS.VERIFIER_USERNAME, verifierUsername)
                .set(KEY_VERIFICATIONS.VERIFIED_USERNAME, verifiedUsername)
                .set(KEY_VERIFICATIONS.FINGERPRINT, fingerprint)
                .returning()
                .fetchOne();
        return new StoredKeyVerification(
                record.getVerifierUsername(), record.getVerifiedUsername(),
                record.getFingerprint(), record.getVerifiedAt());
    }

    @Override
    public void deleteVerification(String verifierUsername, String verifiedUsername) {
        dsl.deleteFrom(KEY_VERIFICATIONS)
                .where(KEY_VERIFICATIONS.VERIFIER_USERNAME.eq(verifierUsername))
                .and(KEY_VERIFICATIONS.VERIFIED_USERNAME.eq(verifiedUsername))
                .execute();
    }
}

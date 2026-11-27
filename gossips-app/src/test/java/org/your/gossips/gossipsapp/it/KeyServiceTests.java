package org.your.gossips.gossipsapp.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.your.gossips.common.dto.KeyVerifyRequest;
import org.your.gossips.common.dto.PublicKeyInfo;
import org.your.gossips.common.dto.PublicKeyUpsertRequest;
import org.your.gossips.gossipsapp.exception.KeyNotFoundException;
import org.your.gossips.gossipsapp.service.KeyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционные тесты E2E-директории ключей: публикация, верификация отпечатка,
 * инвалидация верификации при ротации ключа.
 */
class KeyServiceTests extends PostgresIT {

    private static final PublicKeyUpsertRequest KEY_V1 = new PublicKeyUpsertRequest(
            "public-key-material-v1-0123456789abcdef", "fp-v1-0123");
    private static final PublicKeyUpsertRequest KEY_V2 = new PublicKeyUpsertRequest(
            "public-key-material-v2-fedcba9876543210", "fp-v2-4567");

    @Autowired
    private KeyService keyService;

    @Test
    void ownKeyIsPublishedAndNeverSelfVerified() {
        PublicKeyInfo published = keyService.publish("key-bob", KEY_V1);

        assertThat(published.getPublicKey()).isEqualTo(KEY_V1.getPublicKey());
        assertThat(published.getFingerprint()).isEqualTo(KEY_V1.getFingerprint());

        PublicKeyInfo own = keyService.getOwn("key-bob");
        assertThat(own.getFingerprint()).isEqualTo(KEY_V1.getFingerprint());
        assertThat(own.getVerified()).isFalse();
    }

    @Test
    void unknownKeyThrows() {
        assertThatThrownBy(() -> keyService.getOwn("key-ghost"))
                .isInstanceOf(KeyNotFoundException.class);
        assertThatThrownBy(() -> keyService.getFor("key-alice", "key-ghost"))
                .isInstanceOf(KeyNotFoundException.class);
    }

    @Test
    void verificationSurvivesOnlyForMatchingFingerprint() {
        keyService.publish("rotate-bob", KEY_V1);
        assertThat(keyService.getFor("rotate-alice", "rotate-bob").getVerified()).isFalse();

        keyService.verify("rotate-alice", "rotate-bob", new KeyVerifyRequest(KEY_V1.getFingerprint()));
        assertThat(keyService.getFor("rotate-alice", "rotate-bob").getVerified()).isTrue();

        // ротация ключа меняет отпечаток — старая верификация больше не считается
        keyService.publish("rotate-bob", KEY_V2);
        assertThat(keyService.getFor("rotate-alice", "rotate-bob").getVerified()).isFalse();

        // верификация нового отпечатка восстанавливает флаг
        keyService.verify("rotate-alice", "rotate-bob", new KeyVerifyRequest(KEY_V2.getFingerprint()));
        assertThat(keyService.getFor("rotate-alice", "rotate-bob").getVerified()).isTrue();
    }

    @Test
    void unverifyRemovesVerification() {
        keyService.publish("unverify-bob", KEY_V1);
        keyService.verify("unverify-alice", "unverify-bob", new KeyVerifyRequest(KEY_V1.getFingerprint()));
        keyService.unverify("unverify-alice", "unverify-bob");

        assertThat(keyService.getFor("unverify-alice", "unverify-bob").getVerified()).isFalse();
    }
}

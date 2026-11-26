package org.your.gossips.gossipsapp.service;

import org.your.gossips.common.dto.KeyVerifyRequest;
import org.your.gossips.common.dto.PublicKeyInfo;
import org.your.gossips.common.dto.PublicKeyUpsertRequest;
import org.your.gossips.common.dto.VerificationInfo;

/**
 * Бизнес-логика директории публичных ключей E2E.
 * Приватные ключи в сервисе никогда не появляются — только публикация/выдача публичных.
 */
public interface KeyService {

    /**
     * Публикует (создаёт или заменяет) публичный ключ пользователя.
     * Замена ключа обнуляет чужие верификации этого ключа (меняется отпечаток).
     */
    PublicKeyInfo publish(String username, PublicKeyUpsertRequest request);

    /**
     * Собственный опубликованный ключ.
     *
     * @throws org.your.gossips.gossipsapp.exception.KeyNotFoundException если ключ не опубликован
     */
    PublicKeyInfo getOwn(String username);

    /**
     * Ключ пользователя с флагом verified — верифицировал ли caller именно этот отпечаток.
     *
     * @throws org.your.gossips.gossipsapp.exception.KeyNotFoundException если пользователь не публиковал ключ
     */
    PublicKeyInfo getFor(String caller, String username);

    /**
     * Сохраняет верификацию ключа пользователя.
     */
    VerificationInfo verify(String caller, String username, KeyVerifyRequest request);

    /**
     * Удаляет верификацию ключа пользователя. Идемпотентно.
     */
    void unverify(String caller, String username);
}

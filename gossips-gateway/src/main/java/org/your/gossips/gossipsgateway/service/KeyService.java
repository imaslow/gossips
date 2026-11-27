package org.your.gossips.gossipsgateway.service;

import org.your.gossips.common.dto.KeyVerifyRequest;
import org.your.gossips.common.dto.PublicKeyInfo;
import org.your.gossips.common.dto.PublicKeyUpsertRequest;
import org.your.gossips.common.dto.VerificationInfo;

/**
 * Директория E2E-ключей: проксирование в app-сервис от имени аутентифицированного пользователя.
 */
public interface KeyService {

    PublicKeyInfo publish(String username, PublicKeyUpsertRequest request);

    PublicKeyInfo getOwn(String username);

    PublicKeyInfo getFor(String caller, String username);

    VerificationInfo verify(String caller, String username, KeyVerifyRequest request);

    void unverify(String caller, String username);
}

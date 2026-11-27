package org.your.gossips.gossipsgateway.service;

import org.your.gossips.common.dto.Profile;
import org.your.gossips.common.dto.ProfileUpdateRequest;

/**
 * Профили: проксирование в app-сервис от имени аутентифицированного пользователя.
 */
public interface ProfileService {

    Profile get(String username);

    Profile update(String username, ProfileUpdateRequest request);
}

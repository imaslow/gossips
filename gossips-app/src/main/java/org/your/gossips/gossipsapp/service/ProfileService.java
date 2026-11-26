package org.your.gossips.gossipsapp.service;

import org.your.gossips.common.dto.Profile;
import org.your.gossips.common.dto.ProfileUpdateRequest;

/**
 * Бизнес-логика профилей пользователей.
 */
public interface ProfileService {

    /**
     * Профиль пользователя. Если ещё не создан — дефолт (displayName = username), не сохраняя.
     */
    Profile get(String username);

    /**
     * Создаёт или обновляет профиль пользователя.
     */
    Profile update(String username, ProfileUpdateRequest request);
}

package org.your.gossips.gossipsapp.adapter.out.persistence;

import org.your.gossips.gossipsapp.adapter.out.model.StoredProfile;

import java.util.Optional;

/**
 * Порт для доступа к хранилищу профилей пользователей.
 */
public interface ProfilePersistence {

    /**
     * Ищет профиль по логину.
     *
     * @param username логин
     * @return профиль или {@link Optional#empty()}, если пользователь ещё не сохранял профиль
     */
    Optional<StoredProfile> findByUsername(String username);

    /**
     * Создаёт или обновляет профиль (upsert по username).
     * Временные метки проставляются хранилищем.
     *
     * @param profile профиль с заполненными {@code username}, {@code displayName} и {@code bio}
     * @return сохранённый профиль
     */
    StoredProfile upsert(StoredProfile profile);
}

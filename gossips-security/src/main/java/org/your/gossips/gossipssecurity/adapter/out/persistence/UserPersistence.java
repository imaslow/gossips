package org.your.gossips.gossipssecurity.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

/**
 * Порт для доступа к хранилищу пользователей.
 * Пароли в этом порту всегда представлены хешами, никогда сырыми.
 */
public interface UserPersistence {

    /**
     * Ищет пользователя по логину.
     *
     * @param username логин
     * @return пользователь или {@link Optional#empty()}, если не найден
     */
    /**
     * Ищет пользователя по идентификатору.
     *
     * @param id идентификатор пользователя
     * @return пользователь или {@link Optional#empty()}, если не найден
     */
    Optional<StoredUser> findById(UUID id);

    /**
     * Ищет пользователя по логину.
     *
     * @param username логин
     * @return пользователь или {@link Optional#empty()}, если не найден
     */
    Optional<StoredUser> findByUsername(String username);

    /**
     * Сохраняет нового пользователя.
     * Поле {@code id} заполняется внутри реализации, {@code createdAt} — из хранилища.
     *
     * @param user пользователь с заполненными {@code username} и {@code passwordHash}
     * @return сохранённый пользователь с присвоенным id
     */
    StoredUser insert(StoredUser user);
}

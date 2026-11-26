package org.your.gossips.gossipsapp.adapter.out.persistence;

import org.your.gossips.gossipsapp.adapter.out.model.StoredContact;

import java.util.List;
import java.util.Optional;

/**
 * Порт хранения контактов (ростера) пользователя.
 */
public interface ContactPersistence {

    /**
     * Все контакты владельца, от новых к старым.
     */
    List<StoredContact> findAllByOwner(String ownerUsername);

    /**
     * Конкретный контакт владельца, если есть.
     */
    Optional<StoredContact> findByOwnerAndContact(String ownerUsername, String contactUsername);

    /**
     * Добавляет контакт. Если уже существует — возвращает существующую запись без изменений.
     */
    StoredContact add(String ownerUsername, String contactUsername);

    /**
     * Удаляет контакт. Если контакта не было — ничего не делает.
     */
    void remove(String ownerUsername, String contactUsername);
}

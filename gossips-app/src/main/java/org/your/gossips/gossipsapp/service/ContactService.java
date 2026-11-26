package org.your.gossips.gossipsapp.service;

import org.your.gossips.common.dto.Contact;

import java.util.List;

/**
 * Бизнес-логика контактов (ростера).
 */
public interface ContactService {

    /**
     * Все контакты пользователя, от новых к старым.
     */
    List<Contact> findAll(String username);

    /**
     * Добавляет контакт. Идемпотентно — существующий контакт возвращается без изменений.
     *
     * @throws IllegalArgumentException если contactUsername совпадает с username
     */
    Contact add(String username, String contactUsername);

    /**
     * Удаляет контакт. Идемпотентно — отсутствующий контакт не является ошибкой.
     */
    void remove(String username, String contactUsername);
}

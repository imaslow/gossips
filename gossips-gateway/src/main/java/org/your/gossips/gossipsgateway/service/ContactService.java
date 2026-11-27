package org.your.gossips.gossipsgateway.service;

import org.your.gossips.common.dto.Contact;
import org.your.gossips.common.dto.ContactAddRequest;

import java.util.List;

/**
 * Контакты: проксирование в app-сервис от имени аутентифицированного пользователя.
 */
public interface ContactService {

    List<Contact> findAll(String username);

    Contact add(String username, ContactAddRequest request);

    void remove(String username, String contactUsername);
}

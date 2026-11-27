package org.your.gossips.gossipsgateway.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.your.gossips.common.dto.Contact;
import org.your.gossips.common.dto.ContactAddRequest;
import org.your.gossips.gossipsgateway.adapter.out.AppServiceClient;
import org.your.gossips.gossipsgateway.service.ContactService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final AppServiceClient appClient;

    @Override
    public List<Contact> findAll(String username) {
        return appClient.getContacts(username);
    }

    @Override
    public Contact add(String username, ContactAddRequest request) {
        return appClient.addContact(username, request);
    }

    @Override
    public void remove(String username, String contactUsername) {
        appClient.removeContact(username, contactUsername);
    }
}

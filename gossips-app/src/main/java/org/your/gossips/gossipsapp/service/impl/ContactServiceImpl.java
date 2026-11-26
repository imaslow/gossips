package org.your.gossips.gossipsapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.your.gossips.common.dto.Contact;
import org.your.gossips.gossipsapp.adapter.out.persistence.ContactPersistence;
import org.your.gossips.gossipsapp.mapper.ContactMapper;
import org.your.gossips.gossipsapp.service.ContactService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactPersistence contactPersistence;
    private final ContactMapper contactMapper;

    @Override
    public List<Contact> findAll(String username) {
        return contactPersistence.findAllByOwner(username).stream()
                .map(contactMapper::toDto)
                .toList();
    }

    @Override
    public Contact add(String username, String contactUsername) {
        if (contactUsername.equals(username)) {
            throw new IllegalArgumentException("cannot add yourself to contacts");
        }
        return contactMapper.toDto(contactPersistence.add(username, contactUsername));
    }

    @Override
    public void remove(String username, String contactUsername) {
        contactPersistence.remove(username, contactUsername);
    }
}

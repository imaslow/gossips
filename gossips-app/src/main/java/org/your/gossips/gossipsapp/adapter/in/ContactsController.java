package org.your.gossips.gossipsapp.adapter.in;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.your.gossips.common.dto.Contact;
import org.your.gossips.common.dto.ContactAddRequest;
import org.your.gossips.gossipsapp.api.ContactsApi;
import org.your.gossips.gossipsapp.service.ContactService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ContactsController implements ContactsApi {

    private final ContactService contactService;

    @Override
    public ResponseEntity<List<Contact>> getContacts(String xUsername) {
        return ResponseEntity.ok(contactService.findAll(xUsername));
    }

    @Override
    public ResponseEntity<Contact> addContact(String xUsername, ContactAddRequest contactAddRequest) {
        return ResponseEntity.ok(contactService.add(xUsername, contactAddRequest.getUsername()));
    }

    @Override
    public ResponseEntity<Void> removeContact(String xUsername, String username) {
        contactService.remove(xUsername, username);
        return ResponseEntity.noContent().build();
    }
}

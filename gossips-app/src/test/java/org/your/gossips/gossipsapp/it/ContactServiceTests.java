package org.your.gossips.gossipsapp.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.your.gossips.common.dto.Contact;
import org.your.gossips.gossipsapp.service.ContactService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционные тесты контактов: идемпотентное добавление/удаление, запрет self-add.
 */
class ContactServiceTests extends PostgresIT {

    @Autowired
    private ContactService contactService;

    @Test
    void addIsIdempotent() {
        Contact first = contactService.add("contact-a", "contact-b");
        contactService.add("contact-a", "contact-b");

        assertThat(first.getUsername()).isEqualTo("contact-b");
        // без профиля displayName откатывается на username
        assertThat(first.getDisplayName()).isEqualTo("contact-b");
        assertThat(contactService.findAll("contact-a")).hasSize(1);
    }

    @Test
    void addYourselfIsRejected() {
        assertThatThrownBy(() -> contactService.add("contact-self", "contact-self"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeIsIdempotent() {
        contactService.add("contact-c", "contact-d");
        contactService.remove("contact-c", "contact-d");
        contactService.remove("contact-c", "contact-d");

        assertThat(contactService.findAll("contact-c")).isEmpty();
    }
}

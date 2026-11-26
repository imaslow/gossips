package org.your.gossips.gossipsapp.adapter.out.persistence.impl;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record4;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Repository;
import org.your.gossips.gossipsapp.adapter.out.model.StoredContact;
import org.your.gossips.gossipsapp.adapter.out.persistence.ContactPersistence;
import org.your.gossips.gossipsapp.jooq.tables.records.ContactsRecord;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.coalesce;
import static org.your.gossips.gossipsapp.jooq.tables.Contacts.CONTACTS;
import static org.your.gossips.gossipsapp.jooq.tables.Profiles.PROFILES;

@Repository
@RequiredArgsConstructor
public class ContactRepository implements ContactPersistence {

    private final DSLContext dsl;

    @Override
    public List<StoredContact> findAllByOwner(String ownerUsername) {
        return selectWithDisplayName()
                .where(CONTACTS.OWNER_USERNAME.eq(ownerUsername))
                .orderBy(CONTACTS.CREATED_AT.desc())
                .fetchInto(StoredContact.class);
    }

    @Override
    public Optional<StoredContact> findByOwnerAndContact(String ownerUsername, String contactUsername) {
        return selectWithDisplayName()
                .where(CONTACTS.OWNER_USERNAME.eq(ownerUsername))
                .and(CONTACTS.CONTACT_USERNAME.eq(contactUsername))
                .fetchOptionalInto(StoredContact.class);
    }

    @Override
    public StoredContact add(String ownerUsername, String contactUsername) {
        return findByOwnerAndContact(ownerUsername, contactUsername).orElseGet(() -> {
            dsl.insertInto(CONTACTS)
                    .set(CONTACTS.OWNER_USERNAME, ownerUsername)
                    .set(CONTACTS.CONTACT_USERNAME, contactUsername)
                    .execute();
            // Перечитываем с join — отдать актуальный displayName из профиля
            return findByOwnerAndContact(ownerUsername, contactUsername).orElseThrow();
        });
    }

    @Override
    public void remove(String ownerUsername, String contactUsername) {
        dsl.deleteFrom(CONTACTS)
                .where(CONTACTS.OWNER_USERNAME.eq(ownerUsername))
                .and(CONTACTS.CONTACT_USERNAME.eq(contactUsername))
                .execute();
    }

    /**
     * Выборка контактов с подстановкой отображаемого имени из profiles
     * (LEFT JOIN: профиль может отсутствовать — тогда вместо имени логин).
     */
    private SelectJoinStep<Record4<String, String, String, OffsetDateTime>> selectWithDisplayName() {
        return dsl.select(
                        CONTACTS.OWNER_USERNAME,
                        CONTACTS.CONTACT_USERNAME,
                        coalesce(PROFILES.DISPLAY_NAME, CONTACTS.CONTACT_USERNAME).as("displayName"),
                        CONTACTS.CREATED_AT)
                .from(CONTACTS)
                .leftJoin(PROFILES).on(PROFILES.USERNAME.eq(CONTACTS.CONTACT_USERNAME));
    }
}

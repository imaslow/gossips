package org.your.gossips.gossipssecurity.adapter.out.persistence.impl;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.your.gossips.gossipssecurity.adapter.out.model.StoredUser;
import org.your.gossips.gossipssecurity.adapter.out.persistence.UserPersistence;
import org.your.gossips.gossipssecurity.jooq.tables.records.UsersRecord;

import java.util.Optional;
import java.util.UUID;

import static org.your.gossips.gossipssecurity.jooq.tables.Users.USERS;

@Repository
@RequiredArgsConstructor
public class UserRepository implements UserPersistence {

    private final DSLContext dsl;

    @Override
    public Optional<StoredUser> findById(UUID id) {
        return dsl.selectFrom(USERS)
                .where(USERS.ID.eq(id))
                .fetchOptionalInto(StoredUser.class);
    }

    @Override
    public Optional<StoredUser> findByUsername(String username) {
        return dsl.selectFrom(USERS)
                .where(USERS.USERNAME.eq(username))
                .fetchOptionalInto(StoredUser.class);
    }

    @Override
    public StoredUser insert(StoredUser user) {
        UsersRecord record = dsl.newRecord(USERS, user);
        record.setId(UUID.randomUUID());
        return dsl.insertInto(USERS)
                .set(record)
                .returning()
                .fetchOneInto(StoredUser.class);
    }
}

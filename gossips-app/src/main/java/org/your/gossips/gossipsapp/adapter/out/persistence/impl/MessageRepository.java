package org.your.gossips.gossipsapp.adapter.out.persistence.impl;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.your.gossips.gossipsapp.adapter.out.persistence.MessagePersistence;
import org.your.gossips.gossipsapp.adapter.out.persistence.StoredMessage;
import org.your.gossips.gossipsapp.jooq.tables.records.MessagesRecord;

import java.util.List;
import java.util.UUID;

import static org.your.gossips.gossipsapp.jooq.tables.Messages.MESSAGES;

@Repository
@RequiredArgsConstructor
public class MessageRepository implements MessagePersistence {

    private final DSLContext dsl;

    @Override
    public StoredMessage insert(StoredMessage message) {
        MessagesRecord record = dsl.newRecord(MESSAGES, message);
        record.setId(UUID.randomUUID());
        return dsl.insertInto(MESSAGES)
                .set(record)
                .returning()
                .fetchOneInto(StoredMessage.class);
    }

    @Override
    public List<StoredMessage> findAll() {
        return dsl.selectFrom(MESSAGES)
                .orderBy(MESSAGES.CREATED_AT.asc())
                .fetchInto(StoredMessage.class);
    }
}

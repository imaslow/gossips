package org.your.gossips.gossipsapp.adapter.out.persistence.impl;

import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.your.gossips.gossipsapp.adapter.out.persistence.MessagePersistence;
import org.your.gossips.gossipsapp.adapter.out.model.StoredMessage;
import org.your.gossips.gossipsapp.adapter.out.page.MessagePage;
import org.your.gossips.gossipsapp.jooq.tables.records.MessagesRecord;

import java.time.OffsetDateTime;
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
    public MessagePage findPage(UUID conversationId, UUID beforeId, int limit) {
        // Курсор — позиция (created_at, id) сообщения: берём записи «старше» неё
        // (сортировка новые-первыми). Двойной ключ снимает коллизии created_at.
        Condition cursor = beforeId == null
                ? DSL.noCondition()
                : DSL.row(MESSAGES.CREATED_AT, MESSAGES.ID).lt(
                        dsl.select(MESSAGES.CREATED_AT, MESSAGES.ID)
                                .from(MESSAGES)
                                .where(MESSAGES.ID.eq(beforeId)));

        List<StoredMessage> rows = dsl.selectFrom(MESSAGES)
                .where(MESSAGES.CONVERSATION_ID.eq(conversationId))
                .and(cursor)
                .orderBy(MESSAGES.CREATED_AT.desc(), MESSAGES.ID.desc())
                .limit(limit + 1)  // +1 — детект «есть ли ещё»
                .fetchInto(StoredMessage.class);

        boolean hasMore = rows.size() > limit;
        List<StoredMessage> page = hasMore ? rows.subList(0, limit) : rows;
        UUID nextBeforeId = hasMore ? page.get(page.size() - 1).id() : null;
        return new MessagePage(page, nextBeforeId);
    }

    @Override
    public int markRead(UUID conversationId, String readerUsername) {
        return dsl.update(MESSAGES)
                .set(MESSAGES.READ_AT, OffsetDateTime.now())
                .where(MESSAGES.CONVERSATION_ID.eq(conversationId))
                .and(MESSAGES.USERNAME.ne(readerUsername))
                .and(MESSAGES.READ_AT.isNull())
                .execute();
    }
}

package org.your.gossips.gossipsapp.adapter.out.persistence.impl;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.your.gossips.gossipsapp.adapter.out.persistence.ConversationPersistence;
import org.your.gossips.gossipsapp.adapter.out.model.ConversationSummary;
import org.your.gossips.gossipsapp.adapter.out.model.StoredConversation;
import org.your.gossips.gossipsapp.jooq.tables.records.ConversationMembersRecord;
import org.your.gossips.gossipsapp.jooq.tables.records.ConversationsRecord;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.coalesce;
import static org.your.gossips.gossipsapp.jooq.tables.ConversationMembers.CONVERSATION_MEMBERS;
import static org.your.gossips.gossipsapp.jooq.tables.Conversations.CONVERSATIONS;
import static org.your.gossips.gossipsapp.jooq.tables.Messages.MESSAGES;
import static org.your.gossips.gossipsapp.jooq.tables.Profiles.PROFILES;

@Repository
@RequiredArgsConstructor
public class ConversationRepository implements ConversationPersistence {

    private final DSLContext dsl;

    @Override
    public Optional<StoredConversation> findByPair(String userA, String userB) {
        var m1 = CONVERSATION_MEMBERS.asTable("m1");
        var m2 = CONVERSATION_MEMBERS.asTable("m2");
        return dsl.select(CONVERSATIONS.ID, CONVERSATIONS.CREATED_AT)
                .from(CONVERSATIONS)
                .join(m1).on(m1.field(CONVERSATION_MEMBERS.CONVERSATION_ID).eq(CONVERSATIONS.ID))
                .join(m2).on(m2.field(CONVERSATION_MEMBERS.CONVERSATION_ID).eq(CONVERSATIONS.ID))
                .where(m1.field(CONVERSATION_MEMBERS.USERNAME).eq(userA))
                .and(m2.field(CONVERSATION_MEMBERS.USERNAME).eq(userB))
                .fetchOptionalInto(StoredConversation.class);
    }

    @Override
    @Transactional
    public StoredConversation createForPair(String userA, String userB) {
        ConversationsRecord conversation = dsl.insertInto(CONVERSATIONS)
                .set(CONVERSATIONS.ID, UUID.randomUUID())
                .returning()
                .fetchOne();

        for (String username : new String[]{userA, userB}) {
            ConversationMembersRecord member = dsl.newRecord(CONVERSATION_MEMBERS);
            member.setConversationId(conversation.getId());
            member.setUsername(username);
            member.insert();
        }
        return new StoredConversation(conversation.getId(), conversation.getCreatedAt());
    }

    @Override
    public List<ConversationSummary> findAllSummariesFor(String username) {
        var peer = CONVERSATION_MEMBERS.asTable("peer");

        Field<OffsetDateTime> lastMessageAt = dsl.select(MESSAGES.CREATED_AT)
                .from(MESSAGES)
                .where(MESSAGES.CONVERSATION_ID.eq(CONVERSATIONS.ID))
                .orderBy(MESSAGES.CREATED_AT.desc(), MESSAGES.ID.desc())
                .limit(1)
                .asField("lastMessageAt");
        Field<String> lastMessage = dsl.select(MESSAGES.CONTENT)
                .from(MESSAGES)
                .where(MESSAGES.CONVERSATION_ID.eq(CONVERSATIONS.ID))
                .orderBy(MESSAGES.CREATED_AT.desc(), MESSAGES.ID.desc())
                .limit(1)
                .asField("lastMessage");
        Field<Integer> unreadCount = dsl.selectCount()
                .from(MESSAGES)
                .where(MESSAGES.CONVERSATION_ID.eq(CONVERSATIONS.ID))
                .and(MESSAGES.USERNAME.ne(username))
                .and(MESSAGES.READ_AT.isNull())
                .asField("unreadCount");

        return dsl.select(
                        CONVERSATIONS.ID,
                        peer.field(CONVERSATION_MEMBERS.USERNAME).as("peer"),
                        coalesce(PROFILES.DISPLAY_NAME, peer.field(CONVERSATION_MEMBERS.USERNAME)).as("displayName"),
                        CONVERSATIONS.CREATED_AT,
                        lastMessage,
                        lastMessageAt,
                        unreadCount)
                .from(CONVERSATIONS)
                .join(CONVERSATION_MEMBERS)
                .on(CONVERSATION_MEMBERS.CONVERSATION_ID.eq(CONVERSATIONS.ID))
                .and(CONVERSATION_MEMBERS.USERNAME.eq(username))
                .join(peer).on(peer.field(CONVERSATION_MEMBERS.CONVERSATION_ID).eq(CONVERSATIONS.ID)
                        .and(peer.field(CONVERSATION_MEMBERS.USERNAME).ne(username)))
                .leftJoin(PROFILES).on(PROFILES.USERNAME.eq(peer.field(CONVERSATION_MEMBERS.USERNAME)))
                .orderBy(lastMessageAt.desc().nullsLast(), CONVERSATIONS.CREATED_AT.desc())
                .fetchInto(ConversationSummary.class);
    }

    @Override
    public boolean isMember(UUID conversationId, String username) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(CONVERSATION_MEMBERS)
                        .where(CONVERSATION_MEMBERS.CONVERSATION_ID.eq(conversationId))
                        .and(CONVERSATION_MEMBERS.USERNAME.eq(username)));
    }

    @Override
    public List<String> findMemberUsernames(UUID conversationId) {
        return dsl.select(CONVERSATION_MEMBERS.USERNAME)
                .from(CONVERSATION_MEMBERS)
                .where(CONVERSATION_MEMBERS.CONVERSATION_ID.eq(conversationId))
                .fetchInto(String.class);
    }
}

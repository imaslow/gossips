package org.your.gossips.gossipsapp.adapter.out.persistence.impl;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.your.gossips.gossipsapp.adapter.out.persistence.ConversationPersistence;
import org.your.gossips.gossipsapp.adapter.out.model.ConversationMember;
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

    private static final String TYPE_PAIR = "pair";
    private static final String TYPE_GROUP = "group";

    private final DSLContext dsl;

    @Override
    public Optional<StoredConversation> findByPair(String userA, String userB) {
        var m1 = CONVERSATION_MEMBERS.asTable("m1");
        var m2 = CONVERSATION_MEMBERS.asTable("m2");
        return dsl.select(CONVERSATIONS.ID, CONVERSATIONS.CREATED_AT,
                        CONVERSATIONS.TYPE, CONVERSATIONS.TITLE, CONVERSATIONS.CREATED_BY)
                .from(CONVERSATIONS)
                .join(m1).on(m1.field(CONVERSATION_MEMBERS.CONVERSATION_ID).eq(CONVERSATIONS.ID))
                .join(m2).on(m2.field(CONVERSATION_MEMBERS.CONVERSATION_ID).eq(CONVERSATIONS.ID))
                .where(m1.field(CONVERSATION_MEMBERS.USERNAME).eq(userA))
                .and(m2.field(CONVERSATION_MEMBERS.USERNAME).eq(userB))
                .and(CONVERSATIONS.TYPE.eq(TYPE_PAIR))
                .fetchOptionalInto(StoredConversation.class);
    }

    @Override
    @Transactional
    public StoredConversation createForPair(String userA, String userB) {
        ConversationsRecord conversation = dsl.insertInto(CONVERSATIONS)
                .set(CONVERSATIONS.ID, UUID.randomUUID())
                .set(CONVERSATIONS.TYPE, TYPE_PAIR)
                .set(CONVERSATIONS.CREATED_BY, userA)
                .returning()
                .fetchOne();

        for (String username : new String[]{userA, userB}) {
            ConversationMembersRecord member = dsl.newRecord(CONVERSATION_MEMBERS);
            member.setConversationId(conversation.getId());
            member.setUsername(username);
            member.insert();
        }
        return new StoredConversation(conversation.getId(), conversation.getCreatedAt(),
                conversation.getType(), conversation.getTitle(), conversation.getCreatedBy());
    }

    @Override
    @Transactional
    public StoredConversation createGroup(String creator, List<String> members, String title) {
        ConversationsRecord conversation = dsl.insertInto(CONVERSATIONS)
                .set(CONVERSATIONS.ID, UUID.randomUUID())
                .set(CONVERSATIONS.TYPE, TYPE_GROUP)
                .set(CONVERSATIONS.TITLE, title)
                .set(CONVERSATIONS.CREATED_BY, creator)
                .returning()
                .fetchOne();

        for (String username : members) {
            ConversationMembersRecord member = dsl.newRecord(CONVERSATION_MEMBERS);
            member.setConversationId(conversation.getId());
            member.setUsername(username);
            member.insert();
        }
        return new StoredConversation(conversation.getId(), conversation.getCreatedAt(),
                conversation.getType(), conversation.getTitle(), conversation.getCreatedBy());
    }

    @Override
    public List<ConversationSummary> findAllSummariesFor(String username) {
        // Собеседник берём только для pair-диалогов: у групп N-1 строк в members,
        // scalar-подзапрос без фильтра по типу вернул бы мусор.
        // displayName — отдельный самодостаточный подзапрос: Postgres не разрешает
        // ссылаться на псевдоним колонки внутри выражений SELECT (только ORDER BY).
        Field<String> peerUsername = dsl.select(CONVERSATION_MEMBERS.USERNAME)
                .from(CONVERSATION_MEMBERS)
                .where(CONVERSATION_MEMBERS.CONVERSATION_ID.eq(CONVERSATIONS.ID))
                .and(CONVERSATION_MEMBERS.USERNAME.ne(username))
                .and(CONVERSATIONS.TYPE.eq(TYPE_PAIR))
                .limit(1)
                .asField("peer");
        Field<String> peerDisplayName = dsl.select(coalesce(PROFILES.DISPLAY_NAME, CONVERSATION_MEMBERS.USERNAME))
                .from(CONVERSATION_MEMBERS)
                .leftJoin(PROFILES).on(PROFILES.USERNAME.eq(CONVERSATION_MEMBERS.USERNAME))
                .where(CONVERSATION_MEMBERS.CONVERSATION_ID.eq(CONVERSATIONS.ID))
                .and(CONVERSATION_MEMBERS.USERNAME.ne(username))
                .and(CONVERSATIONS.TYPE.eq(TYPE_PAIR))
                .limit(1)
                .asField("displayName");

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
        Field<Boolean> lastMessageEncrypted = dsl.select(MESSAGES.ENCRYPTED)
                .from(MESSAGES)
                .where(MESSAGES.CONVERSATION_ID.eq(CONVERSATIONS.ID))
                .orderBy(MESSAGES.CREATED_AT.desc(), MESSAGES.ID.desc())
                .limit(1)
                .asField("lastMessageEncrypted");
        Field<Integer> unreadCount = dsl.selectCount()
                .from(MESSAGES)
                .where(MESSAGES.CONVERSATION_ID.eq(CONVERSATIONS.ID))
                .and(MESSAGES.USERNAME.ne(username))
                .and(MESSAGES.READ_AT.isNull())
                .asField("unreadCount");

        return dsl.select(
                        CONVERSATIONS.ID,
                        peerUsername,
                        peerDisplayName,
                        CONVERSATIONS.TYPE.as("type"),
                        CONVERSATIONS.TITLE.as("title"),
                        CONVERSATIONS.CREATED_AT,
                        lastMessage,
                        lastMessageAt,
                        lastMessageEncrypted,
                        unreadCount)
                .from(CONVERSATIONS)
                .join(CONVERSATION_MEMBERS)
                .on(CONVERSATION_MEMBERS.CONVERSATION_ID.eq(CONVERSATIONS.ID))
                .and(CONVERSATION_MEMBERS.USERNAME.eq(username))
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

    @Override
    public List<ConversationMember> findMemberSummaries(UUID conversationId) {
        return dsl.select(CONVERSATION_MEMBERS.USERNAME,
                        coalesce(PROFILES.DISPLAY_NAME, CONVERSATION_MEMBERS.USERNAME).as("displayName"))
                .from(CONVERSATION_MEMBERS)
                .leftJoin(PROFILES).on(PROFILES.USERNAME.eq(CONVERSATION_MEMBERS.USERNAME))
                .where(CONVERSATION_MEMBERS.CONVERSATION_ID.eq(conversationId))
                .orderBy(CONVERSATION_MEMBERS.JOINED_AT.asc())
                .fetchInto(ConversationMember.class);
    }
}

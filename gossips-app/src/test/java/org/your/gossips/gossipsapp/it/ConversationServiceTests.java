package org.your.gossips.gossipsapp.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.your.gossips.common.dto.Conversation;
import org.your.gossips.common.dto.ConversationMember;
import org.your.gossips.common.dto.Message;
import org.your.gossips.common.dto.MessagesPage;
import org.your.gossips.gossipsapp.exception.AccessDeniedException;
import org.your.gossips.gossipsapp.service.ConversationService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционные тесты диалогов 1-на-1: дедуп создания, права доступа,
 * unread-счётчики, пагинация истории, флаг encrypted.
 * Классы делят одну БД, поэтому у каждого теста свои уникальные юзернеймы.
 */
class ConversationServiceTests extends PostgresIT {

    @Autowired
    private ConversationService conversationService;

    @Test
    void createIsIdempotentForPair() {
        Conversation first = conversationService.create("dedup-a", "dedup-b");
        Conversation again = conversationService.create("dedup-b", "dedup-a");

        assertThat(again.getId()).isEqualTo(first.getId());
        // peer всегда «другой участник» относительно вызывающего
        assertThat(first.getPeer()).isEqualTo("dedup-b");
        assertThat(again.getPeer()).isEqualTo("dedup-a");
    }

    @Test
    void createWithSelfIsRejected() {
        assertThatThrownBy(() -> conversationService.create("self-user", "self-user"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonMemberCannotReadOrWrite() {
        Conversation conversation = conversationService.create("member-a", "member-b");

        assertThatThrownBy(() -> conversationService.getMessages("outsider", conversation.getId(), null, 10))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> conversationService.sendMessage("outsider", conversation.getId(), "hi", false))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> conversationService.markRead("outsider", conversation.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void sendMessageKeepsEncryptedFlag() {
        Conversation conversation = conversationService.create("enc-a", "enc-b");

        Message plain = conversationService.sendMessage("enc-a", conversation.getId(), "hello", false);
        Message encrypted = conversationService.sendMessage("enc-b", conversation.getId(), "ciphertext", true);

        assertThat(plain.getEncrypted()).isFalse();
        assertThat(encrypted.getEncrypted()).isTrue();
    }

    @Test
    void unreadCountTracksPeerMessagesAndMarkReadClearsThem() {
        Conversation conversation = conversationService.create("unread-a", "unread-b");
        conversationService.sendMessage("unread-a", conversation.getId(), "one", false);
        conversationService.sendMessage("unread-a", conversation.getId(), "two", false);

        Conversation forBob = onlyConversation(conversationService.findAll("unread-b"));
        assertThat(forBob.getUnreadCount()).isEqualTo(2);
        assertThat(forBob.getLastMessage()).isEqualTo("two");

        // свои сообщения в чужом unread не попадают
        Conversation forAlice = onlyConversation(conversationService.findAll("unread-a"));
        assertThat(forAlice.getUnreadCount()).isZero();

        conversationService.markRead("unread-b", conversation.getId());
        assertThat(onlyConversation(conversationService.findAll("unread-b")).getUnreadCount()).isZero();
    }

    @Test
    void historyIsNewestFirstWithCursor() {
        Conversation conversation = conversationService.create("hist-a", "hist-b");
        for (int i = 1; i <= 5; i++) {
            conversationService.sendMessage("hist-a", conversation.getId(), "m" + i, false);
        }

        MessagesPage firstPage = conversationService.getMessages("hist-b", conversation.getId(), null, 3);
        assertThat(firstPage.getMessages()).hasSize(3);
        assertThat(firstPage.getMessages().getFirst().getContent()).isEqualTo("m5");
        assertThat(firstPage.getNextBeforeId()).isNotNull();

        UUID beforeId = UUID.fromString(firstPage.getNextBeforeId());
        MessagesPage secondPage = conversationService.getMessages("hist-b", conversation.getId(), beforeId, 3);
        assertThat(secondPage.getMessages()).hasSize(2);
        assertThat(secondPage.getMessages().getFirst().getContent()).isEqualTo("m2");
        assertThat(secondPage.getNextBeforeId()).isNull();
    }

    @Test
    void conversationsAreSortedByLastActivity() {
        Conversation quiet = conversationService.create("sort-a", "sort-b");
        conversationService.create("sort-a", "sort-c");
        conversationService.sendMessage("sort-a", quiet.getId(), "wake up", false);

        List<Conversation> conversations = conversationService.findAll("sort-a");
        assertThat(conversations).hasSize(2);
        assertThat(conversations.getFirst().getId()).isEqualTo(quiet.getId());
    }

    @Test
    void groupCreationAddsCreatorAndReturnsMembersWithTitle() {
        Conversation group = conversationService.createGroup(
                "grp-creator", List.of("grp-one", "grp-two"), "Test group");

        assertThat(group.getType()).isEqualTo(Conversation.TypeEnum.GROUP);
        assertThat(group.getTitle()).isEqualTo("Test group");
        assertThat(group.getPeer()).isNull();

        List<ConversationMember> members = conversationService.getMembers("grp-creator", group.getId());
        // порядок не контракт: joined_at в одной транзакции может совпасть
        assertThat(members).extracting(ConversationMember::getUsername)
                .containsExactlyInAnyOrder("grp-creator", "grp-one", "grp-two");

        // участники видят группу в списке с названием и без peer
        Conversation forMember = conversationService.findAll("grp-one").getFirst();
        assertThat(forMember.getType()).isEqualTo(Conversation.TypeEnum.GROUP);
        assertThat(forMember.getTitle()).isEqualTo("Test group");
        assertThat(forMember.getPeer()).isNull();
    }

    @Test
    void groupRequiresAtLeastTwoOthersAndNoCreatorInList() {
        assertThatThrownBy(() -> conversationService.createGroup("grp-solo", List.of("grp-x"), "Small"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> conversationService.createGroup(
                "grp-self", List.of("grp-self", "grp-y"), "Self inside"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void groupMessagesReachAllMembersAndUnreadCountsPerMember() {
        Conversation group = conversationService.createGroup(
                "grp-msg-a", List.of("grp-msg-b", "grp-msg-c"), "Chatters");

        conversationService.sendMessage("grp-msg-a", group.getId(), "hello all", false);
        conversationService.sendMessage("grp-msg-b", group.getId(), "hi", false);

        assertThat(onlyConversation(conversationService.findAll("grp-msg-c")).getUnreadCount()).isEqualTo(2);
        // авторы своих сообщений не видят в непрочитанных
        assertThat(onlyConversation(conversationService.findAll("grp-msg-a")).getUnreadCount()).isEqualTo(1);

        conversationService.markRead("grp-msg-c", group.getId());
        assertThat(onlyConversation(conversationService.findAll("grp-msg-c")).getUnreadCount()).isZero();
    }

    @Test
    void nonMemberCannotListGroupMembers() {
        Conversation group = conversationService.createGroup(
                "grp-priv-a", List.of("grp-priv-b", "grp-priv-c"), "Private");

        assertThatThrownBy(() -> conversationService.getMembers("grp-outsider", group.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void pairAndGroupCoexistWithoutHijack() {
        // оба участника пары входят в одну группу — pair-поиск не должен вернуть группу
        Conversation pair = conversationService.create("mix-a", "mix-b");
        Conversation group = conversationService.createGroup("mix-a", List.of("mix-b", "mix-c"), "Mixed");

        Conversation again = conversationService.create("mix-b", "mix-a");
        assertThat(again.getId()).isEqualTo(pair.getId());
        assertThat(again.getId()).isNotEqualTo(group.getId());
        assertThat(again.getType()).isEqualTo(Conversation.TypeEnum.PAIR);
    }

    private Conversation onlyConversation(List<Conversation> conversations) {
        assertThat(conversations).hasSize(1);
        return conversations.getFirst();
    }
}

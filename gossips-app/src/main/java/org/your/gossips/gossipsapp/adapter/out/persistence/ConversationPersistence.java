package org.your.gossips.gossipsapp.adapter.out.persistence;

import org.your.gossips.gossipsapp.adapter.out.model.ConversationMember;
import org.your.gossips.gossipsapp.adapter.out.model.ConversationSummary;
import org.your.gossips.gossipsapp.adapter.out.model.StoredConversation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Порт для доступа к хранилищу диалогов.
 */
public interface ConversationPersistence {

    /**
     * Ищет существующий pair-диалог между двумя пользователями (в любом порядке).
     *
     * @param userA один участник
     * @param userB другой участник
     * @return диалог или {@link Optional#empty()}
     */
    Optional<StoredConversation> findByPair(String userA, String userB);

    /**
     * Создаёт pair-диалог с двумя участниками (атомарно).
     *
     * @param userA один участник
     * @param userB другой участник
     * @return созданный диалог
     */
    StoredConversation createForPair(String userA, String userB);

    /**
     * Создаёт групповую беседу. Первым участником становится создатель.
     *
     * @param creator автор беседы (обязан входить в members)
     * @param members все участники (включая создателя), от 3 человек
     * @param title   название группы
     * @return созданный диалог
     */
    StoredConversation createGroup(String creator, List<String> members, String title);

    /**
     * Все диалоги пользователя, новые первыми. Для pair — имя второго участника,
     * для group — null в peer/displayName и название в title.
     */
    List<ConversationSummary> findAllSummariesFor(String username);

    /**
     * Проверяет членство в диалоге.
     */
    boolean isMember(UUID conversationId, String username);

    /**
     * Все участники диалога (только логины).
     */
    List<String> findMemberUsernames(UUID conversationId);

    /**
     * Все участники диалога с отображаемыми именами.
     */
    List<ConversationMember> findMemberSummaries(UUID conversationId);
}

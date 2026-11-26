package org.your.gossips.gossipsapp.adapter.out.persistence;

import org.your.gossips.gossipsapp.adapter.out.model.ConversationSummary;
import org.your.gossips.gossipsapp.adapter.out.model.StoredConversation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Порт для доступа к хранилищу диалогов 1-на-1.
 */
public interface ConversationPersistence {

    /**
     * Ищет существующий диалог между двумя пользователями (в любом порядке).
     *
     * @param userA один участник
     * @param userB другой участник
     * @return диалог или {@link Optional#empty()}
     */
    Optional<StoredConversation> findByPair(String userA, String userB);

    /**
     * Создаёт диалог с двумя участниками (атомарно).
     *
     * @param userA один участник
     * @param userB другой участник
     * @return созданный диалог
     */
    StoredConversation createForPair(String userA, String userB);

    /**
     * Все диалоги пользователя, новые первыми, с именем второго участника.
     */
    List<ConversationSummary> findAllSummariesFor(String username);

    /**
     * Проверяет членство в диалоге.
     */
    boolean isMember(UUID conversationId, String username);

    /**
     * Все участники диалога.
     */
    List<String> findMemberUsernames(UUID conversationId);
}

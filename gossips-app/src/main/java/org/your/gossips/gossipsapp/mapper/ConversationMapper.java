package org.your.gossips.gossipsapp.mapper;

import org.mapstruct.Mapper;
import org.your.gossips.common.dto.Conversation;
import org.your.gossips.gossipsapp.adapter.out.model.ConversationSummary;

/**
 * Маппинг сводки диалога в API-DTO (id: UUID -> String).
 */
@Mapper(componentModel = "spring")
public interface ConversationMapper {

    Conversation toDto(ConversationSummary summary);
}

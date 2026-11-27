package org.your.gossips.gossipsapp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.your.gossips.common.dto.Conversation;
import org.your.gossips.common.dto.ConversationMember;
import org.your.gossips.gossipsapp.adapter.out.model.ConversationSummary;

/**
 * Маппинг сводки диалога и участников в API-DTO (id: UUID -> String).
 * Имена рекорда персистенса и DTO совпадают — поэтому параметр квалифицирован.
 */
@Mapper(componentModel = "spring")
public interface ConversationMapper {

    // В DTO-енуме константы в верхнем регистре (PAIR("pair")) — valueOf("pair") не сработал бы
    @Mapping(target = "type", source = "type", qualifiedByName = "conversationType")
    Conversation toDto(ConversationSummary summary);

    @Named("conversationType")
    default Conversation.TypeEnum mapType(String type) {
        return Conversation.TypeEnum.fromValue(type);
    }

    ConversationMember toDto(org.your.gossips.gossipsapp.adapter.out.model.ConversationMember member);
}

package org.your.gossips.gossipsapp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.your.gossips.common.dto.Message;
import org.your.gossips.gossipsapp.adapter.out.model.StoredMessage;

/**
 * Маппинг хранимой записи сообщения в API-DTO.
 * Различаются имя времени создания (created_at -> timestamp) и тип id (UUID -> String).
 */
@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(target = "timestamp", source = "createdAt")
    @Mapping(target = "read", expression = "java(storedMessage.readAt() != null)")
    Message toDto(StoredMessage storedMessage);
    // encrypted маппится по имени автоматически
}

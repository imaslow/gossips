package org.your.gossips.gossipsapp.adapter.in;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.your.gossips.common.dto.Message;
import org.your.gossips.common.dto.MessageRequest;
import org.your.gossips.gossipsapp.adapter.out.persistence.MessagePersistence;
import org.your.gossips.gossipsapp.adapter.out.persistence.StoredMessage;
import org.your.gossips.gossipsapp.api.MessagesApi;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MessageController implements MessagesApi {

    private final MessagePersistence messagePersistence;

    @Override
    public ResponseEntity<List<Message>> getMessages() {
        return ResponseEntity.ok(messagePersistence.findAll().stream().map(this::toDto).toList());
    }

    @Override
    public ResponseEntity<Message> sendMessage(MessageRequest messageRequest) {
        StoredMessage stored = messagePersistence.insert(
                new StoredMessage(null, messageRequest.getUsername(), messageRequest.getContent(), null));
        return ResponseEntity.ok(toDto(stored));
    }

    /**
     * Сборка API-DTO из хранимой записи: различаются имя времени создания
     * (created_at -> timestamp) и тип идентификатора (UUID -> String).
     */
    private Message toDto(StoredMessage stored) {
        Message message = new Message();
        message.setId(stored.id().toString());
        message.setUsername(stored.username());
        message.setContent(stored.content());
        message.setTimestamp(stored.createdAt());
        return message;
    }
}

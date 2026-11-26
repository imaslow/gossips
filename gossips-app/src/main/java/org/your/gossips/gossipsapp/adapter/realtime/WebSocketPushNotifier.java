package org.your.gossips.gossipsapp.adapter.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.your.gossips.common.dto.Message;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Реализация realtime-доставки через WebSocket.
 * Envelope собирается вручную: в модуле нет jackson-jsr310,
 * OffsetDateTime сериализуем строкой (ISO-8601, тот же формат, что у API).
 */
@Component
@RequiredArgsConstructor
public class WebSocketPushNotifier implements PushNotifier {

    private final WebSocketSessionRegistry registry;

    @Override
    public void pushMessage(String username, UUID conversationId, Message message) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", "NEW_MESSAGE");
        envelope.put("conversationId", conversationId.toString());
        envelope.put("message", toPayload(message));
        registry.sendToUser(username, envelope);
    }

    private Map<String, Object> toPayload(Message message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", message.getId());
        payload.put("username", message.getUsername());
        payload.put("content", message.getContent());
        payload.put("timestamp", message.getTimestamp() != null ? message.getTimestamp().toString() : null);
        payload.put("read", message.getRead());
        payload.put("encrypted", message.getEncrypted());
        return payload;
    }
}

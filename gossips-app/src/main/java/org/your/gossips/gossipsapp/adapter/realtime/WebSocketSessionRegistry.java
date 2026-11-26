package org.your.gossips.gossipsapp.adapter.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Реестр активных WebSocket-сессий: username -> его сессии (устройства).
 * Сессии регистрируются/снимаются WebSocket-хендлером после валидации токена.
 */
@Slf4j
@Component
public class WebSocketSessionRegistry {

    // Свой маппер: в модуле нет web-starter'а, бина ObjectMapper нет.
    // Сериализуем простые Map-конверты — стандартного маппера достаточно.
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    public void register(String username, WebSocketSession session) {
        sessionsByUser.computeIfAbsent(username, key -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void unregister(String username, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByUser.get(username);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionsByUser.remove(username, sessions);
            }
        }
    }

    /**
     * Отправляет payload всем активным сессиям пользователя.
     * Мёртвые сессии (обрыв сети) молча вычищаются.
     */
    public void sendToUser(String username, Object payload) {
        Set<WebSocketSession> sessions = sessionsByUser.get(username);
        if (sessions == null) {
            return;
        }
        String json = toJson(payload);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                unregister(username, session);
                continue;
            }
            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.warn("Не удалось доставить сообщение пользователю {}: {}", username, e.getMessage());
            }
        }
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сериализовать WS-сообщение", e);
        }
    }
}

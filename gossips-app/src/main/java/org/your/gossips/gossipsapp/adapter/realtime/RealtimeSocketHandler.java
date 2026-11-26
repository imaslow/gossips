package org.your.gossips.gossipsapp.adapter.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import org.your.gossips.common.security.JwtService;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * WebSocket-эндпоинт /ws. Клиент подключается с токеном:
 * ws://host:8082/ws?token=eyJ...  (с префиксом "Bearer " или без).
 * Токен валидируется тем же JwtService, что и в gateway; невалидный — закрытие 4401.
 * Клиент ничего не шлёт — канал только на доставку (server -> client).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeSocketHandler extends TextWebSocketHandler {

    private static final CloseStatus UNAUTHORIZED = new CloseStatus(4401, "invalid or expired token");
    private static final String USERNAME_ATTRIBUTE = "username";

    private final JwtService jwtService;
    private final WebSocketSessionRegistry registry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session.getUri());
        if (token == null || !jwtService.isTokenValid(token)) {
            session.close(UNAUTHORIZED);
            return;
        }
        String username = jwtService.extractUsername(token);
        session.getAttributes().put(USERNAME_ATTRIBUTE, username);
        registry.register(username, session);
        log.debug("WS: {} подключился (сессия {})", username, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        unregister(session);
    }

    private void unregister(WebSocketSession session) {
        String username = (String) session.getAttributes().get(USERNAME_ATTRIBUTE);
        if (username != null) {
            registry.unregister(username, session);
        }
    }

    private String extractToken(URI uri) {
        if (uri == null) {
            return null;
        }
        Map<String, List<String>> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        List<String> tokens = params.get("token");
        return tokens == null || tokens.isEmpty() ? null : tokens.get(0);
    }
}

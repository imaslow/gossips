package org.your.gossips.gossipsapp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.your.gossips.common.ApiPaths;
import org.your.gossips.gossipsapp.adapter.realtime.RealtimeSocketHandler;

/**
 * Регистрация WebSocket-эндпоинта realtime-доставки сообщений.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final RealtimeSocketHandler realtimeSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeSocketHandler, ApiPaths.Realtime.WS).setAllowedOriginPatterns("*");
    }
}

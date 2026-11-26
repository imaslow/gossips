package org.your.gossips.gossipsapp.adapter.realtime;

import org.your.gossips.common.dto.Message;

import java.util.UUID;

/**
 * Порт realtime-доставки: уведомляет пользователей о новых сообщениях.
 */
public interface PushNotifier {

    /**
     * Доставляет новое сообщение всем активным сессиям пользователя.
     */
    void pushMessage(String username, UUID conversationId, Message message);
}

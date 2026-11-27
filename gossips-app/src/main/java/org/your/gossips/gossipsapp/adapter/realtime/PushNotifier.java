package org.your.gossips.gossipsapp.adapter.realtime;

import org.your.gossips.common.dto.Message;

import java.util.UUID;

/**
 * Порт realtime-доставки: уведомляет пользователей об изменениях в переписке.
 */
public interface PushNotifier {

    /**
     * Доставляет новое сообщение всем активным сессиям пользователя.
     */
    void pushMessage(String username, UUID conversationId, Message message);

    /**
     * Сообщает, что у пользователя появился новый (или изменённый) диалог —
     * клиенту стоит перечитать сводки. Отправляется без тела сообщения.
     */
    void pushConversationUpdated(String username, UUID conversationId);

    /**
     * Эфемерный сигнал «печатает»: ничего не хранится, доставляется как TYPING.
     *
     * @param toUsername   кому доставить (получатель)
     * @param typingUsername кто печатает — уходит в тело события
     */
    void pushTyping(String toUsername, UUID conversationId, String typingUsername);

    /**
     * Сообщает участникам, что пользователь прочитал диалог: доставляется как
     * MESSAGES_READ с именем читателя — клиенты обновляют статусы своих сообщений.
     *
     * @param toUsername  кому доставить (получатель)
     * @param readUsername кто прочитал — уходит в тело события
     */
    void pushRead(String toUsername, UUID conversationId, String readUsername);
}

package org.your.gossips.gossipsapp.adapter.out.page;

import org.your.gossips.gossipsapp.adapter.out.model.StoredMessage;

import java.util.List;
import java.util.UUID;

/**
 * Страница сообщений: список (новые первыми) + курсор для следующей страницы.
 *
 * @param messages      сообщения страницы, новые первыми
 * @param nextBeforeId  id последнего сообщения страницы — курсор «глубже в историю»;
 *                      {@code null}, если более старых сообщений нет
 */
public record MessagePage(List<StoredMessage> messages, UUID nextBeforeId) {
}

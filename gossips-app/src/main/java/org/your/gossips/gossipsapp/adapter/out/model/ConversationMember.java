package org.your.gossips.gossipsapp.adapter.out.model;

/**
 * Участник диалога с отображаемым именем (fallback — username).
 */
public record ConversationMember(String username, String displayName) {
}

/**
 * Зеркала DTO из openapi-спек (org.your.gossips.common.dto).
 * Поля и ограничения совпадают со спеками security-api.yml / app-api.yml / gateway-api.yml.
 */

export interface UserCredentials {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  username: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface Conversation {
  id: string;
  /** "pair" | "group" — kind диалога (зеркало enum из спеки). */
  type?: string;
  /** Название группы (null для pair). */
  title?: string | null;
  /** Другой участник; null для групп. */
  peer: string | null;
  displayName: string | null;
  createdAt: string;
  lastMessage: string | null;
  lastMessageAt: string | null;
  /** true — lastMessage это шифротекст, показываем заглушку, а не base64. */
  lastMessageEncrypted: boolean;
  unreadCount: number;
}

export interface ConversationCreateRequest {
  username: string;
}

export interface ConversationGroupCreateRequest {
  /** Участники без создателя (создатель добавляется сервером). */
  usernames: string[];
  title: string;
}

export interface ConversationMember {
  username: string;
  displayName: string;
}

export interface Message {
  id: string;
  username: string;
  content: string;
  timestamp: string;
  read: boolean;
  encrypted: boolean;
}

export interface MessageRequest {
  username?: string;
  content: string;
  /** true — content это base64(JSON) E2E-шифротекст. */
  encrypted?: boolean;
}

export interface PublicKeyInfo {
  username: string;
  publicKey: string;
  fingerprint: string;
  createdAt: string;
  updatedAt: string;
  /** Верифицировал ли caller именно этот отпечаток (актуально только для getFor). */
  verified: boolean;
}

export interface PublicKeyUpsertRequest {
  publicKey: string;
  fingerprint: string;
}

export interface KeyVerifyRequest {
  fingerprint: string;
}

export interface VerificationInfo {
  username: string;
  fingerprint: string;
  verifiedAt: string;
}

export interface MessagesPage {
  messages: Message[];
  nextBeforeId: string | null;
}

export interface Contact {
  username: string;
  displayName: string;
  createdAt: string;
}

export interface ContactAddRequest {
  username: string;
}

export interface Profile {
  username: string;
  displayName: string;
  bio?: string;
}

export interface ProfileUpdateRequest {
  displayName: string;
  bio?: string;
}

/** Тело ошибки API: {"code","message"} из ErrorResponse (common). */
export interface ErrorResponse {
  code: string;
  message: string;
}

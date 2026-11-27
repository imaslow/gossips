import type { Message } from "./types";
import { getValidAccessToken } from "./session";

/**
 * Realtime-доставка: сервер шлёт события, клиент только слушает.
 * Канал server -> client, токен в query (ws://host/ws?token=eyJ...).
 * Разрыв соединения молча лечим повторными подключениями с нарастающей паузой;
 * reconnect повторно проходит через getValidAccessToken — сессия подтягивается сама.
 */

export interface NewMessageEvent {
  type: "NEW_MESSAGE";
  conversationId: string;
  message: Message;
}

/** У пользователя появился новый/изменённый диалог — клиенту пора перечитать сводки. */
export interface ConversationUpdatedEvent {
  type: "CONVERSATION_UPDATED";
  conversationId: string;
}

/** Эфемерный сигнал «печатает» (имя — того, кто печатает). */
export interface TypingEvent {
  type: "TYPING";
  conversationId: string;
  username: string;
}

/** Собеседник прочитал диалог (имя — читателя): свои сообщения теперь «прочитано». */
export interface MessagesReadEvent {
  type: "MESSAGES_READ";
  conversationId: string;
  username: string;
}

export type RealtimeEvent = NewMessageEvent | ConversationUpdatedEvent | TypingEvent | MessagesReadEvent;

export interface RealtimeCallbacks {
  onEvent: (event: RealtimeEvent) => void;
  /** Соединение (пере)установлено — стоит синхронизировать состояние с сервером. */
  onOpen?: () => void;
  /** Токен отклонён сервером или сессию продлить нельзя — показываем предупреждение. */
  onAuthFailed?: () => void;
}

/** Код закрытия сервиса app: токен невалиден/просрочен. */
const UNAUTHORIZED_CLOSE_CODE = 4401;

/** Паузы между попытками, мс (нарастающие, дальше — по верхней границе). */
const RETRY_DELAYS = [1000, 2000, 3000, 5000, 8000, 13000];

const WS_PATH = "/ws";

export function connectRealtime(callbacks: RealtimeCallbacks): () => void {
  let socket: WebSocket | null = null;
  let attempt = 0;
  let timer: number | null = null;
  let stopped = false;

  const scheduleReconnect = () => {
    if (stopped) {
      return;
    }
    const delay = RETRY_DELAYS[Math.min(attempt, RETRY_DELAYS.length - 1)];
    attempt += 1;
    timer = window.setTimeout(() => void open(), delay);
  };

  const open = async () => {
    if (stopped) {
      return;
    }
    const token = await getValidAccessToken();
    if (stopped) {
      return;
    }
    if (!token) {
      callbacks.onAuthFailed?.();
      return;
    }
    const scheme = window.location.protocol === "https:" ? "wss" : "ws";
    socket = new WebSocket(`${scheme}://${window.location.host}${WS_PATH}?token=${encodeURIComponent(token)}`);

    socket.onopen = () => {
      attempt = 0;
      callbacks.onOpen?.();
    };
    socket.onmessage = (event) => {
      try {
        const parsed = JSON.parse(event.data as string) as RealtimeEvent;
        if (
          parsed.type === "NEW_MESSAGE" ||
          parsed.type === "CONVERSATION_UPDATED" ||
          parsed.type === "TYPING" ||
          parsed.type === "MESSAGES_READ"
        ) {
          callbacks.onEvent(parsed);
        }
      } catch {
        // неизвестный формат — пропускаем, не рвем соединение
      }
    };
    socket.onclose = (event) => {
      if (stopped) {
        return;
      }
      if (event.code === UNAUTHORIZED_CLOSE_CODE) {
        callbacks.onAuthFailed?.();
        return;
      }
      scheduleReconnect();
    };
    socket.onerror = () => {
      // браузер вслед за onerror пришлёт onclose — там и решаем
      socket?.close();
    };
  };

  void open();

  return () => {
    stopped = true;
    if (timer !== null) {
      window.clearTimeout(timer);
    }
    socket?.close();
  };
}

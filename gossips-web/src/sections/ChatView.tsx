import { useCallback, useEffect, useLayoutEffect, useRef, useState } from "react";
import { format } from "date-fns";
import { ArrowLeft, Check, CheckCheck, History, Lock, ShieldAlert, ShieldCheck } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { api, ApiError } from "@/lib/api";
import { avatarStyle } from "@/lib/avatar";
import { decryptMessage, encryptGroupMessage, encryptMessage, ensureIdentity } from "@/lib/crypto";
import { fetchPeerKey } from "@/lib/keys";
import type { Conversation, ConversationMember, Message, MessagesPage, PublicKeyInfo } from "@/lib/types";
import type { LiveMessage } from "@/sections/MessengerView";
import VerifyKeyDialog from "@/sections/VerifyKeyDialog";

/** Сообщение с расшифрованным текстом для отображения. */
interface DisplayMessage extends Message {
  plain: string;
}

/** Тик «печатает» — каждый TYPING-событие от собеседника инкрементит seq. */
export interface TypingTick {
  seq: number;
}

/** Тик «прочитано» — собеседник открыл диалог, свои сообщения помечаем прочитанными. */
export interface ReadTick {
  seq: number;
  conversationId: string;
}

interface ChatViewProps {
  conversation: Conversation;
  currentUsername: string;
  onBack: () => void;
  /** Список диалогов в сайдбаре надо перечитать (прочтение/новое сообщение). */
  onActivity: () => void;
  /** Последнее живое сообщение из WebSocket (см. MessengerView). */
  liveMessage?: LiveMessage | null;
  /** Тик «печатает» от собеседника. */
  typingTick?: TypingTick | null;
  /** Тик «прочитано» от собеседника. */
  readTick?: ReadTick | null;
}

/** Размер страницы истории; сервер клампит в [1, maxLimit] (см. PaginationProperties). */
const HISTORY_PAGE = 50;
/** Пауза между typing-сигналами к серверу, мс (сигнал эфемерный, без state). */
const TYPING_THROTTLE_MS = 3000;
/** Как долго показываем «печатает…» после последнего сигнала, мс. */
const TYPING_VISIBLE_MS = 3500;
const MESSAGES_BASE = (conversationId: string) => `/api/conversations/${conversationId}/messages`;

/**
 * Переписка 1-на-1 или групповая. Все сообщения шифруются E2E: content на сервере —
 * base64(JSON), здесь расшифровываем своим приватным ключом (см. lib/crypto).
 * Для группы ключи всех участников запрашиваются по /members + /api/keys/{username}.
 */
export default function ChatView({ conversation, currentUsername, onBack, onActivity, liveMessage, typingTick, readTick }: ChatViewProps) {
  const isGroup = conversation.type === "group";
  const [messages, setMessages] = useState<DisplayMessage[] | null>(null);
  const [nextBeforeId, setNextBeforeId] = useState<string | null>(null);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [peerKey, setPeerKey] = useState<PublicKeyInfo | null | undefined>(undefined);
  /** Группа: участники с отображаемыми именами (нужны и для шапки, и для подписей). */
  const [members, setMembers] = useState<ConversationMember[] | null>(null);
  /** Группа: публичные ключи участников (username → ключ). null — ещё грузим. */
  const [memberKeys, setMemberKeys] = useState<Map<string, PublicKeyInfo> | null>(null);
  /** Группа: участники без опубликованного ключа — отправка недоступна. */
  const [missingKeys, setMissingKeys] = useState<string[]>([]);
  const [draft, setDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);
  const [verifyOpen, setVerifyOpen] = useState(false);
  const [peerTyping, setPeerTyping] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const scrollAdjust = useRef(0);
  const lastTypingSent = useRef(0);
  const typingTimer = useRef<number | null>(null);

  const decryptOne = useCallback(
    async (message: Message) => ({
      ...message,
      plain: message.encrypted
        ? await decryptMessage(await ensureIdentity(), message.content, {
            isOwn: message.username === currentUsername,
            username: currentUsername,
          }).catch(() => "⚠ Не удалось расшифровать")
        : message.content,
    }),
    [currentUsername],
  );

  const loadMessages = useCallback(async () => {
    setMessages(null);
    setNextBeforeId(null);
    const page = await api<MessagesPage>(`${MESSAGES_BASE(conversation.id)}?limit=${HISTORY_PAGE}`);
    const decrypted: DisplayMessage[] = await Promise.all(page.messages.map(decryptOne));
    setMessages(decrypted);
    setNextBeforeId(page.nextBeforeId);
  }, [conversation.id, decryptOne]);

  // Для группы тянем участников и их публичные ключи — без них не зашифровать
  const loadGroupKeys = useCallback(async () => {
    setMembers(null);
    setMemberKeys(null);
    setMissingKeys([]);
    try {
      const memberList = await api<ConversationMember[]>(`/api/conversations/${conversation.id}/members`);
      setMembers(memberList);
      const keys = new Map<string, PublicKeyInfo>();
      const missing: string[] = [];
      for (const member of memberList) {
        const info = await fetchPeerKey(member.username).catch(() => null);
        if (info) {
          keys.set(member.username, info);
        } else {
          missing.push(member.displayName || member.username);
        }
      }
      setMemberKeys(keys);
      setMissingKeys(missing);
    } catch {
      setMemberKeys(new Map());
      setMissingKeys(["участники недоступны"]);
    }
  }, [conversation.id]);

  useEffect(() => {
    setError(null);
    loadMessages().catch(() => setError("Не удалось загрузить сообщения"));
    if (isGroup) {
      loadGroupKeys();
    } else {
      fetchPeerKey(conversation.peer ?? "")
        .then(setPeerKey)
        .catch(() => setPeerKey(null));
    }
    // Помечаем прочитанным при открытии
    api(`/api/conversations/${conversation.id}/read`, { method: "POST" })
      .then(onActivity)
      .catch(() => undefined);
  }, [conversation.id, conversation.peer, isGroup, loadMessages, loadGroupKeys, onActivity]);

  // Подгрузка предыдущей страницы: сохраняем позицию скролла (список растёт вверх)
  const loadOlder = async () => {
    if (!nextBeforeId || loadingOlder) {
      return;
    }
    setLoadingOlder(true);
    try {
      const viewport = listRef.current?.closest('[data-slot="scroll-area-viewport"]');
      scrollAdjust.current = viewport ? viewport.scrollHeight - viewport.scrollTop : 0;
      const page = await api<MessagesPage>(
        `${MESSAGES_BASE(conversation.id)}?beforeId=${nextBeforeId}&limit=${HISTORY_PAGE}`,
      );
      const older = await Promise.all(page.messages.map(decryptOne));
      setMessages((prev) => {
        if (!prev) {
          return older;
        }
        const known = new Set(prev.map((m) => m.id));
        return [...older.filter((m) => !known.has(m.id)), ...prev];
      });
      setNextBeforeId(page.nextBeforeId);
    } catch {
      setError("Не удалось загрузить ранние сообщения");
    } finally {
      setLoadingOlder(false);
    }
  };

  useLayoutEffect(() => {
    if (scrollAdjust.current > 0) {
      const viewport = listRef.current?.closest('[data-slot="scroll-area-viewport"]');
      if (viewport) {
        viewport.scrollTop = viewport.scrollHeight - scrollAdjust.current;
      }
      scrollAdjust.current = 0;
    }
  }, [messages]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: "end" });
  }, [messages === null ? 0 : messages.length]);

  // Живое сообщение по WebSocket: расшифровываем, добавляем, помечаем прочитанным
  useEffect(() => {
    if (!liveMessage || liveMessage.conversationId !== conversation.id) {
      return;
    }
    const incoming = liveMessage.message;
    let cancelled = false;
    void (async () => {
      const display = await decryptOne(incoming);
      if (cancelled) {
        return;
      }
      setMessages((prev) =>
        prev && !prev.some((m) => m.id === incoming.id) ? [display, ...prev] : prev,
      );
      // Пришло реальное сообщение — «печатает» больше не показываем
      setPeerTyping(false);
      api(`/api/conversations/${conversation.id}/read`, { method: "POST" })
        .then(onActivity)
        .catch(() => undefined);
    })();
    return () => {
      cancelled = true;
    };
  }, [liveMessage, conversation.id, decryptOne, onActivity]);

  // «Прочитано»: собеседник открыл диалог — свои сообщения помечаем без рефетча
  useEffect(() => {
    if (!readTick || readTick.conversationId !== conversation.id) {
      return;
    }
    setMessages((prev) =>
      (prev ?? []).map((message) =>
        message.username === currentUsername ? { ...message, read: true } : message,
      ),
    );
  }, [readTick, conversation.id, currentUsername]);

  // «Печатает…»: каждый тик показывает индикатор, гаснет по таймауту
  useEffect(() => {
    if (!typingTick) {
      return;
    }
    setPeerTyping(true);
    if (typingTimer.current !== null) {
      window.clearTimeout(typingTimer.current);
    }
    typingTimer.current = window.setTimeout(() => setPeerTyping(false), TYPING_VISIBLE_MS);
    return () => {
      if (typingTimer.current !== null) {
        window.clearTimeout(typingTimer.current);
      }
    };
  }, [typingTick]);

  const notifyTyping = () => {
    const now = Date.now();
    if (now - lastTypingSent.current < TYPING_THROTTLE_MS) {
      return;
    }
    lastTypingSent.current = now;
    api(`/api/conversations/${conversation.id}/typing`, { method: "POST" }).catch(() => undefined);
  };

  const ready = isGroup ? memberKeys !== null && missingKeys.length === 0 : !!peerKey;

  const send = async () => {
    const text = draft.trim();
    if (!text || !ready || sending) {
      return;
    }
    setSending(true);
    setError(null);
    try {
      const pair = await ensureIdentity();
      // Группа: один сеансовый ключ, зашитый на публичный ключ каждого участника
      const content = isGroup
        ? await encryptGroupMessage(
            [...(memberKeys ?? new Map<string, PublicKeyInfo>())].map(([username, info]) => ({
              username,
              publicKey: info.publicKey,
            })),
            text,
          )
        : await encryptMessage(pair, (peerKey as PublicKeyInfo).publicKey, text);
      const stored = await api<Message>(MESSAGES_BASE(conversation.id), {
        method: "POST",
        body: { content, encrypted: true },
      });
      // Локальное эхо: свой текст показываем как есть (серверное тело — шифротекст)
      setMessages((prev) =>
        prev ? [{ ...stored, plain: text }, ...prev] : [{ ...stored, plain: text }],
      );
      setDraft("");
      onActivity();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Не удалось отправить сообщение");
    } finally {
      setSending(false);
    }
  };

  const title = conversation.displayName || conversation.peer || conversation.title || "Группа";

  return (
    <div className="flex h-full min-w-0 flex-1 flex-col">
      {/* Шапка чата */}
      <header className="flex items-center gap-3 border-b p-3">
        <Button variant="ghost" size="icon" className="md:hidden" onClick={onBack} aria-label="Назад к списку">
          <ArrowLeft className="size-4" aria-hidden />
        </Button>
        <Avatar>
          <AvatarFallback style={avatarStyle(title)}>{title.slice(0, 1).toUpperCase()}</AvatarFallback>
        </Avatar>
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <span className="truncate font-medium">{title}</span>
            {!isGroup && peerKey && (
              <button
                type="button"
                onClick={() => setVerifyOpen(true)}
                className="shrink-0 rounded-full p-0.5 transition-colors hover:bg-secondary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                aria-label={peerKey.verified ? "Ключ верифицирован — подробности" : "Сообщения зашифрованы — верифицировать ключ"}
                title={peerKey.verified ? "Ключ верифицирован" : "Сообщения зашифрованы. Нажмите, чтобы сверить отпечатки"}
              >
                {peerKey.verified ? (
                  <ShieldCheck className="size-4 text-primary" aria-hidden />
                ) : (
                  <Lock className="size-4 text-muted-foreground" aria-hidden />
                )}
              </button>
            )}
            {isGroup && (
              <Lock className="size-4 shrink-0 text-muted-foreground" aria-hidden aria-label="Сообщения зашифрованы" />
            )}
          </div>
          <span className="block truncate text-xs text-muted-foreground">
            {isGroup
              ? members
                ? members.map((m) => m.displayName).join(", ")
                : "Загрузка участников…"
              : `@${conversation.peer}`}
            {peerTyping && <span className="text-primary"> · печатает…</span>}
          </span>
        </div>
      </header>

      {/* Предупреждения */}
      {!isGroup && peerKey === null && (
        <div className="flex items-center gap-2 border-b border-destructive/40 bg-destructive/10 px-4 py-2 text-sm text-destructive">
          <ShieldAlert className="size-4 shrink-0" aria-hidden />
          У собеседника нет опубликованного ключа — отправка недоступна, он ещё не входил с нового клиента.
        </div>
      )}
      {isGroup && memberKeys !== null && missingKeys.length > 0 && (
        <div className="flex items-center gap-2 border-b border-destructive/40 bg-destructive/10 px-4 py-2 text-sm text-destructive">
          <ShieldAlert className="size-4 shrink-0" aria-hidden />
          Нет опубликованного ключа у: {missingKeys.join(", ")} — отправка в беседу недоступна.
        </div>
      )}
      {error && (
        <p role="alert" className="border-b border-destructive/40 bg-destructive/10 px-4 py-2 text-sm text-destructive">
          {error}
        </p>
      )}

      {/* История (newest first с сервера — рендерим от старых к новым) */}
      <ScrollArea className="flex-1">
        <div ref={listRef} className="flex flex-col gap-1.5 p-4">
          {messages !== null && nextBeforeId !== null && (
            <div className="pb-1 text-center">
              <Button variant="ghost" size="sm" disabled={loadingOlder} onClick={() => void loadOlder()}>
                <History className="size-4" aria-hidden />
                {loadingOlder ? "Загрузка…" : "Загрузить раньше"}
              </Button>
            </div>
          )}
          {messages === null && (
            <p className="py-8 text-center text-sm text-muted-foreground">Загрузка сообщений…</p>
          )}
          {[...(messages ?? [])].reverse().map((message) => {
            const own = message.username === currentUsername;
            const author = isGroup && !own
              ? members?.find((m) => m.username === message.username)?.displayName || message.username
              : null;
            return (
              <div key={message.id} className={`flex ${own ? "justify-end" : "justify-start"}`}>
                <div
                  className={`max-w-[75%] rounded-2xl px-3.5 py-2 text-[15px] leading-relaxed ${
                    own ? "rounded-br-sm bg-primary text-primary-foreground" : "rounded-bl-sm bg-secondary"
                  }`}
                >
                  {author && <p className="mb-0.5 text-xs font-semibold text-primary/80">{author}</p>}
                  <p className="break-words whitespace-pre-wrap">{message.plain}</p>
                  <div
                    className={`mt-1 flex items-center justify-end gap-1 text-[11px] ${
                      own ? "text-primary-foreground/70" : "text-muted-foreground"
                    }`}
                  >
                    {format(new Date(message.timestamp), "HH:mm")}
                    {own &&
                      (message.read ? (
                        <CheckCheck className="size-3" aria-label="Прочитано" />
                      ) : (
                        <Check className="size-3" aria-label="Доставлено" />
                      ))}
                    {message.encrypted && (
                      <Lock className="size-3" aria-label="зашифровано" />
                    )}
                  </div>
                </div>
              </div>
            );
          })}
          <div ref={bottomRef} />
        </div>
      </ScrollArea>

      {/* Композер */}
      <div className="border-t p-3">
        <form
          className="flex items-center gap-2"
          onSubmit={(e) => {
            e.preventDefault();
            void send();
          }}
        >
          <Input
            value={draft}
            onChange={(e) => {
              setDraft(e.target.value);
              if (e.target.value.trim()) {
                notifyTyping();
              }
            }}
            placeholder={
              !ready
                ? isGroup
                  ? memberKeys === null
                    ? "Загрузка ключей участников…"
                    : "У части участников нет ключей…"
                  : "Собеседник не опубликовал ключ…"
                : "Сообщение…"
            }
            disabled={!ready || sending}
            maxLength={2000}
            autoComplete="off"
          />
          <Button type="submit" disabled={!ready || !draft.trim() || sending}>
            {sending ? "…" : "Отправить"}
          </Button>
        </form>
      </div>

      {!isGroup && conversation.peer && (
        <VerifyKeyDialog
          open={verifyOpen}
          onOpenChange={setVerifyOpen}
          peer={conversation.peer}
          peerTitle={title}
          peerKey={peerKey ?? null}
          onChanged={() => fetchPeerKey(conversation.peer as string).then(setPeerKey).catch(() => undefined)}
        />
      )}
    </div>
  );
}

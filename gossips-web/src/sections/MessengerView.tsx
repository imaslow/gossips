import { useCallback, useEffect, useRef, useState } from "react";
import { LogOut, MessageSquareLock, MessagesSquare, Pencil, Plus, ShieldAlert, Users, X } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { api, ApiError } from "@/lib/api";
import { avatarStyle } from "@/lib/avatar";
import { ensureAndPublishKey } from "@/lib/keys";
import { connectRealtime } from "@/lib/realtime";
import type { Contact, Conversation, Message } from "@/lib/types";
import { useAuth } from "@/hooks/use-auth";
import ChatView from "@/sections/ChatView";
import type { ReadTick, TypingTick } from "@/sections/ChatView";
import ContactsView from "@/sections/ContactsView";
import ProfileDialog from "@/sections/ProfileDialog";

/** Живое сообщение, пришедшее по WebSocket, для активного чата. */
export interface LiveMessage {
  conversationId: string;
  message: Message;
  seq: number;
}

/** Ключ sessionStorage для восстановления открытого чата после перезагрузки. */
const ACTIVE_CONVERSATION_KEY = "gossips.activeConversation";

/** Подпись под диалогом в списке: шифротекст не показываем. */
function messagePreview(conversation: Conversation): string {
  if (conversation.lastMessage === null) {
    return "Нет сообщений";
  }
  return conversation.lastMessageEncrypted ? "🔒 Зашифрованное сообщение" : conversation.lastMessage;
}

/**
 * Оболочка мессенджера: сайдбар со списком диалогов + активная переписка.
 * При входе гарантирует наличие identity-ключа и публикует его (E2E-инфраструктура).
 */
export default function MessengerView() {
  const { session, logout } = useAuth();
  const [conversations, setConversations] = useState<Conversation[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [active, setActive] = useState<Conversation | null>(null);
  const [newOpen, setNewOpen] = useState(false);
  const [newMode, setNewMode] = useState<"pair" | "group">("pair");
  const [newPeer, setNewPeer] = useState("");
  const [newTitle, setNewTitle] = useState("");
  const [newMembers, setNewMembers] = useState<string[]>([]);
  const [contacts, setContacts] = useState<Contact[] | null>(null);
  const [newError, setNewError] = useState<string | null>(null);
  const [newPending, setNewPending] = useState(false);
  const [keyPending, setKeyPending] = useState(true);
  const [live, setLive] = useState<LiveMessage | null>(null);
  const [realtimeError, setRealtimeError] = useState(false);
  const [tab, setTab] = useState<"chats" | "contacts">("chats");
  const [profileOpen, setProfileOpen] = useState(false);
  const [liveTyping, setLiveTyping] = useState<TypingTick | null>(null);
  const [liveRead, setLiveRead] = useState<ReadTick | null>(null);

  const activeRef = useRef<Conversation | null>(null);
  activeRef.current = active;
  const liveSeq = useRef(0);
  const typingSeq = useRef(0);
  const readSeq = useRef(0);

  const username = session?.username ?? "";
  const usernameRef = useRef(username);
  usernameRef.current = username;

  /** Событие от второй стороны: в pair — от собеседника, в группе — от любого другого участника. */
  const isFromOtherSide = (eventUsername: string): boolean => {
    const active = activeRef.current;
    if (!active) {
      return false;
    }
    return active.type === "group"
      ? eventUsername !== usernameRef.current
      : eventUsername === active.peer;
  };

  const refresh = useCallback(() => {
    api<Conversation[]>("/api/conversations")
      .then(setConversations)
      .catch(() => setLoadError("Не удалось загрузить диалоги"));
  }, []);

  // Realtime: новые сообщения прилетают по WebSocket без рефетча
  useEffect(() => {
    const disconnect = connectRealtime({
      onEvent: (event) => {
        if (event.type === "CONVERSATION_UPDATED") {
          refresh(); // новый диалог (или изменение) — тянем сводки целиком
          return;
        }
        if (event.type === "TYPING") {
          if (activeRef.current?.id === event.conversationId && isFromOtherSide(event.username)) {
            typingSeq.current += 1;
            setLiveTyping({ seq: typingSeq.current });
          }
          return;
        }
        if (event.type === "MESSAGES_READ") {
          if (activeRef.current?.id === event.conversationId && isFromOtherSide(event.username)) {
            readSeq.current += 1;
            setLiveRead({ seq: readSeq.current, conversationId: event.conversationId });
          }
          return;
        }
        const isActive = activeRef.current?.id === event.conversationId;
        setConversations((prev) => {
          const list = prev ?? [];
          const existing = list.find((c) => c.id === event.conversationId);
          if (!existing) {
            // Диалога нет в списке (создан на другом устройстве) — тянем всё с сервера
            refresh();
            return list;
          }
          const updated: Conversation = {
            ...existing,
            lastMessage: event.message.content,
            lastMessageAt: event.message.timestamp,
            lastMessageEncrypted: event.message.encrypted,
            unreadCount: isActive ? existing.unreadCount : existing.unreadCount + 1,
          };
          return [updated, ...list.filter((c) => c.id !== event.conversationId)];
        });
        if (isActive) {
          liveSeq.current += 1;
          setLive({ conversationId: event.conversationId, message: event.message, seq: liveSeq.current });
        }
      },
      onOpen: () => {
        setRealtimeError(false);
        refresh(); // после reconnect синхронизируем пропущенное
      },
      onAuthFailed: () => setRealtimeError(true),
    });
    return disconnect;
  }, [refresh]);

  // Публикация identity-ключа при входе — без него нас не смогут шифровать
  useEffect(() => {
    setKeyPending(true);
    ensureAndPublishKey()
      .catch(() => setLoadError("Не удалось опубликовать E2E-ключ"))
      .finally(() => setKeyPending(false));
    refresh();
  }, [refresh]);

  // Восстановление открытого чата после перезагрузки страницы
  useEffect(() => {
    if (active || !conversations) {
      return;
    }
    const stored = sessionStorage.getItem(ACTIVE_CONVERSATION_KEY);
    if (!stored) {
      return;
    }
    const found = conversations.find((c) => c.id === stored);
    if (found) {
      setActive(found);
    } else {
      sessionStorage.removeItem(ACTIVE_CONVERSATION_KEY);
    }
  }, [conversations, active]);

  // Счётчик непрочитанных в заголовке вкладки
  useEffect(() => {
    const total = (conversations ?? []).reduce((sum, c) => sum + c.unreadCount, 0);
    document.title = total > 0 ? `(${total}) Gossips` : "Gossips";
  }, [conversations]);

  const openConversation = (conversation: Conversation) => {
    setActive(conversation);
    sessionStorage.setItem(ACTIVE_CONVERSATION_KEY, conversation.id);
    setTab("chats");
    // Свежие сводки (unreadCount) после открытия перечитаем по событию из ChatView
  };

  const closeConversation = () => {
    setActive(null);
    sessionStorage.removeItem(ACTIVE_CONVERSATION_KEY);
  };

  const startConversation = async (username: string) => {
    const peer = username.trim();
    if (!peer || newPending) {
      return;
    }
    setNewPending(true);
    setNewError(null);
    try {
      const conversation = await api<Conversation>("/api/conversations", {
        method: "POST",
        body: { username: peer },
      });
      setNewOpen(false);
      setNewPeer("");
      openCreated(conversation);
    } catch (e) {
      setNewError(e instanceof ApiError ? e.message : "Не удалось создать диалог");
    } finally {
      setNewPending(false);
    }
  };

  const startConversationFromForm = () => void startConversation(newPeer);

  // Открытие диалога создания: в режиме группы нужен ростер контактов для выбора
  const openNewDialog = (open: boolean) => {
    setNewOpen(open);
    setNewError(null);
    if (open) {
      setNewPeer("");
      setNewTitle("");
      setNewMembers([]);
      api<Contact[]>("/api/contacts")
        .then(setContacts)
        .catch(() => setContacts([]));
    }
  };

  const toggleMember = (username: string) => {
    setNewMembers((prev) =>
      prev.includes(username) ? prev.filter((u) => u !== username) : [...prev, username],
    );
  };

  const openCreated = (conversation: Conversation) => {
    setConversations((prev) => [conversation, ...(prev ?? []).filter((c) => c.id !== conversation.id)]);
    setActive(conversation);
    sessionStorage.setItem(ACTIVE_CONVERSATION_KEY, conversation.id);
    setTab("chats");
  };

  const startGroup = async () => {
    const title = newTitle.trim();
    if (!title || newMembers.length < 2 || newPending) {
      return;
    }
    setNewPending(true);
    setNewError(null);
    try {
      const conversation = await api<Conversation>("/api/conversations/group", {
        method: "POST",
        body: { usernames: newMembers, title },
      });
      setNewOpen(false);
      openCreated(conversation);
    } catch (e) {
      setNewError(e instanceof ApiError ? e.message : "Не удалось создать группу");
    } finally {
      setNewPending(false);
    }
  };

  return (
    <div className="flex h-dvh">
      {/* Сайдбар: профиль + список диалогов */}
      <aside
        className={`w-full max-w-xs flex-col border-r bg-sidebar text-sidebar-foreground md:flex ${
          active ? "hidden md:flex" : "flex"
        }`}
      >
        <div className="flex items-center gap-2 p-4">
          <div className="flex size-9 items-center justify-center rounded-xl bg-primary/15 text-primary">
            <MessageSquareLock className="size-5" aria-hidden />
          </div>
          <span className="flex-1 text-lg font-semibold tracking-tight">Gossips</span>
          <Button variant="ghost" size="icon" onClick={() => openNewDialog(true)} aria-label="Новый диалог" title="Новый диалог">
            <Plus className="size-4" aria-hidden />
          </Button>
        </div>
        <Separator />
        <div className="flex border-b">
          <button
            type="button"
            onClick={() => setTab("chats")}
            className={`flex flex-1 items-center justify-center gap-1.5 py-2 text-sm transition-colors ${
              tab === "chats" ? "border-b-2 border-primary font-medium text-foreground" : "text-muted-foreground hover:text-foreground"
            }`}
          >
            <MessagesSquare className="size-4" aria-hidden />
            Чаты
          </button>
          <button
            type="button"
            onClick={() => setTab("contacts")}
            className={`flex flex-1 items-center justify-center gap-1.5 py-2 text-sm transition-colors ${
              tab === "contacts" ? "border-b-2 border-primary font-medium text-foreground" : "text-muted-foreground hover:text-foreground"
            }`}
          >
            <Users className="size-4" aria-hidden />
            Контакты
          </button>
        </div>

        {realtimeError && (
          <div className="flex items-center gap-2 border-b border-destructive/40 bg-destructive/10 px-4 py-2 text-sm text-destructive">
            <ShieldAlert className="size-4 shrink-0" aria-hidden />
            Realtime-соединение не авторизовано — обновите страницу или войдите заново.
          </div>
        )}

        {tab === "contacts" && (
          <div className="min-h-0 flex-1">
            <ContactsView onStartConversation={(username) => void startConversation(username)} />
          </div>
        )}

        {tab === "chats" && (
        <ScrollArea className="flex-1">
          {keyPending && (
            <p className="p-4 text-sm text-muted-foreground">Подготовка E2E-ключа…</p>
          )}
          {conversations === null && !loadError && !keyPending && (
            <p className="p-4 text-sm text-muted-foreground">Загрузка диалогов…</p>
          )}
          {loadError && <p className="p-4 text-sm text-destructive">{loadError}</p>}
          {!keyPending && conversations?.length === 0 && (
            <p className="p-4 text-sm text-muted-foreground">
              Диалогов пока нет. Нажмите «+», чтобы начать переписку.
            </p>
          )}
          <ul>
            {conversations?.map((conversation) => (
              <li key={conversation.id}>
                <button
                  type="button"
                  onClick={() => openConversation(conversation)}
                  className={`flex w-full items-center gap-3 p-3 text-left transition-colors hover:bg-sidebar-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sidebar-ring ${
                    active?.id === conversation.id ? "bg-sidebar-accent" : ""
                  }`}
                >
                  <Avatar>
                    <AvatarFallback style={avatarStyle(conversation.displayName || conversation.peer || conversation.title || "")}>
                      {(conversation.displayName || conversation.peer || conversation.title || "?").slice(0, 1).toUpperCase()}
                    </AvatarFallback>
                  </Avatar>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-baseline justify-between gap-2">
                      <span className="truncate font-medium">
                        {conversation.displayName || conversation.peer || conversation.title}
                      </span>
                      {conversation.unreadCount > 0 && (
                        <Badge className="px-1.5">{conversation.unreadCount}</Badge>
                      )}
                    </div>
                    <p className="truncate text-sm text-muted-foreground">
                      {messagePreview(conversation)}
                    </p>
                  </div>
                </button>
              </li>
            ))}
          </ul>
        </ScrollArea>
        )}

        <Separator />
        <div className="flex items-center gap-3 p-3">
          <button
            type="button"
            onClick={() => setProfileOpen(true)}
            className="flex min-w-0 flex-1 items-center gap-3 rounded-lg text-left transition-colors hover:bg-sidebar-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sidebar-ring"
            aria-label="Редактировать профиль"
            title="Профиль"
          >
            <Avatar>
              <AvatarFallback style={avatarStyle(username)}>{username.slice(0, 1).toUpperCase()}</AvatarFallback>
            </Avatar>
            <span className="min-w-0 flex-1 truncate text-sm font-medium">{username}</span>
            <Pencil className="size-4 shrink-0 text-muted-foreground" aria-hidden />
          </button>
          <Button variant="ghost" size="icon" onClick={() => void logout()} aria-label="Выйти" title="Выйти">
            <LogOut className="size-4" aria-hidden />
          </Button>
        </div>
      </aside>

      <ProfileDialog open={profileOpen} onOpenChange={setProfileOpen} onSaved={refresh} />

      {/* Область переписки */}
      {active ? (
        <ChatView
          conversation={active}
          currentUsername={username}
          onBack={closeConversation}
          onActivity={refresh}
          liveMessage={live}
          typingTick={liveTyping}
          readTick={liveRead}
        />
      ) : (
        <main className="hidden min-w-0 flex-1 items-center justify-center bg-background p-8 md:flex">
          <div className="w-full max-w-sm text-center">
            <MessagesSquare className="mx-auto mb-4 size-10 text-muted-foreground" aria-hidden />
            <h1 className="text-lg font-semibold">Выберите диалог</h1>
            <p className="mt-2 text-sm text-muted-foreground">
              Или начните новый — кнопка «+» в панели слева. Все сообщения шифруются
              end-to-end на вашем устройстве.
            </p>
          </div>
        </main>
      )}

      {/* Диалог создания переписки: 1-на-1 или группа */}
      <Dialog open={newOpen} onOpenChange={openNewDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Новая переписка</DialogTitle>
            <DialogDescription>
              Личный диалог с одним пользователем или групповая беседа с несколькими.
            </DialogDescription>
          </DialogHeader>
          <div className="flex rounded-lg border p-0.5" role="tablist" aria-label="Тип переписки">
            {(["pair", "group"] as const).map((mode) => (
              <button
                key={mode}
                type="button"
                role="tab"
                aria-selected={newMode === mode}
                onClick={() => {
                  setNewMode(mode);
                  setNewError(null);
                }}
                className={`flex-1 rounded-md py-1.5 text-sm transition-colors ${
                  newMode === mode ? "bg-primary font-medium text-primary-foreground" : "text-muted-foreground hover:text-foreground"
                }`}
              >
                {mode === "pair" ? "Диалог" : "Группа"}
              </button>
            ))}
          </div>
          {newMode === "pair" ? (
            <form
              className="space-y-4"
              onSubmit={(e) => {
                e.preventDefault();
                startConversationFromForm();
              }}
            >
              <div className="space-y-2">
                <Label htmlFor="new-peer">Имя пользователя</Label>
                <Input
                  id="new-peer"
                  value={newPeer}
                  onChange={(e) => setNewPeer(e.target.value)}
                  minLength={3}
                  maxLength={64}
                  autoComplete="off"
                  required
                />
              </div>
              {newError && (
                <p role="alert" className="text-sm text-destructive">
                  {newError}
                </p>
              )}
              <div className="flex justify-end gap-2">
                <Button type="button" variant="ghost" onClick={() => openNewDialog(false)}>
                  <X className="size-4" aria-hidden />
                  Отмена
                </Button>
                <Button type="submit" disabled={newPending || !newPeer.trim()}>
                  {newPending ? "Подождите…" : "Начать диалог"}
                </Button>
              </div>
            </form>
          ) : (
            <form
              className="space-y-4"
              onSubmit={(e) => {
                e.preventDefault();
                void startGroup();
              }}
            >
              <div className="space-y-2">
                <Label htmlFor="new-title">Название группы</Label>
                <Input
                  id="new-title"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  minLength={1}
                  maxLength={128}
                  autoComplete="off"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label>Участники ({newMembers.length} выбрано, минимум 2)</Label>
                <div className="max-h-48 overflow-y-auto rounded-lg border">
                  {contacts === null && (
                    <p className="p-3 text-sm text-muted-foreground">Загрузка контактов…</p>
                  )}
                  {contacts?.length === 0 && (
                    <p className="p-3 text-sm text-muted-foreground">
                      Контактов пока нет — добавьте их на вкладке «Контакты».
                    </p>
                  )}
                  {contacts?.map((contact) => {
                    const label = contact.displayName || contact.username;
                    return (
                      <label
                        key={contact.username}
                        className="flex cursor-pointer items-center gap-3 p-2.5 transition-colors hover:bg-secondary"
                      >
                        <input
                          type="checkbox"
                          className="size-4 accent-primary"
                          checked={newMembers.includes(contact.username)}
                          onChange={() => toggleMember(contact.username)}
                          aria-label={`Выбрать ${label}`}
                        />
                        <Avatar className="size-7">
                          <AvatarFallback style={avatarStyle(label)} className="text-xs">
                            {label.slice(0, 1).toUpperCase()}
                          </AvatarFallback>
                        </Avatar>
                        <span className="min-w-0 truncate text-sm">{label}</span>
                      </label>
                    );
                  })}
                </div>
              </div>
              {newError && (
                <p role="alert" className="text-sm text-destructive">
                  {newError}
                </p>
              )}
              <div className="flex justify-end gap-2">
                <Button type="button" variant="ghost" onClick={() => openNewDialog(false)}>
                  <X className="size-4" aria-hidden />
                  Отмена
                </Button>
                <Button type="submit" disabled={newPending || !newTitle.trim() || newMembers.length < 2}>
                  {newPending ? "Подождите…" : "Создать группу"}
                </Button>
              </div>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}

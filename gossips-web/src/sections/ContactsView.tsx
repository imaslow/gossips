import { useCallback, useEffect, useState } from "react";
import { MessageSquarePlus, Trash2, UserPlus } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api, ApiError } from "@/lib/api";
import { avatarStyle } from "@/lib/avatar";
import type { Contact } from "@/lib/types";

interface ContactsViewProps {
  /** Клик по контакту — начать (или открыть) диалог. */
  onStartConversation: (username: string) => void;
}

/**
 * Ростер контактов: список, добавление по username, удаление.
 * Добавление идемпотентно (сервер не дублирует).
 */
export default function ContactsView({ onStartConversation }: ContactsViewProps) {
  const [contacts, setContacts] = useState<Contact[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [newName, setNewName] = useState("");
  const [addError, setAddError] = useState<string | null>(null);
  const [addPending, setAddPending] = useState(false);

  const refresh = useCallback(() => {
    api<Contact[]>("/api/contacts")
      .then(setContacts)
      .catch(() => setLoadError("Не удалось загрузить контакты"));
  }, []);

  useEffect(refresh, [refresh]);

  const add = async () => {
    const username = newName.trim();
    if (!username || addPending) {
      return;
    }
    setAddPending(true);
    setAddError(null);
    try {
      await api<Contact>("/api/contacts", { method: "POST", body: { username } });
      setNewName("");
      refresh();
    } catch (e) {
      setAddError(e instanceof ApiError ? e.message : "Не удалось добавить контакт");
    } finally {
      setAddPending(false);
    }
  };

  const remove = async (username: string) => {
    await api(`/api/contacts/${encodeURIComponent(username)}`, { method: "DELETE" }).catch(() => undefined);
    refresh();
  };

  return (
    <div className="flex h-full flex-col">
      <form
        className="flex items-center gap-2 p-3"
        onSubmit={(e) => {
          e.preventDefault();
          void add();
        }}
      >
        <Input
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          placeholder="Добавить по имени…"
          minLength={3}
          maxLength={64}
          autoComplete="off"
        />
        <Button type="submit" size="icon" variant="ghost" disabled={addPending || !newName.trim()} aria-label="Добавить контакт">
          <UserPlus className="size-4" aria-hidden />
        </Button>
      </form>
      {addError && <p role="alert" className="px-3 pb-2 text-sm text-destructive">{addError}</p>}

      <div className="flex-1 overflow-y-auto">
        {contacts === null && !loadError && (
          <p className="p-4 text-sm text-muted-foreground">Загрузка контактов…</p>
        )}
        {loadError && <p className="p-4 text-sm text-destructive">{loadError}</p>}
        {!loadError && contacts?.length === 0 && (
          <p className="p-4 text-sm text-muted-foreground">
            Контактов пока нет — добавьте по имени пользователя выше.
          </p>
        )}
        <ul>
          {contacts?.map((contact) => (
            <li key={contact.username} className="group flex items-center gap-3 p-3 hover:bg-sidebar-accent">
              <Avatar>
                <AvatarFallback style={avatarStyle(contact.displayName || contact.username)}>
                  {(contact.displayName || contact.username).slice(0, 1).toUpperCase()}
                </AvatarFallback>
              </Avatar>
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium">{contact.displayName || contact.username}</p>
                <p className="truncate text-xs text-muted-foreground">@{contact.username}</p>
              </div>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => onStartConversation(contact.username)}
                aria-label={`Написать ${contact.displayName || contact.username}`}
                title="Написать сообщение"
              >
                <MessageSquarePlus className="size-4" aria-hidden />
              </Button>
              <Button
                variant="ghost"
                size="icon"
                className="opacity-0 transition-opacity group-hover:opacity-100"
                onClick={() => void remove(contact.username)}
                aria-label={`Удалить ${contact.displayName || contact.username}`}
                title="Удалить из контактов"
              >
                <Trash2 className="size-4" aria-hidden />
              </Button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

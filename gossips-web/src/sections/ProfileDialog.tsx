import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { api, ApiError } from "@/lib/api";
import type { Profile } from "@/lib/types";

interface ProfileDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** После сохранения — перечитать диалоги (displayName в списках). */
  onSaved: () => void;
}

/**
 * Редактирование собственного профиля: отображаемое имя (обязательно) и bio.
 * Username из JWT менять нельзя.
 */
export default function ProfileDialog({ open, onOpenChange, onSaved }: ProfileDialogProps) {
  const [displayName, setDisplayName] = useState("");
  const [bio, setBio] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    setError(null);
    api<Profile>("/api/profiles/me")
      .then((profile) => {
        setDisplayName(profile.displayName);
        setBio(profile.bio ?? "");
      })
      .catch(() => setError("Не удалось загрузить профиль"));
  }, [open]);

  const save = async () => {
    if (busy || !displayName.trim()) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await api<Profile>("/api/profiles/me", {
        method: "PUT",
        body: { displayName: displayName.trim(), bio: bio.trim() || undefined },
      });
      onSaved();
      onOpenChange(false);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Не удалось сохранить профиль");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Профиль</DialogTitle>
          <DialogDescription>
            Отображаемое имя видят собеседники в списках диалогов и переписке.
          </DialogDescription>
        </DialogHeader>
        <form
          className="space-y-4"
          onSubmit={(e) => {
            e.preventDefault();
            void save();
          }}
        >
          <div className="space-y-2">
            <Label htmlFor="profile-name">Отображаемое имя</Label>
            <Input
              id="profile-name"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              minLength={1}
              maxLength={64}
              required
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="profile-bio">О себе</Label>
            <Textarea
              id="profile-bio"
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              maxLength={500}
              rows={3}
            />
          </div>
          {error && (
            <p role="alert" className="text-sm text-destructive">{error}</p>
          )}
          <div className="flex justify-end">
            <Button type="submit" disabled={busy || !displayName.trim()}>
              {busy ? "Подождите…" : "Сохранить"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

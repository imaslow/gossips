import { useEffect, useState } from "react";
import { ShieldAlert, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { ensureIdentity, fingerprintOf, formatFingerprint } from "@/lib/crypto";
import { unverifyPeerKey, verifyPeerKey } from "@/lib/keys";
import type { PublicKeyInfo } from "@/lib/types";

interface VerifyKeyDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Логин собеседника (для заголовка и API). */
  peer: string;
  peerTitle: string;
  /** Опубликованный ключ собеседника; null — ключ не опубликован. */
  peerKey: PublicKeyInfo | null;
  /** Вызывается после успешной верификации/снятия — перечитать ключ. */
  onChanged: () => void;
}

/**
 * Диалог сверки safety number: показывает отпечатки обоих участников
 * (свой — из локального identity, чужой — с сервера) и кнопку подтверждения.
 */
export default function VerifyKeyDialog({ open, onOpenChange, peer, peerTitle, peerKey, onChanged }: VerifyKeyDialogProps) {
  const [myFingerprint, setMyFingerprint] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    setError(null);
    ensureIdentity()
      .then(fingerprintOf)
      .then(setMyFingerprint)
      .catch(() => setMyFingerprint(null));
  }, [open]);

  const run = async (action: () => Promise<unknown>) => {
    if (busy) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await action();
      onChanged();
      onOpenChange(false);
    } catch {
      setError("Не удалось сохранить верификацию");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Верификация ключа</DialogTitle>
          <DialogDescription>
            Сверьте отпечатки по телефону или при встрече. Если они совпадают, ключи не
            подменены — переписку действительно видите только вы двое.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="rounded-lg bg-secondary/60 p-3">
            <p className="text-xs text-muted-foreground">{peerTitle} (@{peer})</p>
            <p className="mt-1 font-mono text-sm break-all select-all">
              {peerKey ? formatFingerprint(peerKey.fingerprint) : "Ключ не опубликован"}
            </p>
            {peerKey?.verified && (
              <p className="mt-1 flex items-center gap-1 text-xs text-primary">
                <ShieldCheck className="size-3.5" aria-hidden /> Отпечаток верифицирован
              </p>
            )}
          </div>

          <div className="rounded-lg bg-secondary/60 p-3">
            <p className="text-xs text-muted-foreground">Ваш ключ</p>
            <p className="mt-1 font-mono text-sm break-all select-all">
              {myFingerprint ? formatFingerprint(myFingerprint) : "Загрузка…"}
            </p>
          </div>

          <p className="flex items-start gap-2 text-sm text-muted-foreground">
            <ShieldAlert className="mt-0.5 size-4 shrink-0" aria-hidden />
            Верификация привязывается к отпечатку: если собеседник сменит ключ, её придётся
            пройти заново.
          </p>

          {error && (
            <p role="alert" className="text-sm text-destructive">{error}</p>
          )}

          <div className="flex justify-end gap-2">
            {peerKey?.verified ? (
              <Button
                variant="ghost"
                disabled={busy}
                onClick={() => void run(() => unverifyPeerKey(peer))}
              >
                Снять верификацию
              </Button>
            ) : (
              <Button
                disabled={busy || !peerKey}
                onClick={() => void run(() => verifyPeerKey(peer, peerKey!.fingerprint))}
              >
                {busy ? "Подождите…" : "Отпечатки совпадают — подтвердить"}
              </Button>
            )}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

import { api } from "./api";
import { ensureIdentity, exportPublicKeyBase64, fingerprintOf } from "./crypto";
import type { PublicKeyInfo, VerificationInfo } from "./types";

/**
 * Идемпотентная публикация identity-ключа: если в IndexedDB пары ещё нет — генерирует,
 * публичный ключ и отпечаток отправляет на сервер (PUT /api/keys/me — upsert).
 */
export async function ensureAndPublishKey(): Promise<void> {
  const pair = await ensureIdentity();
  const publicKey = await exportPublicKeyBase64(pair);
  const fingerprint = await fingerprintOf(pair);
  // Одна повторная попытка: при reload vite dev (re-optimization) первая
  // отправка может оборваться вместе с прокси.
  for (let attempt = 0; ; attempt++) {
    try {
      await api<PublicKeyInfo>("/api/keys/me", {
        method: "PUT",
        body: { publicKey, fingerprint },
      });
      return;
    } catch (e) {
      if (attempt === 1) {
        throw e;
      }
      await new Promise((resolve) => setTimeout(resolve, 800));
    }
  }
}

/** Публичный ключ собеседника; null — ключ не опубликован (404 KeyNotFound). */
export async function fetchPeerKey(username: string): Promise<PublicKeyInfo | null> {
  try {
    return await api<PublicKeyInfo>(`/api/keys/${encodeURIComponent(username)}`);
  } catch (e) {
    if (e instanceof Error && "status" in e && (e as { status: number }).status === 404) {
      return null;
    }
    throw e;
  }
}

/** Сохраняет верификацию отпечатка ключа собеседника (safety number сверен). */
export async function verifyPeerKey(username: string, fingerprint: string): Promise<VerificationInfo> {
  return api<VerificationInfo>(`/api/keys/${encodeURIComponent(username)}/verify`, {
    method: "PUT",
    body: { fingerprint },
  });
}

/** Снимает верификацию ключа собеседника (идемпотентно). */
export async function unverifyPeerKey(username: string): Promise<void> {
  await api(`/api/keys/${encodeURIComponent(username)}/verify`, { method: "DELETE" });
}

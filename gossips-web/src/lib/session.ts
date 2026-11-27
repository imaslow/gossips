/**
 * Хранение сессии в localStorage. Access+refresh токены живут только на клиенте;
 * приватный E2E-ключ — в IndexedDB (см. lib/crypto), сюда не попадает.
 */

const TOKEN_KEY = "gossips.token";
const REFRESH_KEY = "gossips.refresh";
const USERNAME_KEY = "gossips.username";

export interface Session {
  token: string;
  refreshToken: string;
  username: string;
}

export function loadSession(): Session | null {
  const token = localStorage.getItem(TOKEN_KEY);
  const refreshToken = localStorage.getItem(REFRESH_KEY);
  const username = localStorage.getItem(USERNAME_KEY);
  if (!token || !refreshToken || !username) {
    return null;
  }
  return { token, refreshToken, username };
}

export function saveSession(session: Session): void {
  localStorage.setItem(TOKEN_KEY, session.token);
  localStorage.setItem(REFRESH_KEY, session.refreshToken);
  localStorage.setItem(USERNAME_KEY, session.username);
}

export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_KEY);
  localStorage.removeItem(USERNAME_KEY);
}

/**
 * Обновление сессии: вызывается, когда refresh-ротация не удалась
 * (refresh отозван/просрочен) — владелец (AuthProvider) должен разлогинить.
 */
let sessionExpiredHandler: (() => void) | null = null;

export function onSessionExpired(handler: () => void): void {
  sessionExpiredHandler = handler;
}

/** Запас до истечения access-токена, чтобы не ловить 401 на границе срока. */
const EXPIRY_MARGIN_MS = 30_000;

let refreshInFlight: Promise<string | null> | null = null;

/** exp из JWT-payload; null — если токен не разобрать. */
function decodeExpiry(token: string): number | null {
  try {
    const payload = token.split(".")[1];
    const json = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
    return typeof json.exp === "number" ? json.exp : null;
  } catch {
    return null;
  }
}

/**
 * Access-токен, пригодный для запроса: живой возвращается как есть,
 * просроченный — молча меняется на новую пару через ротацию refresh.
 * Возвращает null, если сессию продлить нельзя (слушатель уже уведомлён).
 */
export function getValidAccessToken(): Promise<string | null> {
  const session = loadSession();
  if (!session) {
    return Promise.resolve(null);
  }
  const exp = decodeExpiry(session.token);
  if (exp !== null && exp * 1000 > Date.now() + EXPIRY_MARGIN_MS) {
    return Promise.resolve(session.token);
  }
  // single-flight: параллельные запросы ждут одну ротацию
  refreshInFlight ??= rotate(session.refreshToken)
    .catch(() => null)
    .finally(() => {
      refreshInFlight = null;
    });
  return refreshInFlight;
}

async function rotate(refreshToken: string): Promise<string | null> {
  const response = await fetch("/api/auth/refresh", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });
  if (!response.ok) {
    clearSession();
    sessionExpiredHandler?.();
    return null;
  }
  const auth = (await response.json()) as { token: string; refreshToken: string; username: string };
  saveSession({ token: auth.token, refreshToken: auth.refreshToken, username: auth.username });
  return auth.token;
}

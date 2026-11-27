import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { api } from "@/lib/api";
import { clearSession, loadSession, onSessionExpired, saveSession } from "@/lib/session";
import type { Session } from "@/lib/session";
import type { AuthResponse, UserCredentials } from "@/lib/types";

interface AuthContextValue {
  session: Session | null;
  login: (credentials: UserCredentials) => Promise<void>;
  register: (credentials: UserCredentials) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => loadSession());

  // Refresh-ротация не удалась (refresh отозван/просрочен) — разлогиниваем
  useEffect(() => {
    onSessionExpired(() => {
      clearSession();
      setSession(null);
    });
  }, []);

  const applyAuth = useCallback((auth: AuthResponse) => {
    const next: Session = {
      token: auth.token,
      refreshToken: auth.refreshToken,
      username: auth.username,
    };
    saveSession(next);
    setSession(next);
  }, []);

  const login = useCallback(
    async (credentials: UserCredentials) => {
      const auth = await api<AuthResponse>("/api/auth/login", {
        method: "POST",
        body: credentials,
      });
      applyAuth(auth);
    },
    [applyAuth],
  );

  const register = useCallback(
    async (credentials: UserCredentials) => {
      const auth = await api<AuthResponse>("/api/auth/register", {
        method: "POST",
        body: credentials,
      });
      applyAuth(auth);
    },
    [applyAuth],
  );

  const logout = useCallback(async () => {
    const current = loadSession();
    if (current) {
      // Отзыв refresh-токена на сервере; сессию чистим в любом случае
      try {
        await api("/api/auth/logout", {
          method: "POST",
          body: { refreshToken: current.refreshToken },
        });
      } catch {
        // сервер недоступен — всё равно выходим локально
      }
    }
    clearSession();
    setSession(null);
  }, []);

  const value = useMemo(
    () => ({ session, login, register, logout }),
    [session, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth должен использоваться внутри AuthProvider");
  }
  return context;
}

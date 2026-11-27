import type { ErrorResponse } from "./types";
import { getValidAccessToken } from "./session";

/**
 * Обёртка над fetch: проксируется на gateway (vite proxy /api → :8080),
 * подставляет Authorization, разбирает тело ошибки {"code","message"}.
 */

export class ApiError extends Error {
  readonly code: string;
  readonly status: number;

  constructor(status: number, body: ErrorResponse | null) {
    super(body?.message ?? `HTTP ${status}`);
    this.code = body?.code ?? "UNKNOWN";
    this.status = status;
  }
}

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: unknown;
}

export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const token = await getValidAccessToken();
  const headers: Record<string, string> = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(path, {
    method: options.method ?? "GET",
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  if (!response.ok) {
    let body: ErrorResponse | null = null;
    try {
      body = (await response.json()) as ErrorResponse;
    } catch {
      // тела нет — оставляем null, сообщение будет "HTTP <status>"
    }
    throw new ApiError(response.status, body);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

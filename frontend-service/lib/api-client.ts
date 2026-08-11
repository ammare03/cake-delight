import { ErrorResponseBody, ValidationFieldError } from "@/lib/types";
import { clearSession, getToken } from "@/lib/auth-storage";

// All requests go through api-gateway (CLAUDE.md §4 — single public entry
// point; downstream services are never called directly from the browser).
const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:9090/api";

/** Thrown for every non-2xx response. Carries the standard error shape (api-conventions skill) so callers can branch on `code` or render `fieldErrors` inline. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: ValidationFieldError[] | null;

  constructor(status: number, code: string, message: string, fieldErrors: ValidationFieldError[] | null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: unknown;
  /** Skip attaching the Authorization header — only the public auth/catalog GETs need this. */
  skipAuth?: boolean;
}

/**
 * The one place that knows how to reach the gateway. Every lib/*-api.ts
 * module is a thin, typed wrapper around this.
 */
export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, skipAuth = false } = options;

  const headers: Record<string, string> = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";

  const token = skipAuth ? null : getToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const res = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 204) {
    return undefined as T;
  }

  if (!res.ok) {
    let parsed: ErrorResponseBody | null = null;
    try {
      parsed = (await res.json()) as ErrorResponseBody;
    } catch {
      // Response wasn't JSON (e.g. gateway/service unreachable) — fall through to the generic error below.
    }

    // A 401 on a request that *carried* a token means the session expired or
    // was invalidated — clear it and tell the rest of the app so
    // AuthProvider can redirect to /login. A 401 with no token was just an
    // anonymous call to a protected route, not an expired session.
    if (res.status === 401 && token && typeof window !== "undefined") {
      clearSession();
      window.dispatchEvent(new CustomEvent("cake-delight:unauthorized"));
    }

    throw new ApiError(
      parsed?.status ?? res.status,
      parsed?.code ?? "UNKNOWN_ERROR",
      parsed?.message ?? "Something went wrong. Please try again.",
      parsed?.fieldErrors ?? null,
    );
  }

  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

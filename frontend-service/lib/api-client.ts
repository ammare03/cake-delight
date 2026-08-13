import { ErrorResponseBody, ValidationFieldError } from "@/lib/types";
import { clearSession, getToken } from "@/lib/auth-storage";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:9090/api";

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
  skipAuth?: boolean;
}

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
    }

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

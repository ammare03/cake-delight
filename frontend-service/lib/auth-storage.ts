import { AuthResponse } from "@/lib/types";

/**
 * Single source of truth for where the signed-in session lives (CLAUDE.md
 * §4: "Frontend stores JWT (in memory / httpOnly cookie later)").
 * localStorage for this capstone — simplest option that survives a page
 * refresh; an httpOnly cookie would need a backend-issued cookie, out of
 * scope here.
 *
 * Stores the whole login response (not just the token) so a page refresh
 * can restore `email`/`role` for the UI without decoding the JWT
 * client-side or calling the backend again.
 *
 * Both api-client.ts (attaches the Authorization header on every request)
 * and auth-context.tsx (drives UI state) read/write through this one
 * module so they can never disagree about the session.
 */

const STORAGE_KEY = "cake-delight-auth";

export type StoredSession = Pick<AuthResponse, "token" | "email" | "role">;

export function getSession(): StoredSession | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as StoredSession;
  } catch {
    return null;
  }
}

export function setSession(session: StoredSession): void {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function clearSession(): void {
  window.localStorage.removeItem(STORAGE_KEY);
}

export function getToken(): string | null {
  return getSession()?.token ?? null;
}

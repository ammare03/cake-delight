"use client";

import { createContext, useContext, useEffect, useState, ReactNode } from "react";
import { useRouter } from "next/navigation";
import { authApi } from "@/lib/auth-api";
import { clearSession, getSession, setSession, StoredSession } from "@/lib/auth-storage";
import { LoginRequest, RegisterRequest } from "@/lib/types";

interface AuthContextValue {
  user: StoredSession | null;
  isLoading: boolean;
  login: (request: LoginRequest) => Promise<void>;
  register: (request: RegisterRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<StoredSession | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    setUser(getSession());
    setIsLoading(false);

    function onUnauthorized() {
      setUser(null);
      router.push("/login");
    }
    window.addEventListener("cake-delight:unauthorized", onUnauthorized);
    return () => window.removeEventListener("cake-delight:unauthorized", onUnauthorized);
  }, [router]);

  async function login(request: LoginRequest) {
    const response = await authApi.login(request);
    const session = { token: response.token, email: response.email, role: response.role };
    setSession(session);
    setUser(session);
  }

  async function register(request: RegisterRequest) {
    const response = await authApi.register(request);
    const session = { token: response.token, email: response.email, role: response.role };
    setSession(session);
    setUser(session);
  }

  function logout() {
    clearSession();
    setUser(null);
    router.push("/");
  }

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}

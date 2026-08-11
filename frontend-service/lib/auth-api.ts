import { apiFetch } from "@/lib/api-client";
import { AuthResponse, LoginRequest, RegisterRequest } from "@/lib/types";

export const authApi = {
  register: (request: RegisterRequest) =>
    apiFetch<AuthResponse>("/auth/register", { method: "POST", body: request, skipAuth: true }),

  login: (request: LoginRequest) =>
    apiFetch<AuthResponse>("/auth/login", { method: "POST", body: request, skipAuth: true }),
};

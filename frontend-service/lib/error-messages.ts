import { ApiError } from "@/lib/api-client";

const OVERRIDES: Record<string, string> = {
  UNAUTHORIZED: "Your session has expired. Please log in again.",
  UNAUTHENTICATED: "Your session has expired. Please log in again.",
};

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return OVERRIDES[error.code] ?? error.message;
  }
  return "Something went wrong. Please try again.";
}

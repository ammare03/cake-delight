import { ApiError } from "@/lib/api-client";

// Backend messages (api-conventions skill) are already written to be
// user-safe, so most codes just pass their message straight through. Only
// override where the raw message wouldn't make sense out of context.
const OVERRIDES: Record<string, string> = {
  UNAUTHORIZED: "Your session has expired. Please log in again.",
  UNAUTHENTICATED: "Your session has expired. Please log in again.",
};

/** Turns any thrown value (ideally an ApiError) into copy safe to show in a toast. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return OVERRIDES[error.code] ?? error.message;
  }
  return "Something went wrong. Please try again.";
}

"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/auth-context";
import { Skeleton } from "@/components/ui/skeleton";

/**
 * Wraps a page that needs a signed-in user (basket, checkout, orders,
 * notifications — every route the gateway itself requires a token for,
 * see the Phase 5 plan's endpoint contract table). Client-side only: the
 * token lives in localStorage, not a cookie, so there's nothing a server
 * component or middleware could check.
 */
export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !user) {
      router.push("/login");
    }
  }, [isLoading, user, router]);

  if (isLoading || !user) {
    return (
      <div className="mx-auto max-w-4xl space-y-4 px-6 py-12">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-32 w-full" />
      </div>
    );
  }

  return <>{children}</>;
}

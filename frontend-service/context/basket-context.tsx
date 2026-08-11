"use client";

import { createContext, useCallback, useContext, useEffect, useState, ReactNode } from "react";
import { basketApi } from "@/lib/order-api";
import { Basket } from "@/lib/types";
import { useAuth } from "@/context/auth-context";

interface BasketContextValue {
  basket: Basket | null;
  itemCount: number;
  /** True only for the very first fetch — the basket page's loading skeleton keys off this, not subsequent refreshes. */
  loading: boolean;
  /** Re-fetch the basket — call after any add/update/remove/checkout so the nav badge and basket page agree. */
  refresh: () => Promise<void>;
}

const BasketContext = createContext<BasketContextValue | null>(null);

export function BasketProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const [basket, setBasket] = useState<Basket | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    if (!user) {
      setBasket(null);
      setLoading(false);
      return;
    }
    try {
      setBasket(await basketApi.get());
    } catch {
      // Basket badge is a nice-to-have, not a critical path — swallow and
      // leave the last-known basket rather than surfacing a toast for it.
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const itemCount = basket?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0;

  return <BasketContext.Provider value={{ basket, itemCount, loading, refresh }}>{children}</BasketContext.Provider>;
}

export function useBasket(): BasketContextValue {
  const ctx = useContext(BasketContext);
  if (!ctx) throw new Error("useBasket must be used within a BasketProvider");
  return ctx;
}

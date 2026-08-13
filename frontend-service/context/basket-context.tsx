"use client";

import { createContext, useCallback, useContext, useEffect, useState, ReactNode } from "react";
import { basketApi } from "@/lib/order-api";
import { Basket } from "@/lib/types";
import { useAuth } from "@/context/auth-context";

interface BasketContextValue {
  basket: Basket | null;
  itemCount: number;
  loading: boolean;
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

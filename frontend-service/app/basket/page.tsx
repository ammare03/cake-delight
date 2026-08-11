"use client";

import { useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { ProtectedRoute } from "@/components/protected-route";
import { useBasket } from "@/context/basket-context";
import { basketApi } from "@/lib/order-api";
import { errorMessage } from "@/lib/error-messages";
import { formatMoney } from "@/lib/format";
import { BasketItemRow } from "@/components/basket-item-row";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";

function BasketPageContent() {
  const { basket, loading, refresh } = useBasket();
  const [busy, setBusy] = useState(false);

  async function handleQuantityChange(itemId: number, quantity: number) {
    setBusy(true);
    try {
      await basketApi.updateItem(itemId, { quantity });
      await refresh();
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function handleRemove(itemId: number) {
    setBusy(true);
    try {
      await basketApi.removeItem(itemId);
      await refresh();
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-2xl px-6 py-10">
      <h1 className="font-heading text-3xl font-semibold text-foreground">Your Basket</h1>

      {loading ? (
        <div className="mt-8 space-y-4">
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-20 w-full" />
        </div>
      ) : !basket || basket.items.length === 0 ? (
        <div className="mt-8 text-center">
          <p className="text-muted-foreground">Your basket is empty.</p>
          <Link href="/catalog">
            <Button className="mt-4">Browse cakes</Button>
          </Link>
        </div>
      ) : (
        <Card className="mt-8">
          <CardContent className="divide-y divide-border">
            {basket.items.map((item) => (
              <BasketItemRow
                key={item.id}
                item={item}
                onQuantityChange={handleQuantityChange}
                onRemove={handleRemove}
                disabled={busy}
              />
            ))}
          </CardContent>
          <Separator />
          <CardContent className="flex items-center justify-between pt-4">
            <span className="text-lg font-medium text-foreground">Total</span>
            <span className="font-heading text-xl text-primary">{formatMoney(basket.totalAmount)}</span>
          </CardContent>
          <CardContent className="pt-0">
            <Link href="/checkout">
              <Button className="w-full" disabled={busy}>
                Proceed to checkout
              </Button>
            </Link>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

export default function BasketPage() {
  return (
    <ProtectedRoute>
      <BasketPageContent />
    </ProtectedRoute>
  );
}

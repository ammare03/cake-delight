"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { ProtectedRoute } from "@/components/protected-route";
import { useBasket } from "@/context/basket-context";
import { orderApi } from "@/lib/order-api";
import { errorMessage } from "@/lib/error-messages";
import { formatMoney } from "@/lib/format";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";

function CheckoutPageContent() {
  const { basket, loading, refresh } = useBasket();
  const [placing, setPlacing] = useState(false);
  const router = useRouter();

  async function handlePlaceOrder() {
    setPlacing(true);
    try {
      const order = await orderApi.checkout();
      await refresh();
      toast.success("Order placed!");
      router.push(`/orders/${order.id}`);
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setPlacing(false);
    }
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-lg space-y-4 px-6 py-10">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  if (!basket || basket.items.length === 0) {
    return (
      <div className="mx-auto max-w-lg px-6 py-16 text-center">
        <p className="text-muted-foreground">Your basket is empty — nothing to check out yet.</p>
        <Link href="/catalog">
          <Button className="mt-4">Browse cakes</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-lg px-6 py-10">
      <h1 className="font-heading text-3xl font-semibold text-foreground">Checkout</h1>

      <Card className="mt-8">
        <CardHeader>
          <CardTitle>Order summary</CardTitle>
          <CardDescription>Cake Delight doesn&apos;t take payment online yet — placing an order confirms it for pickup/delivery arrangement.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {basket.items.map((item) => (
            <div key={item.id} className="flex items-center justify-between text-sm">
              <span className="text-foreground">
                {item.cakeName} <span className="text-muted-foreground">× {item.quantity}</span>
              </span>
              <span className="text-foreground">{formatMoney(item.lineTotal)}</span>
            </div>
          ))}
          <Separator />
          <div className="flex items-center justify-between font-medium">
            <span className="text-foreground">Total</span>
            <span className="font-heading text-lg text-primary">{formatMoney(basket.totalAmount)}</span>
          </div>
        </CardContent>
        <CardContent className="pt-0">
          <Button className="w-full" onClick={handlePlaceOrder} disabled={placing}>
            {placing ? "Placing order…" : "Confirm & place order"}
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}

export default function CheckoutPage() {
  return (
    <ProtectedRoute>
      <CheckoutPageContent />
    </ProtectedRoute>
  );
}

"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { ProtectedRoute } from "@/components/protected-route";
import { orderApi } from "@/lib/order-api";
import { errorMessage } from "@/lib/error-messages";
import { ApiError } from "@/lib/api-client";
import { formatDateTime, formatMoney } from "@/lib/format";
import { Order } from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";

function OrderDetailContent({ orderId }: { orderId: number }) {
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    orderApi
      .get(orderId)
      .then(setOrder)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 404) setNotFound(true);
        else toast.error(errorMessage(err));
      })
      .finally(() => setLoading(false));
  }, [orderId]);

  if (loading) {
    return (
      <div className="mx-auto max-w-2xl space-y-4 px-6 py-10">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (notFound || !order) {
    return (
      <div className="mx-auto max-w-2xl px-6 py-16 text-center">
        <p className="font-heading text-2xl text-foreground">Order not found</p>
        <Link href="/orders" className="mt-2 inline-block text-primary hover:underline">
          Back to your orders
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-6 py-10">
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-3xl font-semibold text-foreground">Order #{order.id}</h1>
        <Badge variant="outline">{order.status}</Badge>
      </div>
      <p className="mt-1 text-sm text-muted-foreground">Placed {formatDateTime(order.createdAt)}</p>

      <Card className="mt-8">
        <CardHeader>
          <CardTitle>Items</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {order.items.map((item, i) => (
            <div key={item.id}>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-foreground">
                    {item.cakeName} <span className="text-muted-foreground">× {item.quantity}</span>
                  </p>
                  <Link href={`/catalog/${item.cakeId}`} className="text-sm text-primary hover:underline">
                    Rate this cake
                  </Link>
                </div>
                <p className="text-foreground">{formatMoney(item.lineTotal)}</p>
              </div>
              {i < order.items.length - 1 && <Separator className="mt-4" />}
            </div>
          ))}
          <Separator />
          <div className="flex items-center justify-between font-medium">
            <span className="text-foreground">Total</span>
            <span className="font-heading text-lg text-primary">{formatMoney(order.totalAmount)}</span>
          </div>
        </CardContent>
      </Card>

      <Link href="/orders">
        <Button variant="outline" className="mt-6">
          Back to your orders
        </Button>
      </Link>
    </div>
  );
}

export function OrderDetailClient({ orderId }: { orderId: number }) {
  return (
    <ProtectedRoute>
      <OrderDetailContent orderId={orderId} />
    </ProtectedRoute>
  );
}

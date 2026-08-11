"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { ProtectedRoute } from "@/components/protected-route";
import { orderApi } from "@/lib/order-api";
import { errorMessage } from "@/lib/error-messages";
import { Order } from "@/lib/types";
import { OrderCard } from "@/components/order-card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

function OrdersPageContent() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    orderApi
      .list()
      .then(setOrders)
      .catch((err) => toast.error(errorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="mx-auto max-w-3xl px-6 py-10">
      <h1 className="font-heading text-3xl font-semibold text-foreground">Your Orders</h1>

      {loading ? (
        <div className="mt-8 space-y-4">
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-20 w-full" />
        </div>
      ) : orders.length === 0 ? (
        <div className="mt-8 text-center">
          <p className="text-muted-foreground">You haven&apos;t placed any orders yet.</p>
          <Link href="/catalog">
            <Button className="mt-4">Browse cakes</Button>
          </Link>
        </div>
      ) : (
        <div className="mt-8 space-y-4">
          {orders.map((order) => (
            <OrderCard key={order.id} order={order} />
          ))}
        </div>
      )}
    </div>
  );
}

export default function OrdersPage() {
  return (
    <ProtectedRoute>
      <OrdersPageContent />
    </ProtectedRoute>
  );
}

import Link from "next/link";
import { Order } from "@/lib/types";
import { formatDateTime, formatMoney } from "@/lib/format";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

export function OrderCard({ order }: { order: Order }) {
  const itemCount = order.items.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <Link href={`/orders/${order.id}`}>
      <Card className="transition-shadow hover:shadow-md">
        <CardContent className="flex items-center justify-between gap-4">
          <div>
            <p className="font-medium text-foreground">Order #{order.id}</p>
            <p className="text-sm text-muted-foreground">
              {formatDateTime(order.createdAt)} · {itemCount} item{itemCount === 1 ? "" : "s"}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <Badge variant="outline">{order.status}</Badge>
            <p className="font-medium text-primary">{formatMoney(order.totalAmount)}</p>
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}

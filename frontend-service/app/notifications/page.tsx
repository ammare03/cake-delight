"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { ProtectedRoute } from "@/components/protected-route";
import { notificationApi } from "@/lib/notification-api";
import { errorMessage } from "@/lib/error-messages";
import { formatDateTime } from "@/lib/format";
import { Notification } from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

function prettyPayload(payload: string): string {
  try {
    return JSON.stringify(JSON.parse(payload), null, 2);
  } catch {
    return payload;
  }
}

function NotificationsPageContent() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    notificationApi
      .list()
      .then(setNotifications)
      .catch((err) => toast.error(errorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="mx-auto max-w-2xl px-6 py-10">
      <div>
        <h1 className="font-heading text-3xl font-semibold text-foreground">Notifications</h1>
        <p className="mt-1 text-muted-foreground">
          A record of the order-confirmation emails sent for your account.
        </p>
      </div>

      {loading ? (
        <div className="mt-8 space-y-4">
          <Skeleton className="h-28 w-full" />
          <Skeleton className="h-28 w-full" />
        </div>
      ) : notifications.length === 0 ? (
        <p className="mt-8 text-center text-muted-foreground">No notifications yet — place an order to get one.</p>
      ) : (
        <div className="mt-8 space-y-4">
          {notifications.map((n) => (
            <Card key={n.id}>
              <CardHeader className="flex-row items-center justify-between space-y-0">
                <div>
                  <p className="font-medium text-foreground">Order #{n.orderId}</p>
                  <p className="text-xs text-muted-foreground">{formatDateTime(n.createdAt)}</p>
                </div>
                <div className="flex gap-2">
                  <Badge variant="outline">{n.channel}</Badge>
                  <Badge variant={n.status === "SENT" ? "default" : "destructive"}>{n.status}</Badge>
                </div>
              </CardHeader>
              <CardContent>
                <pre className="overflow-x-auto rounded-md bg-muted p-3 text-xs text-muted-foreground">
                  {prettyPayload(n.payload)}
                </pre>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

export default function NotificationsPage() {
  return (
    <ProtectedRoute>
      <NotificationsPageContent />
    </ProtectedRoute>
  );
}

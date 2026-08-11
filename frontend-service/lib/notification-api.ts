import { apiFetch } from "@/lib/api-client";
import { Notification } from "@/lib/types";

export const notificationApi = {
  list: () => apiFetch<Notification[]>("/notifications"),
};

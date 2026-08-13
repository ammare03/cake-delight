import { apiFetch } from "@/lib/api-client";
import { AddBasketItemRequest, Basket, Order, UpdateBasketItemRequest } from "@/lib/types";

export const basketApi = {
  get: () => apiFetch<Basket>("/orders/basket"),

  addItem: (request: AddBasketItemRequest) =>
    apiFetch<Basket>("/orders/basket/items", { method: "POST", body: request }),

  updateItem: (itemId: number, request: UpdateBasketItemRequest) =>
    apiFetch<Basket>(`/orders/basket/items/${itemId}`, { method: "PUT", body: request }),

  removeItem: (itemId: number) =>
    apiFetch<void>(`/orders/basket/items/${itemId}`, { method: "DELETE" }),
};

export const orderApi = {
  checkout: () => apiFetch<Order>("/orders/checkout", { method: "POST" }),

  list: () => apiFetch<Order[]>("/orders"),

  get: (id: number) => apiFetch<Order>(`/orders/${id}`),
};

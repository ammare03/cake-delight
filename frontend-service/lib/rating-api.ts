import { apiFetch } from "@/lib/api-client";
import { CreateRatingRequest, Rating, RatingSummary } from "@/lib/types";

export const ratingApi = {
  submit: (request: CreateRatingRequest) =>
    apiFetch<Rating>("/ratings", { method: "POST", body: request }),

  listForCake: (cakeId: number) => apiFetch<Rating[]>(`/ratings/cakes/${cakeId}`),

  summaryForCake: (cakeId: number) => apiFetch<RatingSummary>(`/ratings/cakes/${cakeId}/summary`),
};

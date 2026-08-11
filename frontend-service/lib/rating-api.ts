import { apiFetch } from "@/lib/api-client";
import { CreateRatingRequest, Rating, RatingSummary } from "@/lib/types";

// Every /ratings/** route requires auth at the gateway — including the two
// GETs below (see the "endpoint contract" note in the Phase 5 plan; the
// gateway config has no public entry for /api/ratings/**, unlike catalog).
export const ratingApi = {
  submit: (request: CreateRatingRequest) =>
    apiFetch<Rating>("/ratings", { method: "POST", body: request }),

  listForCake: (cakeId: number) => apiFetch<Rating[]>(`/ratings/cakes/${cakeId}`),

  summaryForCake: (cakeId: number) => apiFetch<RatingSummary>(`/ratings/cakes/${cakeId}/summary`),
};

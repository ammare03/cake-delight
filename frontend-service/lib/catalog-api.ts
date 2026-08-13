import { apiFetch } from "@/lib/api-client";
import { Cake, CakeFilters } from "@/lib/types";

export const catalogApi = {
  list: (filters: CakeFilters = {}) => {
    const params = new URLSearchParams();
    if (filters.name) params.set("name", filters.name);
    if (filters.category) params.set("category", filters.category);
    if (filters.minPrice) params.set("minPrice", filters.minPrice);
    if (filters.maxPrice) params.set("maxPrice", filters.maxPrice);
    const query = params.toString();
    return apiFetch<Cake[]>(`/catalog/cakes${query ? `?${query}` : ""}`);
  },

  get: (id: number) => apiFetch<Cake>(`/catalog/cakes/${id}`),
};

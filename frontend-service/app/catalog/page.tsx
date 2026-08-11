"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { catalogApi } from "@/lib/catalog-api";
import { errorMessage } from "@/lib/error-messages";
import { Cake, CakeFilters } from "@/lib/types";
import { CakeCard } from "@/components/cake-card";
import { CakeFiltersBar } from "@/components/cake-filters";
import { Skeleton } from "@/components/ui/skeleton";

export default function CatalogPage() {
  const [cakes, setCakes] = useState<Cake[]>([]);
  const [loading, setLoading] = useState(true);

  async function load(filters: CakeFilters = {}) {
    setLoading(true);
    try {
      setCakes(await catalogApi.list(filters));
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="mx-auto max-w-6xl space-y-8 px-6 py-10">
      <div>
        <h1 className="font-heading text-3xl font-semibold text-foreground">Our Cakes</h1>
        <p className="mt-1 text-muted-foreground">Browse the full menu, or narrow it down below.</p>
      </div>

      <CakeFiltersBar onApply={load} />

      {loading ? (
        <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 lg:grid-cols-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="aspect-[3/4] w-full" />
          ))}
        </div>
      ) : cakes.length === 0 ? (
        <p className="py-12 text-center text-muted-foreground">No cakes match those filters.</p>
      ) : (
        <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 lg:grid-cols-4">
          {cakes.map((cake) => (
            <CakeCard key={cake.id} cake={cake} />
          ))}
        </div>
      )}
    </div>
  );
}

"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { catalogApi } from "@/lib/catalog-api";
import { Cake } from "@/lib/types";
import { CakeCard } from "@/components/cake-card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

export default function HomePage() {
  const [cakes, setCakes] = useState<Cake[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    catalogApi
      .list()
      .then((all) => setCakes(all.slice(0, 4)))
      .catch(() => setCakes([]))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <section className="border-b border-border bg-secondary/40">
        <div className="mx-auto max-w-6xl px-6 py-24 text-center">
          <h1 className="font-heading text-4xl font-semibold text-foreground sm:text-5xl">
            Cakes made for the moment.
          </h1>
          <p className="mx-auto mt-4 max-w-xl text-muted-foreground">
            Order handmade cakes online — browse the menu, add to your basket, and check out in a few clicks.
          </p>
          <Link href="/catalog">
            <Button size="lg" className="mt-8">
              Browse the menu
            </Button>
          </Link>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-6 py-16">
        <h2 className="font-heading text-2xl font-semibold text-foreground">A few favorites</h2>
        <div className="mt-6 grid grid-cols-2 gap-6 sm:grid-cols-4">
          {loading
            ? Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="aspect-[3/4] w-full" />)
            : cakes.map((cake) => <CakeCard key={cake.id} cake={cake} />)}
        </div>
      </section>
    </div>
  );
}

"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { CakeSlice, Minus, Plus } from "lucide-react";
import { useAuth } from "@/context/auth-context";
import { useBasket } from "@/context/basket-context";
import { catalogApi } from "@/lib/catalog-api";
import { basketApi } from "@/lib/order-api";
import { ratingApi } from "@/lib/rating-api";
import { ApiError } from "@/lib/api-client";
import { errorMessage } from "@/lib/error-messages";
import { formatMoney } from "@/lib/format";
import { placeholderClass } from "@/lib/category-style";
import { Cake, Rating, RatingSummary } from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { RatingStars } from "@/components/rating-stars";
import { RatingForm } from "@/components/rating-form";
import { RatingList } from "@/components/rating-list";

export function CakeDetailClient({ cakeId }: { cakeId: number }) {
  const { user } = useAuth();
  const { refresh: refreshBasket } = useBasket();

  const [cake, setCake] = useState<Cake | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  const [quantity, setQuantity] = useState(1);
  const [addingToBasket, setAddingToBasket] = useState(false);

  const [summary, setSummary] = useState<RatingSummary | null>(null);
  const [ratings, setRatings] = useState<Rating[]>([]);
  const [ratingsLoading, setRatingsLoading] = useState(false);

  useEffect(() => {
    catalogApi
      .get(cakeId)
      .then(setCake)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 404) setNotFound(true);
        else toast.error(errorMessage(err));
      })
      .finally(() => setLoading(false));
  }, [cakeId]);

  useEffect(() => {
    if (!user) return;
    setRatingsLoading(true);
    Promise.all([ratingApi.summaryForCake(cakeId), ratingApi.listForCake(cakeId)])
      .then(([summaryRes, ratingsRes]) => {
        setSummary(summaryRes);
        setRatings(ratingsRes);
      })
      .catch((err) => toast.error(errorMessage(err)))
      .finally(() => setRatingsLoading(false));
  }, [cakeId, user]);

  async function handleAddToBasket() {
    setAddingToBasket(true);
    try {
      await basketApi.addItem({ cakeId, quantity });
      toast.success("Added to basket");
      setQuantity(1);
      await refreshBasket();
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setAddingToBasket(false);
    }
  }

  function handleRatingSubmitted(rating: Rating) {
    setRatings((prev) => [rating, ...prev]);
    setSummary((prev) => ({
      averageRating: prev ? (prev.averageRating * prev.totalRatings + rating.ratingValue) / (prev.totalRatings + 1) : rating.ratingValue,
      totalRatings: (prev?.totalRatings ?? 0) + 1,
    }));
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-4xl space-y-6 px-6 py-10">
        <Skeleton className="aspect-video w-full" />
        <Skeleton className="h-8 w-1/2" />
        <Skeleton className="h-24 w-full" />
      </div>
    );
  }

  if (notFound || !cake) {
    return (
      <div className="mx-auto max-w-4xl px-6 py-16 text-center">
        <p className="font-heading text-2xl text-foreground">Cake not found</p>
        <Link href="/catalog" className="mt-2 inline-block text-primary hover:underline">
          Back to catalog
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl px-6 py-10">
      <div className="grid gap-8 sm:grid-cols-2">
        <div className={`flex aspect-square items-center justify-center rounded-xl ${placeholderClass(cake.category)}`}>
          {cake.imageUrl ? (
            <img src={cake.imageUrl} alt={cake.name} className="h-full w-full rounded-xl object-cover" />
          ) : (
            <CakeSlice className="size-16 text-foreground/25" aria-hidden />
          )}
        </div>

        <div className="space-y-4">
          <div>
            <Badge variant="secondary">{cake.category}</Badge>
            <h1 className="mt-2 font-heading text-3xl font-semibold text-foreground">{cake.name}</h1>
            {user && summary && summary.totalRatings > 0 && (
              <div className="mt-1 flex items-center gap-2">
                <RatingStars value={summary.averageRating} />
                <span className="text-sm text-muted-foreground">
                  {summary.averageRating.toFixed(1)} ({summary.totalRatings} review{summary.totalRatings === 1 ? "" : "s"})
                </span>
              </div>
            )}
          </div>

          <p className="text-muted-foreground">{cake.description}</p>
          <p className="font-heading text-2xl text-primary">{formatMoney(cake.price)}</p>

          {!cake.available ? (
            <Badge variant="secondary">Currently sold out</Badge>
          ) : user ? (
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-2">
                <Button variant="outline" size="icon" disabled={quantity <= 1} onClick={() => setQuantity((q) => q - 1)}>
                  <Minus className="size-3.5" />
                </Button>
                <span className="w-6 text-center tabular-nums">{quantity}</span>
                <Button variant="outline" size="icon" onClick={() => setQuantity((q) => q + 1)}>
                  <Plus className="size-3.5" />
                </Button>
              </div>
              <Button onClick={handleAddToBasket} disabled={addingToBasket}>
                {addingToBasket ? "Adding…" : "Add to basket"}
              </Button>
            </div>
          ) : (
            <Link href="/login">
              <Button>Log in to order</Button>
            </Link>
          )}
        </div>
      </div>

      <Separator className="my-10" />

      <section className="max-w-2xl">
        <h2 className="font-heading text-xl font-semibold text-foreground">Reviews</h2>
        {!user ? (
          <p className="mt-2 text-sm text-muted-foreground">
            <Link href="/login" className="text-primary hover:underline">
              Log in
            </Link>{" "}
            to see reviews and rate this cake.
          </p>
        ) : ratingsLoading ? (
          <div className="mt-4 space-y-2">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </div>
        ) : (
          <div className="mt-4 space-y-8">
            <RatingForm cakeId={cakeId} onSubmitted={handleRatingSubmitted} />
            <RatingList ratings={ratings} />
          </div>
        )}
      </section>
    </div>
  );
}

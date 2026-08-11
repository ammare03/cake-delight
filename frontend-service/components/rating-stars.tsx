"use client";

import { Star } from "lucide-react";
import { cn } from "@/lib/utils";

interface RatingStarsProps {
  /** 0-5, fractional allowed for an average (rendered rounded to the nearest whole star). */
  value: number;
  onChange?: (value: number) => void;
  size?: "sm" | "md";
}

export function RatingStars({ value, onChange, size = "sm" }: RatingStarsProps) {
  const interactive = !!onChange;
  const rounded = Math.round(value);
  const starSize = size === "sm" ? "size-4" : "size-6";

  return (
    <div className="flex items-center gap-0.5" role={interactive ? "radiogroup" : undefined} aria-label="Rating">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          disabled={!interactive}
          onClick={() => onChange?.(n)}
          className={cn(interactive && "cursor-pointer transition-transform hover:scale-110")}
          aria-label={`${n} star${n > 1 ? "s" : ""}`}
        >
          <Star
            className={cn(starSize, n <= rounded ? "fill-primary text-primary" : "fill-none text-muted-foreground")}
          />
        </button>
      ))}
    </div>
  );
}

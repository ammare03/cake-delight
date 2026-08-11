"use client";

import { useState } from "react";
import { toast } from "sonner";
import { ratingApi } from "@/lib/rating-api";
import { ApiError } from "@/lib/api-client";
import { errorMessage } from "@/lib/error-messages";
import { Rating } from "@/lib/types";
import { RatingStars } from "@/components/rating-stars";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";

// CAKE_NOT_PURCHASED and DUPLICATE_RATING are both expected outcomes here,
// not failures — there's no endpoint to check either condition up front
// (rating-service only verifies on submit), so the form just tries and
// swaps itself for an explanatory message rather than showing an error
// toast for a state the user didn't do anything wrong to reach.
const BLOCKED_MESSAGES: Record<string, string> = {
  CAKE_NOT_PURCHASED: "You can only review cakes you've purchased.",
  DUPLICATE_RATING: "You've already reviewed this cake — thank you!",
};

export function RatingForm({ cakeId, onSubmitted }: { cakeId: number; onSubmitted: (rating: Rating) => void }) {
  const [value, setValue] = useState(0);
  const [reviewText, setReviewText] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [blockedReason, setBlockedReason] = useState<string | null>(null);

  if (blockedReason) {
    return <p className="text-sm text-muted-foreground">{blockedReason}</p>;
  }

  async function handleSubmit() {
    if (value === 0) {
      toast.error("Pick a star rating first.");
      return;
    }
    setSubmitting(true);
    try {
      const rating = await ratingApi.submit({ cakeId, ratingValue: value, reviewText: reviewText || undefined });
      toast.success("Thanks for your review!");
      setValue(0);
      setReviewText("");
      onSubmitted(rating);
    } catch (err) {
      if (err instanceof ApiError && BLOCKED_MESSAGES[err.code]) {
        setBlockedReason(BLOCKED_MESSAGES[err.code]);
      } else {
        toast.error(errorMessage(err));
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="space-y-3">
      <RatingStars value={value} onChange={setValue} size="md" />
      <Textarea
        placeholder="Share a few words about this cake (optional)"
        value={reviewText}
        onChange={(e) => setReviewText(e.target.value)}
        maxLength={2000}
      />
      <Button onClick={handleSubmit} disabled={submitting}>
        {submitting ? "Submitting…" : "Submit review"}
      </Button>
    </div>
  );
}

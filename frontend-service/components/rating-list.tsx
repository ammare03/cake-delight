import { Rating } from "@/lib/types";
import { formatDate } from "@/lib/format";
import { RatingStars } from "@/components/rating-stars";
import { Separator } from "@/components/ui/separator";

export function RatingList({ ratings }: { ratings: Rating[] }) {
  if (ratings.length === 0) {
    return <p className="text-sm text-muted-foreground">No reviews yet — be the first to rate this cake.</p>;
  }

  return (
    <ul className="space-y-4">
      {ratings.map((rating, i) => (
        <li key={rating.id}>
          <div className="flex items-center justify-between">
            <RatingStars value={rating.ratingValue} />
            <span className="text-xs text-muted-foreground">{formatDate(rating.createdAt)}</span>
          </div>
          {rating.reviewText && <p className="mt-1 text-sm text-foreground">{rating.reviewText}</p>}
          {i < ratings.length - 1 && <Separator className="mt-4" />}
        </li>
      ))}
    </ul>
  );
}

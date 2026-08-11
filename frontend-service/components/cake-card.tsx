import Link from "next/link";
import { CakeSlice } from "lucide-react";
import { Cake } from "@/lib/types";
import { formatMoney } from "@/lib/format";
import { placeholderClass } from "@/lib/category-style";
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

export function CakeCard({ cake }: { cake: Cake }) {
  return (
    <Link href={`/catalog/${cake.id}`}>
      <Card className="h-full overflow-hidden py-0 transition-shadow hover:shadow-md">
        <div className={`relative flex aspect-square w-full items-center justify-center ${placeholderClass(cake.category)}`}>
          {cake.imageUrl ? (
            <img src={cake.imageUrl} alt={cake.name} className="h-full w-full object-cover" />
          ) : (
            <CakeSlice className="size-10 text-foreground/25" aria-hidden />
          )}
          {!cake.available && (
            <Badge variant="secondary" className="absolute right-2 top-2">
              Sold out
            </Badge>
          )}
        </div>
        <CardContent className="pb-0">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">{cake.category}</p>
          <h3 className="font-heading text-lg font-medium text-foreground">{cake.name}</h3>
        </CardContent>
        <CardFooter className="pb-4">
          <p className="font-medium text-primary">{formatMoney(cake.price)}</p>
        </CardFooter>
      </Card>
    </Link>
  );
}

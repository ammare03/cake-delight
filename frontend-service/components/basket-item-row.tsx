import { Minus, Plus, Trash2 } from "lucide-react";
import { BasketItem } from "@/lib/types";
import { formatMoney } from "@/lib/format";
import { Button } from "@/components/ui/button";

interface BasketItemRowProps {
  item: BasketItem;
  onQuantityChange: (itemId: number, quantity: number) => void;
  onRemove: (itemId: number) => void;
  disabled?: boolean;
}

export function BasketItemRow({ item, onQuantityChange, onRemove, disabled }: BasketItemRowProps) {
  return (
    <div className="flex items-center justify-between gap-4 py-4">
      <div className="min-w-0 flex-1">
        <p className="truncate font-medium text-foreground">{item.cakeName}</p>
        <p className="text-sm text-muted-foreground">{formatMoney(item.unitPrice)} each</p>
      </div>

      <div className="flex items-center gap-2">
        <Button
          type="button"
          variant="outline"
          size="icon"
          disabled={disabled || item.quantity <= 1}
          onClick={() => onQuantityChange(item.id, item.quantity - 1)}
        >
          <Minus className="size-3.5" />
        </Button>
        <span className="w-6 text-center tabular-nums">{item.quantity}</span>
        <Button
          type="button"
          variant="outline"
          size="icon"
          disabled={disabled}
          onClick={() => onQuantityChange(item.id, item.quantity + 1)}
        >
          <Plus className="size-3.5" />
        </Button>
      </div>

      <p className="w-24 text-right font-medium text-foreground">{formatMoney(item.lineTotal)}</p>

      <Button
        type="button"
        variant="ghost"
        size="icon"
        disabled={disabled}
        onClick={() => onRemove(item.id)}
        aria-label="Remove item"
      >
        <Trash2 className="size-4 text-muted-foreground" />
      </Button>
    </div>
  );
}

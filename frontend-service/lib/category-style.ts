const PALETTE = ["bg-accent", "bg-secondary", "bg-primary/10", "bg-muted"] as const;

export function placeholderClass(category: string): string {
  let hash = 0;
  for (let i = 0; i < category.length; i++) {
    hash = (hash * 31 + category.charCodeAt(i)) >>> 0;
  }
  return PALETTE[hash % PALETTE.length];
}

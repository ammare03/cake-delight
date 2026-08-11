/**
 * Seed data ships with no product photography (catalog-service/data.sql —
 * every row's image_url is NULL), and hotlinking third-party stock photos
 * would make the demo depend on an external host staying up. Instead, every
 * cake without a real imageUrl gets a crafted placeholder: a pastel tile
 * (picked deterministically from the theme's own palette, so it never
 * clashes) with a cake-slice mark. Real photography still works — CakeCard
 * just renders `imageUrl` directly when a cake has one.
 */
const PALETTE = ["bg-accent", "bg-secondary", "bg-primary/10", "bg-muted"] as const;

export function placeholderClass(category: string): string {
  let hash = 0;
  for (let i = 0; i < category.length; i++) {
    hash = (hash * 31 + category.charCodeAt(i)) >>> 0;
  }
  return PALETTE[hash % PALETTE.length];
}

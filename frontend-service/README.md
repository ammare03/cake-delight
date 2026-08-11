# frontend-service

Cake Delight's Phase 5 frontend — Next.js (App Router, TypeScript), Tailwind CSS v4, shadcn/ui. See the root [`README.md`](../README.md#frontend-frontend-service) for the full picture (ports, startup order, CORS/auth notes) — this file only covers running this module on its own.

## Run it

Needs the rest of the stack up first — see the root README's **Startup order**.

```bash
npm install
npm run dev
```

Serves on `http://localhost:3000`. Copy `.env.local.example` to `.env.local` if you need to point at a gateway that isn't `http://localhost:9090/api`.

## Layout

- `app/` — one route per page (App Router). Dynamic routes (`catalog/[id]`, `orders/[id]`) are a thin server component that awaits `params`, handing off to a co-located `*-client.tsx` for the actual UI/data-fetching.
- `components/` — shared UI: shadcn primitives in `ui/`, feature components (cake card, rating form, basket row, …) alongside them.
- `context/` — `AuthProvider` (session in `localStorage`) and `BasketProvider` (current basket, shared between the nav badge and the basket page).
- `lib/` — `api-client.ts` (the one place that calls the gateway) plus a thin, typed wrapper per backend service (`catalog-api.ts`, `order-api.ts`, …) and `types.ts` (DTOs mirrored 1:1 from each service's Java records).

## Conventions

- Every request goes through `api-gateway` (`NEXT_PUBLIC_API_BASE_URL`) — never a business service directly.
- No admin UI — see CLAUDE.md §12; this only covers the customer-facing flow (browse, basket, checkout, orders, ratings, notifications).
- `lib/error-messages.ts` turns a thrown `ApiError` into toast-safe copy; form-level validation errors (`ApiError.fieldErrors`) render inline instead.

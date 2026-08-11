# frontend-service — Phase 6. Multi-stage Next.js build producing a
# standalone runtime (next.config.ts: output: "standalone"), so the final
# image ships only the built server and the node_modules it actually uses —
# not the full dev toolchain.
#
# NEXT_PUBLIC_API_BASE_URL is baked in at BUILD time, not runtime — this is
# a Next.js constraint (NEXT_PUBLIC_* vars are inlined into the client
# bundle during `next build`), not a Cake Delight design choice. The build
# ARG below defaults to the gateway's Docker Compose address; Kubernetes
# builds pass a different value at `docker build` time to match wherever
# the gateway is actually reachable (see the root README's Kubernetes
# section for the exact command).
ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:9090/api

# ---- deps ----
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
# `npm install`, not `npm ci`: the lockfile was generated with a newer local
# npm (11.x) than node:20-alpine ships (10.x), and npm's dependency
# resolution for a couple of transitive packages (picomatch) differs enough
# between those major versions that `npm ci`'s stricter exact-match check
# rejects an otherwise-valid tree. `npm install` resolves against the same
# package.json/package-lock.json and is still deterministic enough for a
# reproducible image at this project's scope.
RUN npm install

# ---- build ----
FROM node:20-alpine AS build
WORKDIR /app
ARG NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ---- runtime ----
FROM node:20-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
RUN addgroup -S nextjs && adduser -S nextjs -G nextjs

# .next/standalone already contains a minimal server.js + only the
# node_modules it needs — static assets and public/ are copied alongside it
# per Next.js's documented standalone-output layout.
#
# The extra /app segment below is not a typo: next.config.ts sets
# `turbopack.root` one directory above frontend-service/ (silences a
# workspace-root inference warning — see the comment there), and Next
# mirrors that relative path inside .next/standalone. Since this stage's
# WORKDIR is also /app, standalone output lands at
# .next/standalone/app/server.js, not .next/standalone/server.js directly.
# Copying that inner app/ directory's *contents* here is what makes
# `node server.js` below find it at the expected path.
COPY --from=build /app/.next/standalone/app ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public

USER nextjs:nextjs
EXPOSE 3000
CMD ["node", "server.js"]

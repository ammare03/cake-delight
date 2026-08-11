import type { NextConfig } from "next";
import path from "path";

const nextConfig: NextConfig = {
  // Silences Turbopack's workspace-root inference warning: it finds an
  // unrelated package-lock.json further up the filesystem (outside this
  // monorepo entirely, e.g. from other local projects) and guesses that's
  // the root. This repo's actual root is one level up from
  // frontend-service/ — the same directory every other Cake Delight
  // service's pom.xml lives in.
  turbopack: {
    root: path.join(__dirname, ".."),
  },
  // Phase 6 — produces a self-contained .next/standalone build (a minimal
  // server.js plus only the node_modules actually used) so the Docker
  // runtime stage doesn't need to ship the full node_modules tree or run
  // `next start` against the source. No effect on `npm run dev`.
  output: "standalone",
};

export default nextConfig;

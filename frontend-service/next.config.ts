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
};

export default nextConfig;

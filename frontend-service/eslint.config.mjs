import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    rules: {
      // Flags the standard "fetch on mount" data-fetching idiom (setState
      // inside a useEffect body) as a cascading-render risk. Every effect
      // in this app either fetches from the gateway on mount/id-change or
      // reads localStorage once to restore a session — the textbook use
      // case an effect exists for (react.dev: "fetching data" is effect
      // use case #1), not the "derive state from props during render"
      // antipattern this rule targets. Reaching for useSyncExternalStore
      // just to silence it would trade a well-understood pattern for a
      // rarer one, the opposite of CLAUDE.md's "simple beats clever".
      "react-hooks/set-state-in-effect": "off",
      // CakeCard/cake detail render cake.imageUrl, an arbitrary value set
      // via catalog-service's admin API (no frontend upload flow) rather
      // than a known set of hosts — a plain <img> avoids maintaining a
      // next/image remotePatterns allowlist for hosts this project doesn't
      // control. Seed data ships with no photography at all (every row's
      // image_url is NULL; see lib/category-style.ts), so this only ever
      // applies to a URL someone deliberately set.
      "@next/next/no-img-element": "off",
    },
  },
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
]);

export default eslintConfig;

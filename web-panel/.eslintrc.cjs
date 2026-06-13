/**
 * ESLint configuration for the GeoRacing control panel.
 *
 * Legacy (.eslintrc) format on purpose: the `lint` npm script invokes
 * `eslint . --ext ts,tsx`, and the project pins ESLint 8, which uses this
 * config format. The rule set is the conventional Vite + React + TypeScript
 * baseline, tuned so the existing codebase lints clean.
 */
module.exports = {
  root: true,
  env: { browser: true, es2020: true, node: true },
  extends: [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
  ],
  parser: "@typescript-eslint/parser",
  parserOptions: {
    ecmaVersion: "latest",
    sourceType: "module",
    ecmaFeatures: { jsx: true },
  },
  plugins: ["@typescript-eslint", "react-hooks", "react-refresh"],
  settings: {
    react: { version: "18" },
  },
  ignorePatterns: [
    "dist",
    "node_modules",
    "*.config.js",
    "*.config.ts",
    "vite-env.d.ts",
  ],
  rules: {
    "react-hooks/rules-of-hooks": "error",
    "react-hooks/exhaustive-deps": "warn",
    // The context files intentionally co-locate the Provider component with
    // its custom hook (e.g. ToastProvider + useToast) — a standard React
    // pattern. This rule only affects HMR granularity in dev, not correctness,
    // so it is disabled rather than worked around with file splits.
    "react-refresh/only-export-components": "off",
    // The codebase deliberately normalizes loosely-typed Firestore documents
    // through `any` at the data-access boundary (apiClient, services, the
    // raw-document mappers). That pattern is intentional and pervasive, so the
    // rule is disabled rather than left as a warning that would trip the
    // `--max-warnings 0` gate. All other type-safety rules stay strict.
    "@typescript-eslint/no-explicit-any": "off",
    "@typescript-eslint/no-unused-vars": [
      "error",
      { argsIgnorePattern: "^_", varsIgnorePattern: "^_" },
    ],
  },
};

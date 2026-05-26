// ══════════════════════════════════════════════════════════════════════════════
// playwright.config.ts — Playwright Test Configuration
// ══════════════════════════════════════════════════════════════════════════════
//
// This file configures how Playwright runs your tests.
// Playwright reads it automatically when you run: npx playwright test
//
// WHY A SEPARATE CONFIG FILE?
//   Without configuration, you'd have to pass every setting on the command line.
//   Having it in a file means running just 'npx playwright test' applies all
//   settings (baseURL, reporters, parallelism, retries) automatically.
//
// HOW TO RUN TESTS:
//   npx playwright test            ← run all tests
//   npm run test                   ← same (uses the "test" script in package.json)
//   npm run test:api               ← run only tests in tests/api/
//   npm run report                 ← open the HTML report in a browser
// ══════════════════════════════════════════════════════════════════════════════

import { defineConfig } from '@playwright/test';
// defineConfig: a TypeScript helper function from Playwright.
// It provides type-checking and autocomplete for the config object.
// Without it you'd write a plain object with no type safety.

// export default: makes this configuration available to Playwright's test runner.
// 'default' means it's the main (only) export from this file.
export default defineConfig({

  // ── testDir ──────────────────────────────────────────────────────────────────
  // The root folder where Playwright looks for test files.
  // All files matching the pattern **/*.spec.ts inside this folder are discovered.
  // './tests' means: the 'tests' subfolder relative to playwright.config.ts
  testDir: './tests',

  // ── fullyParallel ─────────────────────────────────────────────────────────────
  // true  → all spec files run in parallel (simultaneously, in separate workers)
  // false → spec files run one after another (sequential)
  //
  // We set true here because status.spec.ts and books.spec.ts are completely
  // independent — they don't share state and can safely run at the same time.
  //
  // EXCEPTION: orders.spec.ts overrides this with test.describe.serial()
  // so the tests inside it always run one after another (they depend on each other).
  fullyParallel: true,

  // ── forbidOnly ────────────────────────────────────────────────────────────────
  // Prevents accidentally committing 'test.only()' to CI.
  //
  // 'test.only(...)' runs ONLY that one test and skips everything else.
  // It's useful locally for debugging, but if you forget to remove it before
  // pushing to Git, your CI pipeline runs only one test — giving false confidence.
  //
  // process.env.CI: an environment variable that is set to 'true' in most
  // CI/CD systems (GitHub Actions, Jenkins, etc.) but is undefined locally.
  // !!process.env.CI converts it to a boolean: true in CI, false locally.
  // So: forbidOnly=true only in CI, false locally (where .only is fine for debugging).
  forbidOnly: !!process.env.CI,

  // ── retries ───────────────────────────────────────────────────────────────────
  // How many times to RETRY a failed test before marking it as truly failed.
  // process.env.CI ? 1 : 0 means:
  //   - In CI: retry once (helps with flaky network failures in CI environments)
  //   - Locally: no retries (fail immediately so you see the error right away)
  //
  // The ternary operator: condition ? valueIfTrue : valueIfFalse
  // It's a compact if/else: if (process.env.CI) { retries = 1 } else { retries = 0 }
  retries: process.env.CI ? 1 : 0,

  // ── reporter ──────────────────────────────────────────────────────────────────
  // Controls how test results are displayed/saved.
  // You can have MULTIPLE reporters at the same time (hence the array).
  reporter: [
    // 'html': generates a visual report at playwright-report/index.html
    // open: 'never' means don't auto-open the browser after the run.
    // To view it manually: npx playwright show-report  (or npm run report)
    ['html', { open: 'never' }],

    // 'list': prints test results line by line in the terminal as tests run.
    // Output looks like:
    //   ✓ Status API > GET /status — returns HTTP 200 and status OK
    //   ✓ Books API > GET /books — returns a non-empty list of books
    //   ✗ Orders API > POST /orders — create a new order  (FAILED)
    ['list'],
  ],

  // ── use ───────────────────────────────────────────────────────────────────────
  // Default settings applied to ALL tests and requests.
  // Individual tests can OVERRIDE these settings if needed.
  use: {

    // baseURL: the root address of the API being tested.
    // When set, Playwright prepends this to any path starting with '/':
    //   request.get('/status')  →  GET https://simple-books-api.click/status
    //   request.get('/books')   →  GET https://simple-books-api.click/books
    // Without baseURL, every request.get() call would need the full URL.
    baseURL: 'https://simple-books-api.click',

    // extraHTTPHeaders: headers sent with EVERY API request automatically.
    // Tests don't have to add these manually — they're always included.
    extraHTTPHeaders: {
      // Content-Type: application/json tells the server "my request body is JSON"
      'Content-Type': 'application/json',

      // Accept: application/json tells the server "please send me JSON responses"
      // Most APIs send JSON by default, but being explicit is best practice.
      'Accept': 'application/json',
    },
  },

  // ── projects ──────────────────────────────────────────────────────────────────
  // Playwright "projects" let you run tests against different configurations
  // (e.g. different browsers, environments, or viewports).
  // For API testing (no browser needed), we have one project.
  projects: [
    {
      // name: a human-readable label shown in the test report.
      name: 'API Tests',

      // testMatch: a glob pattern that selects which test files belong to this project.
      // '**/api/**/*.spec.ts' means:
      //   **     → any number of nested folders
      //   /api/  → must pass through a folder named "api"
      //   **     → any further subfolders
      //   *.spec.ts → any file ending in .spec.ts
      // This matches: tests/api/status.spec.ts, tests/api/books.spec.ts, etc.
      testMatch: '**/api/**/*.spec.ts',
    },
  ],
});

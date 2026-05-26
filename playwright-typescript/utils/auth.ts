// ══════════════════════════════════════════════════════════════════════════════
// auth.ts — API Client Registration and Token Caching Utility
// ══════════════════════════════════════════════════════════════════════════════
//
// WHAT DOES THIS FILE DO?
//   Handles one task: getting a Bearer access token from the Simple Books API.
//
// WHY A SEPARATE FILE?
//   Multiple spec files need a token (orders.spec.ts needs it, and potentially
//   future spec files too). Without this helper:
//     - Each file would register its own API client → wasted registrations
//     - The same registration logic would be duplicated in each file
//   With this helper:
//     - Registration code lives in ONE place (Single Responsibility)
//     - The token is cached after the first call (no duplicate registrations)
//     - Any spec file can import getAccessToken() with one line
//
// HOW IT WORKS:
//   1st call → cachedToken is null → register API client → cache → return token
//   2nd call → cachedToken is set  → skip registration → return cached token
// ══════════════════════════════════════════════════════════════════════════════

import { APIRequestContext } from '@playwright/test';
// APIRequestContext: the TypeScript TYPE for Playwright's built-in HTTP client.
// It is provided by the { request } fixture in Playwright tests.
// We import the type here so TypeScript can check we're using it correctly.
// (TypeScript imports can be for TYPES only — no runtime code is imported here)

// ── MODULE-LEVEL VARIABLE ──────────────────────────────────────────────────────
// cachedToken stores the token after the first registration.
// 'let' means this variable can be reassigned (unlike 'const').
// Type: string | null means it can hold either a string or null.
//   - null  = not yet registered (initial state)
//   - string = the access token after registration
//
// Module-level means this variable persists as long as the Node.js process runs.
// Every import of auth.ts shares the SAME instance of cachedToken.
let cachedToken: string | null = null;

// ── EXPORTED ASYNC FUNCTION ───────────────────────────────────────────────────

/**
 * getAccessToken() — registers as an API client and returns the Bearer token.
 * On subsequent calls, returns the cached token without re-registering.
 *
 * @param request  Playwright's APIRequestContext (from test.beforeAll or a fixture).
 *                 It provides .post(), .get(), etc. with baseURL already applied.
 * @returns        A Promise that resolves to the access token string.
 *
 * USAGE IN TESTS:
 *   test.beforeAll(async ({ request }) => {
 *     token = await getAccessToken(request);
 *   });
 */
export async function getAccessToken(request: APIRequestContext): Promise<string> {
  // 'export' makes this function importable from other files:
  //   import { getAccessToken } from '../../utils/auth';
  //
  // 'async' means this function is ASYNCHRONOUS — it uses 'await' inside.
  // Async functions always return a Promise. The caller uses 'await' to get
  // the resolved value:
  //   const token = await getAccessToken(request);  // waits for the Promise
  //
  // 'Promise<string>' means: "this async function will eventually produce a string"
  // TypeScript enforces that we return a string (or throw an error).

  // ── CACHE CHECK ──────────────────────────────────────────────────────────────
  // If we already have a token, return it immediately.
  // The 'if (cachedToken)' check is truthy — null is falsy, a non-empty string is truthy.
  // This prevents registering a new API client on every test or every file import.
  if (cachedToken) {
    return cachedToken;
    // TypeScript knows cachedToken is 'string' here (not null) because we just
    // checked it's truthy. This is called TYPE NARROWING.
  }

  // ── GENERATE UNIQUE CLIENT DETAILS ───────────────────────────────────────────
  // Date.now() returns the current Unix timestamp in milliseconds (e.g. 1716400000000)
  // .toString(36) converts it to base-36 (digits 0-9 and letters a-z) for a short string
  // Example: 1716400000000 in base-36 → "lhcvxds"
  // This gives us a unique-enough suffix for each test run without needing a UUID library.
  const uniqueId = Date.now().toString(36);

  // ── REGISTER API CLIENT ───────────────────────────────────────────────────────
  // request.post() sends an HTTP POST to '/api-clients/'.
  // baseURL from playwright.config.ts is prepended: https://simple-books-api.click/api-clients/
  //
  // 'await' pauses execution here until the HTTP response arrives.
  // Without await, 'response' would be a Promise object, not the actual response.
  const response = await request.post('/api-clients/', {
    // data: the request body (automatically serialised to JSON by Playwright)
    data: {
      clientName:  `TestClient_${uniqueId}`,           // e.g. "TestClient_lhcvxds"
      clientEmail: `testclient_${uniqueId}@example.com`, // e.g. "testclient_lhcvxds@example.com"
      // Template literals: backtick strings where ${variable} is replaced with the value.
      // Same concept as f-strings in Python or String.format() in Java.
    },
  });

  // ── ERROR HANDLING ────────────────────────────────────────────────────────────
  // response.ok() returns true if the HTTP status code is 2xx (200–299).
  // !response.ok() is true when registration FAILED (e.g. 409 Conflict, 500 Error).
  //
  // 'if (!response.ok())' — the ! operator negates the boolean.
  // Read as: "if the response is NOT ok..."
  if (!response.ok()) {
    // await response.text() reads the response body as a plain string.
    // We await it to get the actual text content, not a Promise.
    const errorBody = await response.text();

    // 'throw new Error(...)' creates an Error object and throws it.
    // In async functions, throwing rejects the Promise — the caller receives an error.
    // The template literal builds a descriptive error message with the status code and body.
    throw new Error(
      `Failed to register API client. Status: ${response.status()}, Body: ${errorBody}`
    );
    // response.status() returns the HTTP status code as a number (e.g. 409, 500)
  }

  // ── EXTRACT AND CACHE THE TOKEN ───────────────────────────────────────────────
  // await response.json() parses the response body from JSON string to a JavaScript object.
  // We await because reading the body is also an async operation.
  // Example response: { "accessToken": "eyJhbGciOiJIUzI1NiI..." }
  const body = await response.json();

  // body.accessToken reads the "accessToken" property from the parsed object.
  // 'as string' is a TYPE ASSERTION — we tell TypeScript "trust me, this is a string."
  // body.accessToken has type 'any' (from JSON parsing), so we assert the type explicitly.
  cachedToken = body.accessToken as string;

  // Log the first 20 characters to the console for confirmation.
  // cachedToken! — the ! (non-null assertion) tells TypeScript "I know this is not null here"
  // because we just assigned it above. TypeScript cannot always infer that automatically.
  console.log(`[auth] Token obtained: ${cachedToken!.substring(0, 20)}...`);

  // Return the token. The caller receives this string via 'await getAccessToken(request)'.
  return cachedToken!;
}

// ══════════════════════════════════════════════════════════════════════════════
// status.spec.ts — Tests for GET /status (API Health Check)
// ══════════════════════════════════════════════════════════════════════════════
//
// WHAT DOES THIS FILE TEST?
//   The health-check endpoint: GET /status
//   This is the simplest test — it just confirms the API is alive and responding.
//
// ENDPOINT DETAILS:
//   URL      : GET https://simple-books-api.click/status
//   Auth     : NOT required
//   Response : HTTP 200, Content-Type: application/json, Body: { "status": "OK" }
//
// NAMING CONVENTION:
//   Playwright discovers test files ending in .spec.ts or .test.ts
//   (configured in playwright.config.ts via testMatch).
//
// RUN THIS FILE ALONE:
//   npx playwright test tests/api/status.spec.ts
// ══════════════════════════════════════════════════════════════════════════════

import { test, expect } from '@playwright/test';
// 'test' is the function you call to define a test case.
//   Usage: test('description', async ({ request }) => { ... })
//
// 'expect' is Playwright's assertion library.
//   Usage: expect(value).toBe(expected)
//          expect(response.status()).toBe(200)
//
// Both are named imports from '@playwright/test' (the Playwright package).
// The curly braces { test, expect } mean we're importing specific exports,
// not the whole module (that's how ES module imports work in TypeScript).

// ── test.describe() ────────────────────────────────────────────────────────────
// test.describe() creates a NAMED GROUP of related tests.
// It's like a folder for tests — purely organisational.
// The description appears in the HTML report and terminal output.
//
// The second argument is an ARROW FUNCTION that contains the test definitions.
// Arrow function syntax: () => { ... }
// It runs immediately when the spec file is loaded, registering all tests inside.
test.describe('Status API', () => {

  // ── TEST 1 ────────────────────────────────────────────────────────────────────

  // test() defines a single test case.
  // First argument: the test description (shown in reports).
  // Second argument: an async arrow function — the test body.
  //   async ({ request }) means: "this function is async AND I want the 'request' fixture"
  //   { request } is DESTRUCTURING — extracting 'request' from the fixtures object.
  test('GET /status — returns HTTP 200 and status OK', async ({ request }) => {
    // { request } is a Playwright BUILT-IN FIXTURE — you don't need to define it yourself.
    // It provides an HTTP client (APIRequestContext) pre-configured with:
    //   - baseURL from playwright.config.ts
    //   - extraHTTPHeaders from playwright.config.ts (Content-Type, Accept)
    //
    // 'async' and 'await' are required because HTTP requests take time (they're asynchronous).
    // 'await' pauses execution until the HTTP response arrives.
    // Without 'await', the assertions below would run before the response arrives.

    // request.get('/status') sends: GET https://simple-books-api.click/status
    // baseURL from config is prepended to the path '/status' automatically.
    // 'await' waits for the response before continuing.
    const response = await request.get('/status');

    // expect(value).toBe(expected) — the basic assertion in Playwright.
    // It checks that the actual value EXACTLY EQUALS the expected value.
    // If they don't match, Playwright marks the test as FAILED and shows both values.
    //
    // response.status() → returns the HTTP status code as a number (e.g. 200, 404, 500)
    // .toBe(200) → assert it equals 200
    expect(response.status()).toBe(200);

    // response.json() returns a Promise that resolves to the parsed response body.
    // 'await' waits for the JSON to be parsed before we access its properties.
    // Example result: { status: "OK" }
    const body = await response.json();

    // body.status reads the "status" property from the parsed object.
    // .toBe('OK') asserts the value is exactly the string 'OK'.
    // If the API returns { "status": "DOWN" }, this assertion fails.
    expect(body.status).toBe('OK');
  });

  // ── TEST 2 ────────────────────────────────────────────────────────────────────

  test('GET /status — response Content-Type is application/json', async ({ request }) => {

    // Another GET /status request — each test is INDEPENDENT and sets up its own state.
    const response = await request.get('/status');

    // response.headers() returns ALL response headers as a plain object.
    // Example: { 'content-type': 'application/json; charset=utf-8', 'x-powered-by': 'Express' }
    // Note: HTTP header names are case-insensitive, and Playwright lowercases them.
    const contentType = response.headers()['content-type'];
    // ['content-type'] — reads the 'content-type' header value.
    // (Also works with: response.headers()['Content-Type'])

    // .toContain('application/json') — asserts the string CONTAINS the substring.
    // We use toContain (not toBe) because the actual value is often:
    //   "application/json; charset=utf-8"
    // Using toContain means the assertion passes whether or not the charset suffix is present.
    expect(contentType).toContain('application/json');
  });

});
// End of test.describe('Status API', ...)

// ══════════════════════════════════════════════════════════════════════════════
// orders.spec.ts — Full CRUD Lifecycle Tests for /orders
// ══════════════════════════════════════════════════════════════════════════════
//
// WHAT DOES THIS FILE TEST?
//   The complete Create → Read → Update → Delete cycle plus error handling.
//
// WHY test.describe.serial?
//   The tests are DEPENDENT on each other:
//     Step 1 (Create) produces an orderId
//     Step 2 (Read All) verifies the order appears in the list
//     Step 3 (Read One) checks the specific order by its ID
//     Step 4 (Update) changes the customer name
//     Step 5 (Verify Update) confirms the change persisted
//     Step 6 (Delete) removes the order
//     Step 7 (Verify Deleted) confirms it returns 404
//
//   'serial' mode means tests run ONE BY ONE in the order defined here.
//   This is different from the default parallel mode used in status.spec.ts
//   and books.spec.ts where tests are independent.
//
// HOW STATE IS SHARED BETWEEN TESTS:
//   'token' and 'orderId' are declared in the DESCRIBE BLOCK SCOPE — outside
//   any individual test function. This means ALL tests inside test.describe.serial
//   can READ and WRITE these variables, even though they run separately.
//   This is possible because serial mode runs tests in the same Node.js process.
//
// AUTH REQUIRED: Yes — import getAccessToken from utils/auth.ts
// ══════════════════════════════════════════════════════════════════════════════

import { test, expect } from '@playwright/test';
// test: defines test cases and lifecycle hooks (test.beforeAll, test.describe.serial)
// expect: assertion library

import { getAccessToken } from '../../utils/auth';
// Import the getAccessToken utility from auth.ts.
// '../../utils/auth' is the RELATIVE PATH from THIS file to auth.ts:
//   this file:  tests/api/orders.spec.ts
//   auth.ts:    utils/auth.ts
//   path back:  ../../ (go up from api/ → tests/ → playwright-typescript/) + utils/auth
// { getAccessToken } uses named import (matches 'export function getAccessToken' in auth.ts)

// ── test.describe.serial() ────────────────────────────────────────────────────
// test.describe.serial() creates a test group where tests run SEQUENTIALLY.
// Regular test.describe() would run tests in parallel — dangerous here because
// step 2 needs the orderId that step 1 creates.
//
// 'serial' also means if one test FAILS, subsequent tests in the block are SKIPPED
// automatically (not a good idea to try to update or delete an order that wasn't created).
test.describe.serial('Orders API', () => {

  // ── SHARED STATE ─────────────────────────────────────────────────────────────
  // These variables are declared in the DESCRIBE BLOCK SCOPE.
  // All test functions below (and test.beforeAll) can read and write them.
  //
  // 'let' = can be reassigned (unlike 'const' which is final).
  // ': string' = TypeScript type annotation: these must be strings (not numbers, booleans, etc.)
  let token: string;    // Bearer token for all authenticated requests in this block
  let orderId: string;  // ID of the order we create in step 1, reused in steps 2–7

  // ── SETUP: GET TOKEN ONCE BEFORE ALL TESTS IN THIS BLOCK ─────────────────────

  // test.beforeAll() runs ONCE before the first test in this describe block.
  // (Not before every test — that would be test.beforeEach())
  // Perfect for getting the auth token, which we only need to do once.
  //
  // async ({ request }) — the request fixture is available in beforeAll too.
  test.beforeAll(async ({ request }) => {

    // Call our auth utility to get (or retrieve cached) Bearer token.
    // await: getAccessToken is async, so we wait for the Promise to resolve.
    // The result is stored in 'token' (the describe-block variable above).
    token = await getAccessToken(request);
    // After this line, token is a string like "eyJhbGciOiJIUzI1NiI..."
    // Every test below can use 'token' without getting it themselves.
  });

  // ════════════════════════════════════════════════════════════════════════════
  // STEP 1: CREATE AN ORDER
  // ════════════════════════════════════════════════════════════════════════════

  test('POST /orders — create a new order', async ({ request }) => {

    // request.post() sends an HTTP POST request.
    // The path '/orders' is appended to baseURL → https://simple-books-api.click/orders
    const response = await request.post('/orders', {

      // headers: add extra headers for THIS request only.
      // Authorization: Bearer <token> — proves we are a registered API client.
      // Template literal: `Bearer ${token}` → e.g. "Bearer eyJhbGciOiJIUzI1NiI..."
      headers: { Authorization: `Bearer ${token}` },

      // data: the request body (object is automatically serialised to JSON by Playwright).
      // The API requires both bookId and customerName to create an order.
      data: {
        bookId: 1,              // Book to order (must be an existing, available book)
        customerName: 'John Doe', // Name of the customer placing the order
      },
    });

    // 201 = Created — the server successfully created a new resource.
    // Using 201 (not 200) is the correct HTTP status for a POST that creates something.
    expect(response.status()).toBe(201);

    // Parse the JSON response body.
    // Example: { "created": true, "orderId": "LkOMyoQFLRMs7zf" }
    const body = await response.json();

    // body.created should be the boolean 'true'.
    // .toBe(true) is an exact boolean check (not just truthy).
    expect(body.created).toBe(true);

    // .toHaveProperty('orderId') confirms the 'orderId' key exists in the response.
    // This key is what we use in all subsequent tests.
    expect(body).toHaveProperty('orderId');

    // Save the orderId to the shared describe-block variable.
    // This is what makes the "state sharing" possible:
    // test 1 SETS orderId, and tests 2–7 READ orderId.
    orderId = body.orderId;

    // Print the ID for debugging — visible in terminal output.
    console.log(`[orders.spec] Created order ID: ${orderId}`);
  });

  // ════════════════════════════════════════════════════════════════════════════
  // STEP 2: READ ALL ORDERS
  // ════════════════════════════════════════════════════════════════════════════

  test('GET /orders — list all orders', async ({ request }) => {

    // GET /orders returns all orders belonging to this API token.
    const response = await request.get('/orders', {
      headers: { Authorization: `Bearer ${token}` },  // Auth required
    });

    expect(response.status()).toBe(200);
    const orders = await response.json();

    // Confirm the response is an array.
    expect(Array.isArray(orders)).toBe(true);

    // Must have at least the order we just created in step 1.
    expect(orders.length).toBeGreaterThan(0);
  });

  // ════════════════════════════════════════════════════════════════════════════
  // STEP 3: READ ONE ORDER
  // ════════════════════════════════════════════════════════════════════════════

  test('GET /orders/:orderId — get the created order', async ({ request }) => {

    // .toBeDefined() asserts that orderId is NOT undefined.
    // This catches the case where step 1 failed and orderId was never set.
    // Without this guard, the next request would go to /orders/undefined — confusing.
    expect(orderId).toBeDefined();

    // Template literal builds the URL with the orderId embedded in the path:
    //   `/orders/${orderId}` → e.g. "/orders/LkOMyoQFLRMs7zf"
    const response = await request.get(`/orders/${orderId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    expect(response.status()).toBe(200);
    const order = await response.json();

    // Verify the returned order matches what we created.
    expect(order.id).toBe(orderId);          // ID matches
    expect(order.bookId).toBe(1);            // Book ID matches
    expect(order.customerName).toBe('John Doe'); // Customer name matches
  });

  // ════════════════════════════════════════════════════════════════════════════
  // STEP 4: UPDATE THE ORDER
  // ════════════════════════════════════════════════════════════════════════════

  test('PATCH /orders/:orderId — update customer name', async ({ request }) => {

    // request.patch() sends an HTTP PATCH request.
    // PATCH = update ONLY the fields you include in the body.
    //   vs PUT = replace the ENTIRE resource with what you send.
    const response = await request.patch(`/orders/${orderId}`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { customerName: 'Jane Smith' },  // Only send the field we want to change
    });

    // 204 = No Content — success with NO response body.
    // PATCH and DELETE commonly return 204 to say "done, nothing to return."
    // Playwright automatically reads the status — response.json() would fail here
    // because there is no body to parse.
    expect(response.status()).toBe(204);
  });

  test('GET /orders/:orderId — verify the name was updated', async ({ request }) => {

    // After every write (POST/PATCH/DELETE), verify the change with a GET.
    // This is called "read-after-write verification."
    const response = await request.get(`/orders/${orderId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    expect(response.status()).toBe(200);
    const order = await response.json();

    // Name must now be 'Jane Smith' (changed from 'John Doe' in the PATCH above).
    // If the PATCH silently failed, this assertion catches it.
    expect(order.customerName).toBe('Jane Smith');
  });

  // ════════════════════════════════════════════════════════════════════════════
  // STEP 5: DELETE THE ORDER
  // ════════════════════════════════════════════════════════════════════════════

  test('DELETE /orders/:orderId — delete the order', async ({ request }) => {

    // request.delete() sends an HTTP DELETE request.
    // No body needed — the URL identifies what to delete.
    const response = await request.delete(`/orders/${orderId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    // 204 = No Content — deletion was successful.
    expect(response.status()).toBe(204);
  });

  test('GET /orders/:orderId — verify deleted order returns 404', async ({ request }) => {

    // Try to fetch the just-deleted order.
    const response = await request.get(`/orders/${orderId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    // 404 = Not Found — the resource was deleted and no longer exists.
    // This confirms the DELETE actually worked (not a no-op).
    // If this returns 200, there's a serious bug in the API's delete logic.
    expect(response.status()).toBe(404);
  });

  // ════════════════════════════════════════════════════════════════════════════
  // NEGATIVE / SECURITY TESTS
  // ════════════════════════════════════════════════════════════════════════════

  // ── NEGATIVE TEST 1: No auth token ───────────────────────────────────────────

  test('POST /orders without token returns 401 Unauthorized', async ({ request }) => {

    const response = await request.post('/orders', {
      // No 'headers' property — we intentionally omit the Authorization header.
      // This simulates an anonymous (unauthenticated) client.
      data: { bookId: 1, customerName: 'No Auth' },
    });

    // 401 = Unauthorized — the server rejects requests with no credentials.
    // This is a SECURITY test — confirming the API enforces authentication.
    expect(response.status()).toBe(401);
  });

  // ── NEGATIVE TEST 2: Missing required field ───────────────────────────────────

  test('POST /orders with missing customerName returns 400', async ({ request }) => {

    const response = await request.post('/orders', {
      headers: { Authorization: `Bearer ${token}` },  // Valid auth
      data: { bookId: 1 },  // customerName deliberately OMITTED to trigger validation error
    });

    // 400 = Bad Request — the client sent incomplete/invalid data.
    // The API must validate required fields and reject incomplete requests.
    // This test confirms that validation actually happens on the server side.
    expect(response.status()).toBe(400);
  });

});
// End of test.describe.serial('Orders API', ...)

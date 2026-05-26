// ══════════════════════════════════════════════════════════════════════════════
// books.spec.ts — Tests for the /books endpoints
// ══════════════════════════════════════════════════════════════════════════════
//
// WHAT DOES THIS FILE TEST?
//   All /books endpoints — listing, filtering, pagination, single-book details,
//   and error handling for invalid inputs.
//
// ENDPOINTS COVERED:
//   GET /books                   → JSON array of all books (no filter)
//   GET /books?type=fiction      → only fiction books
//   GET /books?type=non-fiction  → only non-fiction books
//   GET /books?limit=2           → at most 2 books
//   GET /books/1                 → full details of book with id=1
//   GET /books/99999             → 404 (book does not exist)
//   GET /books?type=magazine     → 400 (invalid type value)
//   GET /books?limit=0           → 400 (limit out of range)
//
// AUTH REQUIRED: No — all /books endpoints are public.
//   None of these tests need an Authorization header.
//
// PARALLELISM:
//   This file runs in parallel with status.spec.ts (fullyParallel: true in config).
//   Each test in this file also runs in parallel with each other by default.
// ══════════════════════════════════════════════════════════════════════════════

import { test, expect } from '@playwright/test';
// test: function to define individual test cases
// expect: assertion helper — checks values match expectations

// test.describe() groups all /books tests under one label in reports.
test.describe('Books API', () => {

  // ════════════════════════════════════════════════════════════════════════════
  // HAPPY PATH TESTS — valid requests that should return HTTP 200 + data
  // ════════════════════════════════════════════════════════════════════════════

  // ── TEST 1: List all books ────────────────────────────────────────────────────

  test('GET /books — returns a non-empty list of books', async ({ request }) => {
    // async ({ request }): the { request } fixture is Playwright's built-in HTTP client.
    // It has baseURL and extraHTTPHeaders pre-applied from playwright.config.ts.

    // request.get('/books') sends: GET https://simple-books-api.click/books
    // await: wait for the HTTP response before continuing.
    const response = await request.get('/books');

    // Assert HTTP 200 OK
    expect(response.status()).toBe(200);

    // response.json() parses the response body.
    // await: response.json() is async (reads the response stream), so we await it.
    // /books returns a JSON array: [{"id":1,...}, {"id":2,...}, ...]
    // In TypeScript, JSON arrays become JavaScript arrays.
    const books = await response.json();

    // Array.isArray(books) → true if 'books' is a JavaScript array.
    // Confirms the API returned an array, not an object or primitive.
    expect(Array.isArray(books)).toBe(true);

    // books.length → number of items in the array.
    // .toBeGreaterThan(0) → must have MORE than 0 items (at least 1).
    expect(books.length).toBeGreaterThan(0);

    // books[0] → first item in the array (0-based indexing: 0=first, 1=second, ...)
    // .toHaveProperty('id') → asserts the object has a key called 'id'.
    // This checks the response structure matches what we expect.
    expect(books[0]).toHaveProperty('id');
    expect(books[0]).toHaveProperty('name');
    expect(books[0]).toHaveProperty('type');
  });

  // ── TEST 2: Filter by fiction ─────────────────────────────────────────────────

  test('GET /books?type=fiction — returns only fiction books', async ({ request }) => {

    // request.get() with params: adds QUERY PARAMETERS to the URL.
    // params: { type: 'fiction' } appends ?type=fiction to the URL.
    // Full URL: https://simple-books-api.click/books?type=fiction
    const response = await request.get('/books', {
      params: { type: 'fiction' },
      // params: takes an object of key-value pairs.
      // Playwright handles URL encoding automatically (spaces become %20, etc.)
    });

    expect(response.status()).toBe(200);
    const books = await response.json();

    // At least one fiction book should exist.
    expect(books.length).toBeGreaterThan(0);

    // for...of loop: iterate over every book in the array.
    // 'book' represents each element (each book object) one at a time.
    // This is TypeScript/JavaScript's modern loop for arrays and iterables.
    for (const book of books) {
      // Each book object has a 'type' property.
      // We assert it equals 'fiction' for EVERY single book in the response.
      // If even one book has a different type, this fails with a clear message.
      expect(book.type).toBe('fiction');
    }
  });

  // ── TEST 3: Filter by non-fiction ─────────────────────────────────────────────

  test('GET /books?type=non-fiction — returns only non-fiction books', async ({ request }) => {

    // Same pattern as the fiction test, different type value.
    const response = await request.get('/books', {
      params: { type: 'non-fiction' },
    });

    expect(response.status()).toBe(200);
    const books = await response.json();

    for (const book of books) {
      // Every book returned must be non-fiction.
      expect(book.type).toBe('non-fiction');
    }
  });

  // ── TEST 4: Limit results ─────────────────────────────────────────────────────

  test('GET /books?limit=2 — returns at most 2 books', async ({ request }) => {

    const response = await request.get('/books', {
      params: { limit: 2 },
      // limit: 2 is a number; Playwright converts it to the string "2" in the URL.
      // Full URL: /books?limit=2
    });

    expect(response.status()).toBe(200);
    const books = await response.json();

    // .toBeLessThanOrEqualTo(2) → the value must be ≤ 2.
    // We say "at most 2" (not exactly 2) because the API might have fewer
    // available books than the requested limit.
    expect(books.length).toBeLessThanOrEqualTo(2);
  });

  // ── TEST 5: Get single book ───────────────────────────────────────────────────

  test('GET /books/1 — returns full details of book with id=1', async ({ request }) => {

    // The book ID is part of the PATH, not a query parameter.
    // /books/1 → get book with ID 1 (no ? needed, it's in the URL itself)
    const response = await request.get('/books/1');
    // Full URL: https://simple-books-api.click/books/1

    expect(response.status()).toBe(200);

    // A single-book endpoint returns a JSON OBJECT, not an array.
    // In JavaScript/TypeScript: { id: 1, name: "...", author: "...", ... }
    const book = await response.json();

    // book.id reads the 'id' property. .toBe(1) checks it equals the number 1.
    expect(book.id).toBe(1);

    // .toHaveProperty('key') checks the key EXISTS, regardless of value.
    // A detailed single-book view includes more fields than the list view.
    expect(book).toHaveProperty('name');
    expect(book).toHaveProperty('author');   // Author field only in single-book view
    expect(book).toHaveProperty('available'); // Whether this book can be ordered
  });

  // ════════════════════════════════════════════════════════════════════════════
  // NEGATIVE / ERROR PATH TESTS — invalid requests that should return 4xx
  // ════════════════════════════════════════════════════════════════════════════
  // These tests confirm the API correctly rejects bad input.
  // Testing error cases is as important as testing success cases.

  // ── TEST 6: Non-existent book ─────────────────────────────────────────────────

  test('GET /books/99999 — non-existent book returns 404', async ({ request }) => {

    // 99999 is an ID that does not exist in the Simple Books API.
    // The server should return 404 Not Found.
    const response = await request.get('/books/99999');

    // 404 = Not Found — the requested resource does not exist on the server.
    expect(response.status()).toBe(404);
  });

  // ── TEST 7: Invalid book type ─────────────────────────────────────────────────

  test('GET /books?type=magazine — invalid type returns 400', async ({ request }) => {

    // "magazine" is not a valid type (only "fiction" and "non-fiction" are).
    // This is intentionally wrong to test input VALIDATION.
    const response = await request.get('/books', {
      params: { type: 'magazine' },
    });

    // 400 = Bad Request — the client sent invalid/unsupported input.
    // The server validates the 'type' parameter and rejects unknown values.
    expect(response.status()).toBe(400);
  });

  // ── TEST 8: Invalid limit value ───────────────────────────────────────────────

  test('GET /books?limit=0 — limit below minimum returns 400', async ({ request }) => {

    // limit=0 is outside the valid range (1–20).
    // This tests the API's boundary/edge case validation.
    const response = await request.get('/books', {
      params: { limit: 0 },
    });

    // 400 = Bad Request — parameter value out of allowed range.
    expect(response.status()).toBe(400);
  });

});
// End of test.describe('Books API', ...)

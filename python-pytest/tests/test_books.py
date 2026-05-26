"""
test_books.py — Tests for the /books endpoints
================================================

WHAT DOES THIS FILE TEST?
  All the /books endpoints — listing, filtering, single-book retrieval,
  and error handling for invalid inputs.

ENDPOINTS COVERED:
  GET /books                    → returns a JSON array of all books
  GET /books?type=fiction       → returns only fiction books (filtered)
  GET /books?type=non-fiction   → returns only non-fiction books (filtered)
  GET /books?limit=2            → returns at most 2 books
  GET /books/{bookId}           → returns one specific book as a JSON object
  GET /books/99999              → should return 404 (book does not exist)
  GET /books?type=magazine      → should return 400 (invalid type)
  GET /books?limit=0            → should return 400 (invalid limit)

AUTH REQUIRED: No — all /books endpoints are public.
  None of the test methods below use auth_headers or auth_token.

TEST STRATEGY:
  Happy path  → send valid requests → expect HTTP 200 and correct data
  Sad path    → send invalid inputs → expect HTTP 4xx error codes
"""

import requests
# HTTP client library — sends GET requests and returns Response objects.


# ══════════════════════════════════════════════════════════════════════════════
# TEST CLASS
# ══════════════════════════════════════════════════════════════════════════════

class TestBooks:

    # ══════════════════════════════════════════════════════════════════════════
    # HAPPY PATH TESTS (all should return HTTP 200)
    # ══════════════════════════════════════════════════════════════════════════

    # ──────────────────────────────────────────────────────────────────────────
    # TEST 1: Get all books
    # ──────────────────────────────────────────────────────────────────────────

    def test_get_all_books_returns_list(self, base_url):
        # base_url: fixture from conftest.py — "https://simple-books-api.click"
        """GET /books should return a non-empty JSON array of books."""

        # No query parameters — get everything the API has.
        response = requests.get(f"{base_url}/books")
        # Full URL: GET https://simple-books-api.click/books

        assert response.status_code == 200
        # HTTP 200 = OK. If the API is down or has an internal error, we'd get 5xx.

        # response.json() parses the response body.
        # /books returns a JSON ARRAY: [{"id":1,"name":"..."}, {"id":2,...}, ...]
        # In Python, a JSON array becomes a LIST.
        books = response.json()

        # isinstance(books, list) → True if 'books' is a Python list.
        # This confirms the API returned an array, not a dict or string.
        assert isinstance(books, list), "Expected a list of books"

        # len(books) → number of items in the list.
        # > 0 → the list must not be empty (API must have at least one book).
        assert len(books) > 0, "Book list should not be empty"

        # books[0] → the FIRST item in the list (index 0 = first element in Python)
        # Python uses 0-based indexing: first=0, second=1, third=2, ...
        first_book = books[0]

        # 'in' checks if a KEY exists in a dictionary.
        # first_book is a dict like {"id":1, "name":"...", "type":"fiction"}
        # This confirms the API response has the expected data structure.
        assert "id"   in first_book, "Book should have an 'id' field"
        assert "name" in first_book, "Book should have a 'name' field"
        assert "type" in first_book, "Book should have a 'type' field"

    # ──────────────────────────────────────────────────────────────────────────
    # TEST 2: Filter by fiction
    # ──────────────────────────────────────────────────────────────────────────

    def test_filter_books_by_fiction(self, base_url):
        """GET /books?type=fiction should only return fiction books."""

        # params= adds QUERY PARAMETERS to the URL.
        # {"type": "fiction"} becomes ?type=fiction appended to the URL.
        # requests handles URL encoding automatically (e.g. spaces → %20).
        # Full URL: GET https://simple-books-api.click/books?type=fiction
        response = requests.get(f"{base_url}/books", params={"type": "fiction"})

        assert response.status_code == 200

        # Parse the JSON array of fiction books.
        books = response.json()

        # Iterate over EVERY book in the list.
        # A for loop in Python runs the indented block once for each item.
        for book in books:
            # book["type"] reads the "type" field from each book dict.
            # We assert it equals "fiction" for EVERY book.
            # If even ONE book has a different type, the assertion fails
            # and the error message shows which book was wrong.
            assert book["type"] == "fiction", (
                f"Expected fiction, but got: {book['type']} (book: {book['name']})"
            )
            # Note: inside an f-string, use single quotes for dict keys
            # when the f-string itself uses double quotes (Python string rule).

    # ──────────────────────────────────────────────────────────────────────────
    # TEST 3: Filter by non-fiction
    # ──────────────────────────────────────────────────────────────────────────

    def test_filter_books_by_non_fiction(self, base_url):
        """GET /books?type=non-fiction should only return non-fiction books."""

        # Same pattern as test_filter_books_by_fiction, different type.
        response = requests.get(f"{base_url}/books", params={"type": "non-fiction"})

        assert response.status_code == 200
        books = response.json()

        for book in books:
            assert book["type"] == "non-fiction", (
                f"Expected non-fiction, but got: {book['type']}"
            )

    # ──────────────────────────────────────────────────────────────────────────
    # TEST 4: Limit results
    # ──────────────────────────────────────────────────────────────────────────

    def test_limit_books_result(self, base_url):
        """GET /books?limit=2 should return at most 2 books."""

        # limit=2 adds ?limit=2 to the URL.
        # The API should return a maximum of 2 books, regardless of how many exist.
        response = requests.get(f"{base_url}/books", params={"limit": 2})

        assert response.status_code == 200
        books = response.json()

        # len(books) → count how many books were returned.
        # <= 2 → must be at most 2 (could be 0, 1, or 2 — all valid).
        # We say "at most" because the API might have fewer than 2 books of a type.
        assert len(books) <= 2, f"Expected at most 2 books, got {len(books)}"

    # ──────────────────────────────────────────────────────────────────────────
    # TEST 5: Get a single book by ID
    # ──────────────────────────────────────────────────────────────────────────

    def test_get_single_book_by_id(self, base_url):
        """GET /books/1 should return the full details of book with id=1."""

        # No params= here — the ID is part of the URL PATH, not a query parameter.
        # Path: /books/1
        # Compare: /books?id=1 (query param) vs /books/1 (path param — this one)
        response = requests.get(f"{base_url}/books/1")
        # Full URL: https://simple-books-api.click/books/1

        assert response.status_code == 200

        # A SINGLE book endpoint returns a JSON OBJECT (dict in Python),
        # not an array. Example: {"id":1, "name":"The Russian", ...}
        book = response.json()

        # Verify the returned book has id=1 (we asked for book 1).
        assert book["id"] == 1

        # Verify all expected fields exist in a single-book response.
        # A detailed view has more fields than the list view.
        assert "name"      in book   # Book title
        assert "author"    in book   # Author name
        assert "type"      in book   # "fiction" or "non-fiction"
        assert "available" in book   # Boolean: can this book be ordered right now?

    # ══════════════════════════════════════════════════════════════════════════
    # NEGATIVE / ERROR PATH TESTS (all should return 4xx errors)
    # ══════════════════════════════════════════════════════════════════════════
    # Testing error cases is just as important as testing the happy path.
    # A well-designed API must reject bad input with clear error codes.
    # These tests verify that error handling is correct.

    # ──────────────────────────────────────────────────────────────────────────
    # TEST 6: Non-existent book
    # ──────────────────────────────────────────────────────────────────────────

    def test_non_existent_book_returns_404(self, base_url):
        """GET /books/99999 should return 404 because that ID doesn't exist."""

        # 99999 is an ID we know doesn't exist in the Simple Books API.
        # The API should tell us the resource was not found.
        response = requests.get(f"{base_url}/books/99999")

        # 404 = Not Found — the server understood the request but found nothing.
        # This is the CORRECT HTTP code for "no book with that ID exists."
        # If the API returned 200 with an empty body, it would be confusing.
        assert response.status_code == 404, (
            f"Expected 404 Not Found, got {response.status_code}"
        )

    # ──────────────────────────────────────────────────────────────────────────
    # TEST 7: Invalid book type
    # ──────────────────────────────────────────────────────────────────────────

    def test_invalid_book_type_returns_400(self, base_url):
        """GET /books?type=magazine should fail because 'magazine' is not a valid type."""

        # Only "fiction" and "non-fiction" are valid types.
        # Sending "magazine" is intentional — we're testing input VALIDATION.
        response = requests.get(f"{base_url}/books", params={"type": "magazine"})

        # 400 = Bad Request — the client sent invalid/malformed input.
        # The server rejects this before doing any work.
        assert response.status_code == 400, (
            f"Expected 400 Bad Request for invalid type, got {response.status_code}"
        )

    # ──────────────────────────────────────────────────────────────────────────
    # TEST 8: Invalid limit value
    # ──────────────────────────────────────────────────────────────────────────

    def test_limit_zero_returns_400(self, base_url):
        """GET /books?limit=0 should fail because limit must be between 1 and 20."""

        # 0 is out of range — the API requires limit to be 1–20.
        # Sending limit=0 is intentional to test boundary/edge case validation.
        response = requests.get(f"{base_url}/books", params={"limit": 0})

        # 400 = Bad Request — invalid parameter value.
        assert response.status_code == 400

// ──────────────────────────────────────────────────────────────────────────────
// PACKAGE DECLARATION
// ──────────────────────────────────────────────────────────────────────────────
package com.simplebooksapi.tests;

// ──────────────────────────────────────────────────────────────────────────────
// IMPORTS
// ──────────────────────────────────────────────────────────────────────────────

import com.simplebooksapi.base.BaseTest;
// Our BaseTest parent class — gives us BASE_URL and requestSpec.

import io.restassured.RestAssured;
// Provides the .given() entry point for building HTTP requests.

import org.testng.annotations.Test;
// @Test marks methods for TestNG to find and execute as test cases.

import static org.hamcrest.Matchers.*;
// Static import of all Hamcrest matchers (equalTo, notNullValue, everyItem, etc.)
// so we can use them without the class prefix.

/**
 * BooksTest — covers all /books endpoint scenarios.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * ENDPOINTS COVERED:
 * ──────────────────────────────────────────────────────────────────────────────
 *   GET /books                      List all books (returns a JSON array)
 *   GET /books?type=fiction         Filter by book type
 *   GET /books?type=non-fiction     Filter by another type
 *   GET /books?limit=N              Limit the number of results returned
 *   GET /books/{bookId}             Get one specific book by its ID
 *
 * AUTH REQUIRED: No — all /books endpoints are public
 *
 * TEST STRATEGY:
 *   Happy path tests  → send valid requests, verify correct data comes back
 *   Negative tests    → send invalid inputs, verify the API rejects them correctly
 * ──────────────────────────────────────────────────────────────────────────────
 */
public class BooksTest extends BaseTest {

    // ══════════════════════════════════════════════════════════════════════════
    // HAPPY PATH TESTS — Valid requests that should succeed (HTTP 200)
    // ══════════════════════════════════════════════════════════════════════════

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 1: List all books
    // ──────────────────────────────────────────────────────────────────────────

    @Test(description = "GET /books returns a non-empty list of books")
    public void testGetAllBooks() {

        RestAssured.given()
                .spec(requestSpec)    // Apply base URL, Content-Type, logging

        .when()
                .get("/books")        // GET https://simple-books-api.click/books

        .then()
                .statusCode(200)      // Assert HTTP 200 OK

                // "$" is the JsonPath expression for the ROOT of the document.
                // Here the root is a JSON array (list of books).
                // is(not(empty())) combines two Hamcrest matchers:
                //   not()   → negates whatever is inside
                //   empty() → matches an empty collection
                // Together: assert the list is NOT empty (has at least one book).
                .body("$", is(not(empty())))

                // "[0].id" — JsonPath for the first item ([0]) and its "id" field.
                // notNullValue() → assert the value exists and is not null.
                // This confirms each book object has an id field.
                .body("[0].id", notNullValue())

                // "[0].name" — the "name" field of the first book.
                // Confirms books have a name field in the response structure.
                .body("[0].name", notNullValue());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 2: Filter by fiction
    // ──────────────────────────────────────────────────────────────────────────

    @Test(description = "GET /books?type=fiction returns only fiction books")
    public void testFilterByFiction() {

        RestAssured.given()
                .spec(requestSpec)

                // .queryParam("type", "fiction"):
                // Adds a QUERY PARAMETER to the URL.
                // Result: GET /books?type=fiction
                // Query parameters are key=value pairs after the ? in a URL.
                // The API uses this to filter which books to return.
                .queryParam("type", "fiction")

        .when()
                .get("/books")      // Sends: GET /books?type=fiction

        .then()
                .statusCode(200)
                .body("$", is(not(empty())))  // List is not empty

                // "type" — JsonPath expression that extracts the "type" field
                // from EVERY object in the root array.
                // Result: a Java list like ["fiction", "fiction", "fiction"]
                //
                // everyItem(equalTo("fiction")):
                //   everyItem() → applies the inner matcher to EVERY element of the list
                //   equalTo("fiction") → each element must exactly equal "fiction"
                //
                // This is a concise way to say: "all books must have type=fiction"
                // Without everyItem(), you'd have to check each book individually.
                .body("type", everyItem(equalTo("fiction")));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 3: Filter by non-fiction
    // ──────────────────────────────────────────────────────────────────────────

    @Test(description = "GET /books?type=non-fiction returns only non-fiction books")
    public void testFilterByNonFiction() {

        RestAssured.given()
                .spec(requestSpec)
                .queryParam("type", "non-fiction")  // Adds ?type=non-fiction to URL

        .when()
                .get("/books")       // Sends: GET /books?type=non-fiction

        .then()
                .statusCode(200)

                // Every book's "type" field must equal "non-fiction".
                // Same pattern as testFilterByFiction above but for the other type.
                .body("type", everyItem(equalTo("non-fiction")));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 4: Limit results
    // ──────────────────────────────────────────────────────────────────────────

    @Test(description = "GET /books?limit=2 returns at most 2 books")
    public void testLimitBooks() {

        RestAssured.given()
                .spec(requestSpec)
                .queryParam("limit", 2)   // Adds ?limit=2 to URL
                                          // Note: 2 is an int, REST Assured converts it to "2" string

        .when()
                .get("/books")            // Sends: GET /books?limit=2

        .then()
                .statusCode(200)

                // "$.size()" — a special JsonPath function that counts the number
                // of items in the root array ($).
                // Returns: an integer (e.g. 0, 1, or 2)
                //
                // lessThanOrEqualTo(2) — Hamcrest matcher: value must be ≤ 2.
                // We say "at most 2" (not exactly 2) because if the API has only
                // 1 book matching, returning 1 is also valid behaviour.
                .body("$.size()", lessThanOrEqualTo(2));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 5: Get a single book by ID
    // ──────────────────────────────────────────────────────────────────────────

    @Test(description = "GET /books/{bookId} returns the correct book")
    public void testGetSingleBook() {

        RestAssured.given()
                .spec(requestSpec)

                // .pathParam("bookId", 1):
                // Sets a PATH PARAMETER — a variable part of the URL path.
                // The placeholder {bookId} in .get("/books/{bookId}") below
                // is replaced with the value 1.
                // Result: GET /books/1
                //
                // Path params are PART OF the URL structure (not after ?).
                // Compare: /books/1 (path param) vs /books?id=1 (query param)
                .pathParam("bookId", 1)

        .when()
                .get("/books/{bookId}")   // {bookId} is replaced with 1 → GET /books/1

        .then()
                .statusCode(200)

                // "id" — the "id" field of the returned book object.
                // equalTo(1) — the value must be exactly the integer 1.
                // Note: equalTo(1) not equalTo("1") — JSON numbers are integers in Java.
                .body("id", equalTo(1))

                // "name", "author", "available" — other fields that must be present.
                // notNullValue() just checks they exist and are not null.
                // A full book object looks like:
                //   { "id":1, "name":"The Russian", "type":"fiction",
                //     "available":false, "author":"James Patterson", ... }
                .body("name", notNullValue())
                .body("author", notNullValue())
                .body("available", notNullValue());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NEGATIVE / ERROR PATH TESTS — Invalid requests that should be rejected
    // ══════════════════════════════════════════════════════════════════════════
    // These tests verify the API's ERROR HANDLING.
    // A well-built API does not crash or return 200 when given bad input —
    // it returns a meaningful error code so the client can understand what went wrong.

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 6: Book not found
    // ──────────────────────────────────────────────────────────────────────────

    @Test(description = "GET /books/99999 returns 404 for a non-existent book")
    public void testGetNonExistentBook() {

        RestAssured.given()
                .spec(requestSpec)
                .pathParam("bookId", 99999)   // A book ID we know does not exist

        .when()
                .get("/books/{bookId}")        // GET /books/99999

        .then()
                // 404 = "Not Found" — the requested resource does not exist on the server.
                // This is the CORRECT error code for "no book with that ID".
                // If the API returned 200 with an empty body, that would be confusing.
                .statusCode(404);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 7: Invalid book type
    // ──────────────────────────────────────────────────────────────────────────

    @Test(description = "GET /books?type=invalid returns 400 for an unsupported type")
    public void testInvalidBookType() {

        RestAssured.given()
                .spec(requestSpec)
                // "magazine" is not a valid type — only "fiction" and "non-fiction" are allowed.
                // We intentionally send a bad value to test error handling.
                .queryParam("type", "magazine")

        .when()
                .get("/books")    // GET /books?type=magazine

        .then()
                // 400 = "Bad Request" — the client sent invalid input.
                // The server rejects the request before doing any database work.
                // This is better than silently returning an empty list.
                .statusCode(400);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 8: Invalid limit value
    // ──────────────────────────────────────────────────────────────────────────

    @Test(description = "GET /books?limit=0 returns 400 (limit must be >= 1)")
    public void testInvalidLimit() {

        RestAssured.given()
                .spec(requestSpec)
                // 0 is outside the allowed range (1–20).
                // The API should validate input and reject this with 400.
                // Why validate? Returning 0 items could be confused with "no results found"
                // when the real issue is that the input was invalid.
                .queryParam("limit", 0)

        .when()
                .get("/books")    // GET /books?limit=0

        .then()
                // 400 = Bad Request.
                // The API enforces: limit must be between 1 and 20.
                .statusCode(400);
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// PACKAGE DECLARATION
// ──────────────────────────────────────────────────────────────────────────────
// This class lives in the 'tests' sub-package.
// Package path: com/simplebooksapi/tests/StatusTest.java
package com.simplebooksapi.tests;

// ──────────────────────────────────────────────────────────────────────────────
// IMPORTS
// ──────────────────────────────────────────────────────────────────────────────

import com.simplebooksapi.base.BaseTest;
// Imports our own BaseTest class so we can extend it.
// Gives us: BASE_URL constant and the requestSpec shared template.

import io.restassured.RestAssured;
// Entry point for the REST Assured library — provides .given() to start requests.

import org.testng.annotations.Test;
// @Test marks a method as a test case that TestNG should discover and run.
// Without this annotation, TestNG ignores the method completely.

import static org.hamcrest.Matchers.*;
// Static import of ALL Hamcrest matchers.
// Hamcrest provides readable assertion helpers used inside REST Assured's .then() block.
// 'static import' means we can write: equalTo("OK") instead of Matchers.equalTo("OK")
// Commonly used matchers:
//   equalTo(value)          → exact match
//   notNullValue()          → value is not null
//   is(not(empty()))        → list is not empty
//   containsString("text")  → string contains a substring
//   greaterThan(n)          → numeric comparison

/**
 * StatusTest — verifies the API health-check endpoint GET /status.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * WHAT IS A HEALTH CHECK?
 * ──────────────────────────────────────────────────────────────────────────────
 * Most APIs expose a /status or /health endpoint that simply confirms the
 * service is running. It's the simplest possible test — no data, no auth —
 * just "is the server reachable and responding correctly?"
 *
 * Run this first. If it fails, all other tests will fail too (the API is down).
 *
 * ENDPOINT:
 *   GET https://simple-books-api.click/status
 *
 * EXPECTED RESPONSE:
 *   HTTP 200 OK
 *   Content-Type: application/json
 *   Body: { "status": "OK" }
 *
 * AUTH REQUIRED: No
 * ──────────────────────────────────────────────────────────────────────────────
 *
 * 'extends BaseTest' — inherits BASE_URL and requestSpec from BaseTest.
 * StatusTest does NOT have to define those again.
 */
public class StatusTest extends BaseTest {

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 1: Check status code and body
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * testApiStatusIsUp()
     *   Sends GET /status and asserts:
     *     1. HTTP status code is 200 (OK)
     *     2. Response body contains { "status": "OK" }
     *
     * @Test(description = "...") — the description appears in the HTML test report
     * next to the pass/fail result, making reports easier to read.
     */
    @Test(description = "API health-check returns HTTP 200 and status OK")
    public void testApiStatusIsUp() {

        // REST Assured uses a GIVEN / WHEN / THEN pattern — like a Gherkin test:
        //   GIVEN: set up the request (headers, body, params)
        //   WHEN:  send the request (which HTTP method and path)
        //   THEN:  assert the response (status code, body, headers)

        RestAssured.given()
                // .spec(requestSpec): apply the shared template from BaseTest.
                // This adds: base URL, Content-Type header, and logging filters.
                // Every test that calls .spec(requestSpec) benefits from all that config.
                .spec(requestSpec)

        .when()
                // .get("/status"): send an HTTP GET request.
                // The full URL becomes: https://simple-books-api.click/status
                // (base URL from requestSpec + "/status" path)
                .get("/status")

        .then()
                // .statusCode(200): assert the HTTP response code equals 200.
                // HTTP 200 = "OK" — the request succeeded.
                // If the server returns 404, 500, or anything else, the test FAILS.
                .statusCode(200)

                // .body("status", equalTo("OK")):
                //   "status"       → the JSON key to look up in the response body
                //   equalTo("OK")  → Hamcrest matcher: exact string match
                //
                // Response body: { "status": "OK" }
                //                    ↑               ↑
                //                  key            expected value
                //
                // If the body is { "status": "DOWN" } the test FAILS.
                .body("status", equalTo("OK"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 2: Verify Content-Type header
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * testResponseIsJson()
     *   Sends GET /status and asserts the Content-Type header is application/json.
     *
     * WHY test this?
     *   If the API accidentally returns HTML (e.g. an error page) instead of JSON,
     *   a Content-Type check catches it immediately. Clients that expect JSON
     *   will break if they receive HTML.
     */
    @Test(description = "Response has JSON Content-Type header")
    public void testResponseIsJson() {
        RestAssured.given()
                .spec(requestSpec)     // Apply shared base URL, headers, logging
        .when()
                .get("/status")        // Send GET https://simple-books-api.click/status
        .then()
                .statusCode(200)       // Assert HTTP 200

                // .contentType("application/json"):
                // Asserts the response has a Content-Type header that contains
                // "application/json". The actual header value might be
                // "application/json; charset=utf-8" — REST Assured does a
                // substring match so that still passes.
                .contentType("application/json");
    }
}

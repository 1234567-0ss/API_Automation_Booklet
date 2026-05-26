// ──────────────────────────────────────────────────────────────────────────────
// PACKAGE DECLARATION
// ──────────────────────────────────────────────────────────────────────────────
// This file lives in the 'utils' package — a convention for helper/utility classes
// that don't contain test methods themselves but support the test classes.
package com.simplebooksapi.utils;

// ──────────────────────────────────────────────────────────────────────────────
// IMPORTS
// ──────────────────────────────────────────────────────────────────────────────

import io.restassured.RestAssured;
// Entry point for REST Assured — provides .given() to start building requests.

import io.restassured.response.Response;
// Response is the object that REST Assured returns after executing a request.
// It holds the status code, headers, and body of the HTTP response.
// We use it here to extract the accessToken from the JSON body.

import io.restassured.specification.RequestSpecification;
// The shared request template from BaseTest (base URL, headers, logging).
// We receive it as a parameter so AuthHelper doesn't need to rebuild it.

import java.util.HashMap;
// HashMap is a Java data structure that stores key-value pairs.
// We use it to build the JSON request body: {"clientName": "...", "clientEmail": "..."}
// HashMap is the Java equivalent of a Python dict or a JavaScript object.

import java.util.Map;
// Map is the INTERFACE (general type) that HashMap implements.
// Using the interface type (Map) instead of the concrete type (HashMap) is
// good Java practice — it makes the code easier to swap or extend later.

import java.util.UUID;
// UUID (Universally Unique IDentifier) generates random, globally unique IDs.
// Example: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
// We use it to create a unique email address for each test run.

/**
 * AuthHelper — a utility class that handles API client registration and token caching.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * WHY DO WE NEED THIS CLASS?
 * ──────────────────────────────────────────────────────────────────────────────
 * The Simple Books API protects the /orders endpoints with authentication.
 * To call them, you need a BEARER TOKEN in the Authorization header:
 *   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 *
 * To GET a token, you must first REGISTER as an API client:
 *   POST /api-clients/
 *   Body: { "clientName": "MyApp", "clientEmail": "test@example.com" }
 *   Response: { "accessToken": "eyJhbGci..." }
 *
 * Tokens are valid for 7 days, so registering once per test run is enough.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * WHY CACHE THE TOKEN?
 * ──────────────────────────────────────────────────────────────────────────────
 * If every test method registered a new client, we'd hit the API many times
 * just for setup, and each run would leave behind junk client registrations.
 * By caching (storing once, reusing), registration happens exactly ONCE
 * per test run, no matter how many tests call getAccessToken().
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * WHY A UNIQUE EMAIL EACH RUN?
 * ──────────────────────────────────────────────────────────────────────────────
 * The API returns HTTP 409 Conflict if you try to register an email that
 * already exists. Using a UUID-based email guarantees it's fresh every run.
 * ──────────────────────────────────────────────────────────────────────────────
 */
public class AuthHelper {

    // ──────────────────────────────────────────────────────────────────────────
    // TOKEN CACHE
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * accessToken — holds the registered token for the duration of the test run.
     *
     * KEYWORDS EXPLAINED:
     *   private  → only code INSIDE this class can read or write this field.
     *              Test classes cannot access it directly — they go through
     *              the getAccessToken() method below.
     *   static   → one shared value for the entire JVM run. All calls to
     *              getAccessToken() share the same accessToken field.
     *   String   → the data type; a text string containing the JWT token.
     *
     * Initial value is null (Java default for objects), meaning "not yet registered".
     * After the first successful registration, it holds the token string.
     */
    private static String accessToken;

    // ──────────────────────────────────────────────────────────────────────────
    // PUBLIC METHOD: GET (OR REGISTER AND GET) THE ACCESS TOKEN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * getAccessToken() — returns the Bearer token, registering if needed.
     *
     * FLOW:
     *   1st call  → accessToken is null → register → cache → return token
     *   2nd call+ → accessToken is set  → skip registration → return cached token
     *
     * @param spec  the shared RequestSpecification from BaseTest
     *              (contains base URL, headers, and logging filters)
     * @return      the access token string (e.g. "eyJhbGciOiJIUzI1NiI...")
     *
     * HOW TO USE IN A TEST:
     *   token = AuthHelper.getAccessToken(requestSpec);
     *   // Then include it in requests:
     *   RestAssured.given().spec(requestSpec)
     *       .header("Authorization", "Bearer " + token)
     *       ...
     */
    public static String getAccessToken(RequestSpecification spec) {

        // ── CACHE CHECK ────────────────────────────────────────────────────────
        // If accessToken is not null, we already registered in this run.
        // Return immediately without hitting the API again.
        // This is the "short-circuit" pattern — expensive work is skipped on repeat calls.
        if (accessToken != null) {
            return accessToken;
        }

        // ── GENERATE UNIQUE CLIENT DETAILS ────────────────────────────────────
        // UUID.randomUUID() generates a random 128-bit ID like:
        //   "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        // .toString() converts it to that text format.
        // .substring(0, 8) takes only the first 8 characters: "a1b2c3d4"
        // This gives us a short, unique suffix for the name and email.
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        // Build the request body as a Java Map (will be serialised to JSON by Jackson).
        // The API expects: { "clientName": "...", "clientEmail": "..." }
        Map<String, String> body = new HashMap<>();

        // clientName: any human-readable label for this API client.
        // Using uniqueId makes each registration identifiable in logs.
        body.put("clientName", "TestClient_" + uniqueId);
        // e.g. "TestClient_a1b2c3d4"

        // clientEmail: must be a unique email. The API rejects duplicates with 409.
        // uniqueId ensures a fresh address every run.
        body.put("clientEmail", "testclient_" + uniqueId + "@example.com");
        // e.g. "testclient_a1b2c3d4@example.com"

        // ── SEND POST /api-clients/ ────────────────────────────────────────────
        // RestAssured.given() starts a new HTTP request chain.
        // .spec(spec)  applies the shared config (base URL, headers, logging)
        // .body(body)  attaches the HashMap as JSON (Jackson serialises it)
        // .post(path)  sends the HTTP POST request to BASE_URL + "/api-clients/"
        // The result is stored in 'response' so we can inspect it.
        Response response = RestAssured.given()
                .spec(spec)                // Use shared base URL, Content-Type header, logging
                .body(body)                // Request body: {"clientName":"...","clientEmail":"..."}
                .post("/api-clients/");    // HTTP POST to https://simple-books-api.click/api-clients/

        // ── ASSERT SUCCESS ─────────────────────────────────────────────────────
        // .then() switches from "building the request" to "asserting the response".
        // .statusCode(201) asserts the HTTP status is 201 Created.
        // 201 = the server successfully created a new resource (our API client).
        // If the status is anything else (400, 409, 500...) this line throws an
        // AssertionError and the test fails immediately with a clear message.
        response.then().statusCode(201);

        // ── EXTRACT AND CACHE THE TOKEN ────────────────────────────────────────
        // response.jsonPath() gives us a helper to navigate the JSON response body.
        // .getString("accessToken") reads the value at key "accessToken" as a String.
        // Example response body:
        //   { "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." }
        accessToken = response.jsonPath().getString("accessToken");

        // Print the first 20 characters of the token to the console for confirmation.
        // We don't print the full token for security reasons (don't log secrets in full).
        System.out.println("[AuthHelper] Token obtained: " + accessToken.substring(0, 20) + "...");

        // Return the token to the caller (e.g. OrdersTest.setupAuth()).
        return accessToken;
    }
}

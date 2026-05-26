// ──────────────────────────────────────────────────────────────────────────────
// PACKAGE DECLARATION
// ──────────────────────────────────────────────────────────────────────────────
package com.simplebooksapi.tests;

// ──────────────────────────────────────────────────────────────────────────────
// IMPORTS
// ──────────────────────────────────────────────────────────────────────────────

import com.simplebooksapi.base.BaseTest;
// Parent class — gives us BASE_URL and requestSpec.

import com.simplebooksapi.utils.AuthHelper;
// Our utility class that handles API client registration and token caching.
// We call AuthHelper.getAccessToken(requestSpec) to get the Bearer token.

import io.restassured.RestAssured;
// REST Assured entry point — provides the .given() method to build HTTP requests.

import io.restassured.response.Response;
// The object returned after executing a request.
// Holds the status code, headers, and body of the HTTP response.
// We need it here to EXTRACT the orderId from the create-order response.

import org.testng.annotations.BeforeClass;
// @BeforeClass: runs ONCE before all @Test methods in this specific class.
// We use it to get the auth token once before any order test runs.

import org.testng.annotations.Test;
// @Test marks a method as a test case for TestNG to discover and run.

import java.util.HashMap;
// HashMap stores key-value pairs — used to build JSON request bodies.
// Example: {"bookId": 1, "customerName": "John Doe"}

import java.util.Map;
// Map is the interface type for HashMap. Using the interface is best practice
// because it makes code easier to swap or extend later.

import static org.hamcrest.Matchers.*;
// Static import of all Hamcrest matchers (equalTo, notNullValue, is, not, empty, etc.)

/**
 * OrdersTest — full CRUD lifecycle tests for the /orders endpoint.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * WHAT IS CRUD?
 * ──────────────────────────────────────────────────────────────────────────────
 * CRUD stands for: Create, Read, Update, Delete
 * These are the four basic operations any data resource should support.
 *
 * For the orders endpoint:
 *   Create → POST   /orders            (create a new order)
 *   Read   → GET    /orders            (list all orders)
 *          → GET    /orders/{id}       (get one order)
 *   Update → PATCH  /orders/{id}       (change part of an order)
 *   Delete → DELETE /orders/{id}       (remove an order)
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * WHY IS ORDER IMPORTANT HERE?
 * ──────────────────────────────────────────────────────────────────────────────
 * The tests are DEPENDENT on each other:
 *   1. Create gets us an orderId
 *   2. Read uses that orderId
 *   3. Update modifies that specific order
 *   4. Delete removes it
 *   5. Final GET confirms it's gone (404)
 *
 * If "Create" fails, every subsequent test would also fail because there's
 * no orderId to work with.
 *
 * We use TestNG's dependsOnMethods to express this dependency explicitly.
 * TestNG then:
 *   a) Runs tests in the correct order
 *   b) Automatically SKIPS any test whose dependency failed
 *      (so you see "SKIP" not "FAIL" in the report — more honest)
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * AUTH REQUIRED: Yes — every /orders endpoint needs a Bearer token.
 * We get the token once in @BeforeClass via AuthHelper.
 * ──────────────────────────────────────────────────────────────────────────────
 */
public class OrdersTest extends BaseTest {

    // ──────────────────────────────────────────────────────────────────────────
    // INSTANCE VARIABLES — shared state between test methods
    // ──────────────────────────────────────────────────────────────────────────

    // token: the Bearer authentication token.
    // Set by setupAuth() in @BeforeClass.
    // Used in every test via: .header("Authorization", "Bearer " + token)
    private String token;

    // orderId: the ID returned when we create an order in testCreateOrder().
    // All subsequent tests (Read, Update, Delete) need this specific ID.
    // By storing it as an instance variable, all test methods in this class
    // can read it.
    private String orderId;

    // ──────────────────────────────────────────────────────────────────────────
    // LIFECYCLE: GET AUTH TOKEN BEFORE ANY TESTS RUN
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * setupAuth() — fetches the Bearer token before any test runs.
     *
     * @BeforeClass: TestNG calls this ONCE before the first @Test method
     * in this class. The token is then available to all test methods below.
     *
     * Why not @BeforeSuite? Because only OrdersTest needs auth — StatusTest
     * and BooksTest don't. @BeforeClass keeps the setup scoped to this class.
     */
    @BeforeClass
    public void setupAuth() {
        // AuthHelper.getAccessToken() registers as an API client and returns
        // the Bearer token. It caches the token so it only registers ONCE
        // even if called multiple times across different test classes.
        token = AuthHelper.getAccessToken(requestSpec);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STEP 1: CREATE AN ORDER
    // ══════════════════════════════════════════════════════════════════════════

    @Test(description = "POST /orders — create a new order for bookId=1")
    public void testCreateOrder() {

        // Build the request body as a Java Map.
        // Jackson (via REST Assured) will serialise this to:
        //   { "bookId": 1, "customerName": "John Doe" }
        Map<String, Object> body = new HashMap<>();

        // bookId: must be an EXISTING and AVAILABLE book.
        // Book ID 1 is always available in the Simple Books API.
        // The Object type (not just String) because bookId is a number, not text.
        body.put("bookId", 1);

        // customerName: the name of the person placing the order.
        // Required field — omitting it will cause a 400 error (tested later).
        body.put("customerName", "John Doe");

        // We use .extract().response() to capture the FULL response object
        // instead of just asserting. We need it to extract the orderId.
        // Without .extract(), REST Assured performs assertions but discards the response.
        Response response = RestAssured.given()
                .spec(requestSpec)

                // "Authorization" header carries the Bearer token.
                // Format is always: "Bearer " + the token string.
                // The API reads this header to identify WHO is making the request.
                .header("Authorization", "Bearer " + token)

                // .body(body): attach the Map as the request body.
                // REST Assured uses Jackson to convert the Map to a JSON string.
                .body(body)

        .when()
                .post("/orders")   // POST https://simple-books-api.click/orders

        .then()
                // 201 = Created — the server successfully created a new resource.
                // POST creating new data should return 201, not 200.
                // 200 means "OK but I didn't necessarily create anything new".
                .statusCode(201)

                // "created": true — the API explicitly confirms creation in the body.
                .body("created", equalTo(true))

                // "orderId" must be present — we need it for all subsequent tests.
                .body("orderId", notNullValue())

                // .extract().response(): return the full response object so we can
                // read values from it outside the assertion chain.
                .extract().response();

        // Extract the orderId string from the JSON response body.
        // response.jsonPath().getString("orderId") navigates the JSON and returns
        // the value at key "orderId" as a Java String.
        // Example response: { "created": true, "orderId": "LkOMyoQFLRMs7zf" }
        orderId = response.jsonPath().getString("orderId");

        // Print to console so you can see the ID in the terminal output.
        System.out.println("[OrdersTest] Created Order ID: " + orderId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STEP 2: READ ALL ORDERS
    // ══════════════════════════════════════════════════════════════════════════

    @Test(
        description = "GET /orders — list all orders for this token",
        // dependsOnMethods = "testCreateOrder":
        // This test ONLY runs if testCreateOrder passed successfully.
        // If testCreateOrder failed (or was skipped), this test is automatically SKIPPED.
        // This is better than failing with a confusing "empty list" error.
        dependsOnMethods = "testCreateOrder"
    )
    public void testGetAllOrders() {
        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + token)  // Auth header required

        .when()
                .get("/orders")    // GET https://simple-books-api.click/orders

        .then()
                .statusCode(200)

                // The list should have at least the order we just created.
                // is(not(empty())) → the root array is not empty.
                .body("$", is(not(empty())));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STEP 3: READ ONE ORDER
    // ══════════════════════════════════════════════════════════════════════════

    @Test(
        description = "GET /orders/{orderId} — retrieve the specific order we created",
        dependsOnMethods = "testCreateOrder"  // Needs the orderId set in step 1
    )
    public void testGetOrderById() {
        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + token)

                // .pathParam("orderId", orderId): replaces {orderId} in the path
                // with the actual orderId value we saved from testCreateOrder.
                // Example: /orders/{orderId} → /orders/LkOMyoQFLRMs7zf
                .pathParam("orderId", orderId)

        .when()
                .get("/orders/{orderId}")

        .then()
                .statusCode(200)

                // Verify the returned order matches what we created.
                // "id" must equal the orderId we stored.
                .body("id", equalTo(orderId))

                // "bookId" must be 1 — the book we ordered.
                .body("bookId", equalTo(1))

                // "customerName" must be the name we provided in the create step.
                .body("customerName", equalTo("John Doe"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STEP 4: UPDATE THE ORDER
    // ══════════════════════════════════════════════════════════════════════════

    @Test(
        description = "PATCH /orders/{orderId} — change the customer name",
        // Depends on testGetOrderById to ensure we confirmed the order exists first.
        dependsOnMethods = "testGetOrderById"
    )
    public void testUpdateOrder() {

        // PATCH sends only the FIELDS YOU WANT TO CHANGE.
        // It is different from PUT which replaces the ENTIRE resource.
        // Here we only change customerName, not bookId or anything else.
        Map<String, String> body = new HashMap<>();
        body.put("customerName", "Jane Smith");   // New name, replacing "John Doe"

        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + token)
                .pathParam("orderId", orderId)    // Which order to update
                .body(body)                       // What to change

        .when()
                .patch("/orders/{orderId}")       // PATCH https://...orders/{orderId}

        .then()
                // 204 = No Content.
                // Means: "the operation succeeded, but there is no body to return."
                // This is the correct code for a successful PATCH/DELETE — the server
                // does not need to return the updated resource; it just confirms success.
                .statusCode(204);
    }

    @Test(
        description = "GET /orders/{orderId} — verify the name was changed to Jane Smith",
        dependsOnMethods = "testUpdateOrder"   // Only run if the PATCH succeeded
    )
    public void testVerifyOrderUpdate() {
        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + token)
                .pathParam("orderId", orderId)

        .when()
                .get("/orders/{orderId}")

        .then()
                .statusCode(200)

                // Assert the customerName was actually changed.
                // If the PATCH was silently ignored, this assertion would fail.
                // That's the point — we always VERIFY changes after making them.
                .body("customerName", equalTo("Jane Smith"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STEP 5: DELETE THE ORDER
    // ══════════════════════════════════════════════════════════════════════════

    @Test(
        description = "DELETE /orders/{orderId} — remove the order",
        dependsOnMethods = "testVerifyOrderUpdate"
    )
    public void testDeleteOrder() {
        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + token)
                .pathParam("orderId", orderId)

        .when()
                .delete("/orders/{orderId}")   // DELETE https://...orders/{orderId}

        .then()
                // 204 = No Content — deletion was successful.
                // Same code as PATCH: success with no response body.
                .statusCode(204);
    }

    @Test(
        description = "GET /orders/{orderId} — verify deleted order returns 404",
        dependsOnMethods = "testDeleteOrder"  // Only run after delete succeeds
    )
    public void testVerifyOrderDeleted() {
        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + token)
                .pathParam("orderId", orderId)

        .when()
                .get("/orders/{orderId}")

        .then()
                // 404 = Not Found — the order no longer exists.
                // This CONFIRMS the deletion actually worked.
                // If the server still returned 200, the delete would have been a no-op (bug).
                .statusCode(404);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NEGATIVE / SECURITY TESTS
    // ══════════════════════════════════════════════════════════════════════════
    // These tests verify the API's access control and input validation.
    // They do NOT depend on the CRUD chain above — they stand alone.

    // ──────────────────────────────────────────────────────────────────────────
    // NEGATIVE TEST 1: No authentication token
    // ──────────────────────────────────────────────────────────────────────────

    @Test(description = "POST /orders without token returns 401 Unauthorized")
    public void testCreateOrderWithoutToken() {
        Map<String, Object> body = new HashMap<>();
        body.put("bookId", 1);
        body.put("customerName", "Anonymous");

        RestAssured.given()
                .spec(requestSpec)
                // INTENTIONALLY no .header("Authorization", ...) here.
                // We want to test: "does the API reject unauthenticated requests?"
                .body(body)

        .when()
                .post("/orders")

        .then()
                // 401 = Unauthorized — the client did not provide valid credentials.
                // The server cannot verify WHO is making the request.
                // This is a security test — the API MUST reject requests with no token.
                .statusCode(401)

                // The error response should include an "error" field explaining why.
                // notNullValue() confirms the field exists.
                .body("error", notNullValue());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // NEGATIVE TEST 2: Missing required field
    // ──────────────────────────────────────────────────────────────────────────

    @Test(description = "POST /orders with missing customerName returns 400 Bad Request")
    public void testCreateOrderMissingCustomerName() {
        Map<String, Object> body = new HashMap<>();
        body.put("bookId", 1);
        // customerName is INTENTIONALLY OMITTED.
        // The API requires it — without it the order is incomplete.

        RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + token)  // Valid auth this time
                .body(body)                                   // But missing customerName

        .when()
                .post("/orders")

        .then()
                // 400 = Bad Request — the client sent invalid/incomplete data.
                // The server performs input validation and rejects incomplete requests
                // before attempting to save anything.
                .statusCode(400)

                // An error description must be in the response body.
                .body("error", notNullValue());
    }
}

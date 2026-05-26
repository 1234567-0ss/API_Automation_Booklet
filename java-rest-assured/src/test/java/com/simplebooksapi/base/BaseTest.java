// ──────────────────────────────────────────────────────────────────────────────
// PACKAGE DECLARATION
// ──────────────────────────────────────────────────────────────────────────────
// 'package' tells Java which folder/namespace this file belongs to.
// It must match the folder path:
//   com/simplebooksapi/base/ → package com.simplebooksapi.base
// Without this, the compiler cannot find classes that reference each other.
package com.simplebooksapi.base;

// ──────────────────────────────────────────────────────────────────────────────
// IMPORTS
// ──────────────────────────────────────────────────────────────────────────────
// 'import' makes a class from another package available BY SHORT NAME.
// Without imports you would have to write the full class name every time, e.g.:
//   io.restassured.RestAssured.given()  instead of just  RestAssured.given()

import io.restassured.RestAssured;
// RestAssured is the main entry point for the REST Assured library.
// It provides the static .given() method that starts every HTTP request chain.
// Example usage: RestAssured.given().when().get("/status").then().statusCode(200)

import io.restassured.builder.RequestSpecBuilder;
// RequestSpecBuilder is a BUILDER object — it lets you configure a request
// template step by step (base URL, headers, filters) and then produce a
// final, immutable RequestSpecification with .build().
// Pattern: Builder Pattern — collect settings → build once → reuse everywhere.

import io.restassured.filter.log.RequestLoggingFilter;
// A FILTER that intercepts every outgoing request and prints it to the console.
// Useful for debugging: you can see exactly what URL and headers were sent.
// Output example:
//   Request method: GET
//   Request URI:    https://simple-books-api.click/books
//   Headers:        Content-Type=application/json

import io.restassured.filter.log.ResponseLoggingFilter;
// A FILTER that intercepts every incoming response and prints it to the console.
// Useful for debugging: you can see the status code, headers, and body.
// Output example:
//   HTTP/1.1 200 OK
//   Content-Type: application/json
//   Body: [{"id":1,"name":"The Russian",...}]

import io.restassured.specification.RequestSpecification;
// This is the TYPE (interface) of the object that RequestSpecBuilder.build() returns.
// Think of it as a reusable "request template" — holds base URL, default headers,
// and logging filters. Passed into every test with .given().spec(requestSpec).

import org.testng.annotations.BeforeSuite;
// @BeforeSuite is a TestNG LIFECYCLE ANNOTATION.
// A method marked with @BeforeSuite runs automatically ONCE before the first
// @Test method in the entire suite. Great for one-time setup like this.
// Other lifecycle annotations (for reference):
//   @BeforeClass  → once before each test class
//   @BeforeMethod → before each individual @Test method
//   @AfterMethod  → after each individual @Test method
//   @AfterClass   → once after each test class
//   @AfterSuite   → once after all tests finish

/**
 * BaseTest — the parent class shared by all test classes.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * WHY DO WE HAVE A BASE CLASS?
 * ──────────────────────────────────────────────────────────────────────────
 * All three test files (StatusTest, BooksTest, OrdersTest) need the same
 * boilerplate:
 *   - The API's base URL
 *   - A shared Content-Type header (always JSON)
 *   - Logging so you can see requests/responses when debugging
 *
 * Instead of copying that setup into each file (DRY = Don't Repeat Yourself),
 * we put it in ONE place here and use Java INHERITANCE:
 *   public class StatusTest extends BaseTest { ... }
 *
 * 'extends BaseTest' means StatusTest INHERITS everything from BaseTest —
 * including the BASE_URL constant and the requestSpec variable.
 * ──────────────────────────────────────────────────────────────────────────
 */
public class BaseTest {

    // ──────────────────────────────────────────────────────────────────────────
    // CONSTANTS
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * BASE_URL — the root address of the API we are testing.
     *
     * KEYWORDS EXPLAINED:
     *   public  → any other class (including subclasses) can READ this value
     *   static  → belongs to the CLASS itself, not to any instance/object.
     *             There is only ONE copy of BASE_URL, shared by everyone.
     *   final   → the value CANNOT be changed after it is set here.
     *             This makes it a CONSTANT (convention: ALL_CAPS name).
     *   String  → the data type. This value is a text string.
     *
     * WHY static final?
     *   A URL never changes during a test run. Making it a static constant
     *   means it's set once at class loading time and available everywhere
     *   without needing to create a BaseTest object.
     */
    public static final String BASE_URL = "https://simple-books-api.click";

    // ──────────────────────────────────────────────────────────────────────────
    // SHARED REQUEST TEMPLATE
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * requestSpec — a reusable configuration template for every HTTP request.
     *
     * KEYWORDS EXPLAINED:
     *   protected → visible to this class AND all subclasses (StatusTest, BooksTest,
     *               OrdersTest that extend BaseTest). NOT visible to unrelated classes.
     *   static    → one shared instance for the whole test run. Every test class
     *               that extends BaseTest reads the SAME requestSpec object.
     *
     * HOW IT'S USED in test methods:
     *   RestAssured.given()
     *       .spec(requestSpec)   ← applies base URL + headers + logging from here
     *   .when()
     *       .get("/books")       ← the path is appended to the base URL
     *   .then()
     *       .statusCode(200);    ← assertion runs against the response
     *
     * It is null until setupSuite() runs. TestNG guarantees setupSuite()
     * runs before any @Test method, so requestSpec is always ready when needed.
     */
    protected static RequestSpecification requestSpec;

    // ──────────────────────────────────────────────────────────────────────────
    // LIFECYCLE METHOD: SUITE SETUP
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * setupSuite() — builds the shared request configuration.
     *
     * @BeforeSuite: TestNG calls this method AUTOMATICALLY, exactly ONCE,
     * before the very first @Test method in the entire suite begins.
     *
     * 'public void':
     *   public → TestNG needs to call it from outside this class, so it must be public
     *   void   → this method does not return any value; it just sets up requestSpec
     */
    @BeforeSuite
    public void setupSuite() {

        // new RequestSpecBuilder() creates a blank builder object.
        // We CHAIN configuration methods onto it one at a time.
        // Each method returns the SAME builder so we can keep chaining.
        // Finally, .build() produces the finished, immutable RequestSpecification.
        requestSpec = new RequestSpecBuilder()

                // setBaseUri(): every request path is APPENDED to this base URL.
                // Example: .get("/books") becomes GET https://simple-books-api.click/books
                // Without this, you'd have to write the full URL in every single test.
                .setBaseUri(BASE_URL)

                // setContentType(): adds the "Content-Type: application/json" header
                // to every request automatically.
                // This header tells the server: "I am sending data in JSON format."
                // The server uses this to know how to parse the request body.
                .setContentType("application/json")

                // addFilter(new RequestLoggingFilter()):
                // Registers a filter that prints every OUTGOING request to System.out.
                // Extremely useful when a test fails — you can see exactly what was sent.
                // Without this, you'd be guessing what the request looked like.
                .addFilter(new RequestLoggingFilter())

                // addFilter(new ResponseLoggingFilter()):
                // Registers a filter that prints every INCOMING response to System.out.
                // Shows you the status code, headers, and response body after each call.
                // Together with RequestLoggingFilter, you get a full request/response log.
                .addFilter(new ResponseLoggingFilter())

                // .build(): finalises the builder and creates the RequestSpecification.
                // After this call, the specification is IMMUTABLE — it cannot be changed.
                // The result is assigned to the static 'requestSpec' field above.
                .build();
    }
}

# Complete Guide to API Test Automation
### Simple Books API — Java · Python · TypeScript (Playwright)

---

# TABLE OF CONTENTS

1. [What is an API?](#1-what-is-an-api)
2. [What is REST?](#2-what-is-rest)
3. [HTTP Fundamentals](#3-http-fundamentals)
4. [Authentication — Bearer Tokens](#4-authentication--bearer-tokens)
5. [What is API Testing?](#5-what-is-api-testing)
6. [The Simple Books API — Explained](#6-the-simple-books-api--explained)
7. [Java — REST Assured + TestNG](#7-java--rest-assured--testng)
8. [Python — pytest + requests](#8-python--pytest--requests)
9. [TypeScript — Playwright API Tests](#9-typescript--playwright-api-tests)
10. [Test Design Principles](#10-test-design-principles)
11. [Cheat Sheets & Quick Reference](#11-cheat-sheets--quick-reference)

---

# 1. What is an API?

## The Simple Explanation

**API** stands for **Application Programming Interface**.

Think of it like a waiter in a restaurant:
- You (the client) sit at the table and want food.
- The kitchen (the server) has the food.
- You don't go into the kitchen yourself — the **waiter (API)** takes your order and brings back the food.

In software:
- Your app (client) needs data or wants to do something.
- A database or service (server) has that data.
- The **API** is the agreed way for your app to ask for it and get a response.

## Real-World Examples

| Service | What the API Does |
|---|---|
| Google Maps | Your app asks for directions → API returns a route |
| PayPal | Your shop sends payment request → API confirms/rejects |
| Weather App | Your phone asks current temp → API returns weather data |
| Simple Books API | Your test asks for books → API returns a JSON list of books |

## How an API Call Works (Step by Step)

```
YOUR PROGRAM                    SERVER (API)
     │                               │
     │  ── 1. Send HTTP Request ──►  │
     │     Method: GET               │
     │     URL: /books               │
     │     Headers: ...              │
     │                               │
     │  ◄── 2. Receive Response ──   │
     │     Status: 200               │
     │     Body: [{"id":1, ...}]     │
     │                               │
```

---

# 2. What is REST?

**REST** = **RE**presentational **S**tate **T**ransfer

It is a set of rules (an "architectural style") for designing APIs. When an API follows REST rules, it is called a **RESTful API**.

## The 4 Core REST Rules

### Rule 1: Resources are identified by URLs

Everything is a "resource" and has its own URL (address):

```
https://simple-books-api.click/books        ← the "books" resource (all books)
https://simple-books-api.click/books/1      ← one specific book (ID = 1)
https://simple-books-api.click/orders       ← the "orders" resource
https://simple-books-api.click/orders/abc   ← one specific order
```

### Rule 2: Use HTTP Methods to say WHAT you want to do

| HTTP Method | Meaning | Example |
|---|---|---|
| GET | Read / Fetch | Get list of books |
| POST | Create | Place a new order |
| PUT | Replace entirely | Replace an order completely |
| PATCH | Update partially | Change just the customer name |
| DELETE | Delete | Remove an order |

### Rule 3: Stateless — each request is independent

The server does NOT remember your previous request.
Every request must include all the information it needs (including the auth token).

### Rule 4: Use standard response codes (HTTP Status Codes)

The server tells you what happened using a number (explained in detail in Section 3).

---

# 3. HTTP Fundamentals

## 3.1 The Request — What You Send

Every HTTP request has these parts:

```
┌─────────────────────────────────────────────────────────────┐
│  REQUEST LINE                                               │
│  POST https://simple-books-api.click/orders HTTP/1.1        │
│                                                             │
│  HEADERS  (metadata about the request)                      │
│  Content-Type: application/json                             │
│  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR...          │
│  Accept: application/json                                   │
│                                                             │
│  BODY  (the data you are sending)                           │
│  {                                                          │
│    "bookId": 1,                                             │
│    "customerName": "John Doe"                               │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
```

| Part | Purpose | Example |
|---|---|---|
| **Method** | What action to take | `GET`, `POST`, `PATCH`, `DELETE` |
| **URL** | Which resource to act on | `https://simple-books-api.click/orders` |
| **Headers** | Extra info (auth, content type) | `Authorization: Bearer abc123` |
| **Body** | Data sent to the server | `{"bookId": 1, "customerName": "John"}` |

> **GET** and **DELETE** requests usually have NO body.
> **POST** and **PATCH** requests usually have a body.

## 3.2 The Response — What You Receive

Every HTTP response has these parts:

```
┌─────────────────────────────────────────────────────────────┐
│  STATUS LINE                                                │
│  HTTP/1.1 201 Created                                       │
│                                                             │
│  HEADERS                                                    │
│  Content-Type: application/json                             │
│  Date: Fri, 23 May 2026 12:00:00 GMT                        │
│                                                             │
│  BODY  (the data returned by the server)                    │
│  {                                                          │
│    "created": true,                                         │
│    "orderId": "PF6MflPDcuhWobZcgmJy5"                       │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
```

## 3.3 HTTP Status Codes — The Full Picture

Status codes are 3-digit numbers. The first digit tells you the category:

```
1xx → Informational  (rarely seen in testing)
2xx → SUCCESS        ← you want these
3xx → Redirect       (the resource moved somewhere else)
4xx → CLIENT ERROR   ← your request was wrong
5xx → SERVER ERROR   ← the server crashed or failed
```

### The Codes You Will See in These Tests

| Code | Name | Meaning | When You See It |
|---|---|---|---|
| **200** | OK | Request succeeded, body contains the data | GET requests that worked |
| **201** | Created | A new resource was created successfully | POST /orders after creating an order |
| **204** | No Content | Success, but no response body | PATCH and DELETE (they work but return nothing) |
| **400** | Bad Request | You sent invalid or missing data | Missing `customerName` in order |
| **401** | Unauthorized | No token or invalid token | Calling /orders without Authorization header |
| **403** | Forbidden | Token is valid but you don't have permission | Accessing another client's orders |
| **404** | Not Found | The resource doesn't exist | Getting book with ID 99999 |
| **409** | Conflict | Duplicate resource conflict | Registering the same email twice |
| **500** | Internal Server Error | The server crashed | Server-side bug (not your fault) |

## 3.4 Headers — The Metadata Layer

Headers are key-value pairs sent with every request or response.

### Request Headers You Will Use

```
Content-Type: application/json
  → Tells the server: "The body I'm sending is JSON format"

Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
  → Proves you are authenticated. The long string is the token.

Accept: application/json
  → Tells the server: "Please send back JSON (not XML or HTML)"
```

### Response Headers You Will See

```
Content-Type: application/json; charset=utf-8
  → Confirms the body is JSON

Content-Length: 87
  → How many bytes the body contains
```

## 3.5 JSON — The Data Format

Almost all modern REST APIs use **JSON** (JavaScript Object Notation) to send data.

JSON has two main structures:

**Object** — key/value pairs, surrounded by `{ }`:
```json
{
  "id": 1,
  "name": "The Russian",
  "type": "fiction",
  "available": true
}
```

**Array** — an ordered list, surrounded by `[ ]`:
```json
[
  { "id": 1, "name": "The Russian" },
  { "id": 2, "name": "Just as I Am" },
  { "id": 3, "name": "The Vanishing Half" }
]
```

**JSON Data Types:**

| Type | Example |
|---|---|
| String | `"John Doe"` (in quotes) |
| Number | `1`, `3.14` (no quotes) |
| Boolean | `true`, `false` |
| Null | `null` |
| Object | `{ "key": "value" }` |
| Array | `[1, 2, 3]` |

---

# 4. Authentication — Bearer Tokens

## Why Authentication is Needed

Without authentication, anyone could:
- See your orders
- Delete your orders
- Create fake orders

Authentication proves **who you are** so the API only gives you YOUR data.

## How Bearer Token Authentication Works

```
STEP 1: Register as an API client
────────────────────────────────
You   → POST /api-clients/
        Body: { "clientName": "MyApp", "clientEmail": "me@example.com" }

API   → 201 Created
        Body: { "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." }

        ↑ This long string is your TOKEN. Save it.

STEP 2: Use the token in every protected request
─────────────────────────────────────────────────
You   → POST /orders
        Header: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
        Body: { "bookId": 1, "customerName": "John" }

API   → 201 Created  ✓ (because your token is valid)

Without the token:
You   → POST /orders
        (no Authorization header)

API   → 401 Unauthorized  ✗
```

## What is "Bearer"?

The word **Bearer** means "the person holding this token". The format is:
```
Authorization: Bearer <your-token-here>
```

The `Bearer` prefix is required — the API rejects the request without it.

## Why We Use UUID/Unique Email

If you try to register the same email twice, the API returns **409 Conflict**.
To avoid this during test runs, we generate a unique email every time:

```java
// Java
String uniqueId = UUID.randomUUID().toString().substring(0, 8);
// Result: "a3f7c2d1" — different every run

// Python
unique_id = str(uuid.uuid4())[:8]

// TypeScript
const uniqueId = Date.now().toString(36);  // e.g. "lxyz123"
```

---

# 5. What is API Testing?

## Definition

API testing means **sending HTTP requests to an API and checking the response** is correct.

Unlike UI testing (which clicks buttons and reads screens), API testing:
- Talks directly to the server
- Is **faster** (no browser)
- Is **more stable** (no UI changes break it)
- Validates the actual **business logic**

## What We Check in API Tests

For every test we typically verify:

| Check | Example |
|---|---|
| **Status code** | `statusCode == 201` |
| **Response body fields** | `body.created == true` |
| **Field values** | `body.customerName == "John Doe"` |
| **Field presence** | `body` has an `orderId` key |
| **Data type** | `body.id` is a number |
| **Error messages** | `body.error` exists when 400 |

## Types of Tests

### Happy Path Tests
The "normal" case — valid data, correct auth, expected to succeed.
```
POST /orders with valid bookId + customerName → 201 Created
```

### Negative Tests
Deliberately wrong input — expected to fail gracefully.
```
POST /orders with no auth token           → 401 Unauthorized
POST /orders with missing customerName    → 400 Bad Request
GET  /books/99999 (doesn't exist)         → 404 Not Found
```

### Boundary Tests
Edge of valid input range.
```
GET /books?limit=1   → minimum allowed
GET /books?limit=20  → maximum allowed
GET /books?limit=0   → below minimum → 400
GET /books?limit=21  → above maximum → 400
```

### CRUD Lifecycle Test
Testing the full Create → Read → Update → Delete flow:
```
1. POST   /orders                → Create, save orderId
2. GET    /orders/{orderId}      → Read, verify data matches
3. PATCH  /orders/{orderId}      → Update, verify 204
4. GET    /orders/{orderId}      → Read again, verify update applied
5. DELETE /orders/{orderId}      → Delete, verify 204
6. GET    /orders/{orderId}      → Read again, verify 404 (gone)
```

---

# 6. The Simple Books API — Explained

## Base URL
```
https://simple-books-api.click
```

## All Endpoints with Request/Response Examples

### GET /status — Health Check

```
Request:
  GET https://simple-books-api.click/status

Response (200 OK):
  {
    "status": "OK"
  }
```

No headers needed. Just checks if the API is running.

---

### GET /books — List Books

```
Request:
  GET https://simple-books-api.click/books
  GET https://simple-books-api.click/books?type=fiction
  GET https://simple-books-api.click/books?type=non-fiction&limit=3

Response (200 OK):
  [
    { "id": 1, "name": "The Russian",       "type": "fiction",     "available": true  },
    { "id": 2, "name": "Just as I Am",      "type": "non-fiction", "available": false },
    { "id": 3, "name": "The Vanishing Half","type": "fiction",     "available": true  }
  ]
```

**Query Parameters** (optional, added with `?`):

| Parameter | Valid Values | Description |
|---|---|---|
| `type` | `fiction`, `non-fiction` | Filter by book type |
| `limit` | 1 to 20 | Maximum number of results |

---

### GET /books/{bookId} — Get One Book

```
Request:
  GET https://simple-books-api.click/books/1

Response (200 OK):
  {
    "id": 1,
    "name": "The Russian",
    "author": "James Patterson and James O. Born",
    "isbn": "1780899475",
    "type": "fiction",
    "price": 12.98,
    "current-stock": 12,
    "available": true
  }

If not found:
  404 Not Found
  { "error": "No book with id 99999" }
```

---

### POST /api-clients/ — Register & Get Token

```
Request:
  POST https://simple-books-api.click/api-clients/
  Content-Type: application/json

  {
    "clientName": "MyTestApp",
    "clientEmail": "myapp@example.com"
  }

Response (201 Created):
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGll..."
  }

If same email registered again:
  409 Conflict
  { "error": "API client already registered. Try a different email." }
```

---

### POST /orders — Create an Order (AUTH REQUIRED)

```
Request:
  POST https://simple-books-api.click/orders
  Content-Type: application/json
  Authorization: Bearer <your-token>

  {
    "bookId": 1,
    "customerName": "John Doe"
  }

Response (201 Created):
  {
    "created": true,
    "orderId": "PF6MflPDcuhWobZcgmJy5"
  }

If no token:
  401 Unauthorized
  { "error": "Missing Authorization header." }

If missing field:
  400 Bad Request
  { "error": "customerName" }
```

---

### GET /orders — List All Orders (AUTH REQUIRED)

```
Request:
  GET https://simple-books-api.click/orders
  Authorization: Bearer <your-token>

Response (200 OK):
  [
    {
      "id": "PF6MflPDcuhWobZcgmJy5",
      "bookId": 1,
      "customerName": "John Doe",
      "createdBy": "...",
      "quantity": 1,
      "timestamp": 1716400000000
    }
  ]
```

---

### GET /orders/{orderId} — Get One Order (AUTH REQUIRED)

```
Request:
  GET https://simple-books-api.click/orders/PF6MflPDcuhWobZcgmJy5
  Authorization: Bearer <your-token>

Response (200 OK):
  {
    "id": "PF6MflPDcuhWobZcgmJy5",
    "bookId": 1,
    "customerName": "John Doe",
    "createdBy": "...",
    "quantity": 1,
    "timestamp": 1716400000000
  }
```

---

### PATCH /orders/{orderId} — Update Order (AUTH REQUIRED)

```
Request:
  PATCH https://simple-books-api.click/orders/PF6MflPDcuhWobZcgmJy5
  Content-Type: application/json
  Authorization: Bearer <your-token>

  {
    "customerName": "Jane Smith"
  }

Response (204 No Content):
  [empty body — no content returned]
```

> **204 No Content** means success. The server processed your request but has nothing to return.

---

### DELETE /orders/{orderId} — Delete Order (AUTH REQUIRED)

```
Request:
  DELETE https://simple-books-api.click/orders/PF6MflPDcuhWobZcgmJy5
  Authorization: Bearer <your-token>

Response (204 No Content):
  [empty body]
```

---

# 7. Java — REST Assured + TestNG

## 7.1 What is REST Assured?

**REST Assured** is a Java library that makes it easy to test REST APIs. It provides a fluent DSL (Domain Specific Language) — meaning the code reads almost like plain English.

```java
// This is what a REST Assured request looks like:
given()
    .header("Authorization", "Bearer abc123")
    .body(orderData)
.when()
    .post("/orders")
.then()
    .statusCode(201)
    .body("created", equalTo(true));
```

Reading this out loud: *"Given a header and body, when I POST to /orders, then the status code should be 201 and the 'created' field should equal true."*

## 7.2 What is TestNG?

**TestNG** is a test framework for Java (similar to JUnit). It:
- Discovers and runs test methods annotated with `@Test`
- Controls test execution order and dependencies
- Supports parallel test execution
- Generates reports

### Key TestNG Annotations

| Annotation | When It Runs | Used For |
|---|---|---|
| `@BeforeSuite` | Once, before ALL tests | Setup that applies to everything |
| `@BeforeClass` | Once, before tests in THIS class | Class-level setup (e.g., get a token) |
| `@BeforeMethod` | Before EACH test method | Per-test setup |
| `@Test` | Marks a method as a test | Every test you write |
| `@AfterMethod` | After EACH test method | Per-test cleanup |
| `@AfterClass` | After tests in THIS class end | Class-level cleanup |
| `@AfterSuite` | Once, after ALL tests | Final cleanup |

## 7.3 Project Structure Explained

```
java-rest-assured/
│
├── pom.xml                    ← Maven configuration (like a shopping list of libraries)
├── testng.xml                 ← Test suite definition (which tests run, in what order)
│
└── src/test/java/com/simplebooksapi/
    ├── base/
    │   └── BaseTest.java      ← Shared setup for all test classes
    ├── utils/
    │   └── AuthHelper.java    ← Handles token registration and caching
    └── tests/
        ├── StatusTest.java    ← Tests for GET /status
        ├── BooksTest.java     ← Tests for GET /books and GET /books/{id}
        └── OrdersTest.java    ← Tests for all /orders operations
```

## 7.4 File-by-File Deep Dive

### pom.xml — The Dependency Manager

Maven (`pom.xml`) is like a shopping list for your Java project. Instead of manually downloading JAR files, you declare what you need and Maven downloads it.

```xml
<!-- REST Assured: sends HTTP requests and validates responses -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>5.4.0</version>
    <scope>test</scope>     ← only needed for tests, not production
</dependency>

<!-- TestNG: the test runner and organiser -->
<dependency>
    <groupId>org.testng</groupId>
    <artifactId>testng</artifactId>
    <version>7.9.0</version>
    <scope>test</scope>
</dependency>

<!-- Jackson: converts Java objects (Maps) into JSON strings -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.16.1</version>
</dependency>
```

**Why do we need Jackson?**
When you do `.body(myMap)` in REST Assured, it needs to convert your Java `HashMap` into a JSON string. Jackson does that conversion.

---

### BaseTest.java — The Foundation

Every test class extends `BaseTest`. Think of it as a **template** that all tests inherit from.

```java
public class BaseTest {

    // BASE_URL: the root of the API — all other tests just add "/books" or "/orders"
    public static final String BASE_URL = "https://simple-books-api.click";

    // requestSpec: a "pre-configured" template for requests
    // Instead of repeating the base URL and Content-Type in every test,
    // we configure it ONCE here and reuse it everywhere.
    protected static RequestSpecification requestSpec;

    @BeforeSuite
    public void setupSuite() {
        requestSpec = new RequestSpecBuilder()
                .setBaseUri(BASE_URL)
                .setContentType("application/json")
                .addFilter(new RequestLoggingFilter())   // print requests to console
                .addFilter(new ResponseLoggingFilter())  // print responses to console
                .build();
    }
}
```

**What is `RequestSpecification`?**
It is a "saved configuration" for requests. Instead of:
```java
// WITHOUT requestSpec (repetitive):
given()
    .baseUri("https://simple-books-api.click")
    .contentType("application/json")
    .header("Accept", "application/json")
    .post("/orders");

// WITH requestSpec (clean):
given()
    .spec(requestSpec)
    .post("/orders");
```

---

### AuthHelper.java — Token Management

This is one of the most important utility classes. It handles the authentication flow.

```java
public class AuthHelper {

    // static = shared across ALL instances of this class
    // This means the token is registered ONCE per test run, no matter how many
    // test classes call getAccessToken().
    private static String accessToken;

    public static String getAccessToken(RequestSpecification spec) {

        // If we already have a token, return it immediately (don't re-register)
        if (accessToken != null) {
            return accessToken;
        }

        // Generate unique email to avoid 409 Conflict
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        // Build the request body as a Map (Jackson will convert this to JSON)
        Map<String, String> body = new HashMap<>();
        body.put("clientName", "TestClient_" + uniqueId);
        body.put("clientEmail", "testclient_" + uniqueId + "@example.com");

        // Send the POST request and validate the response
        Response response = RestAssured.given()
                .spec(spec)
                .body(body)
                .post("/api-clients/");

        // This will throw an AssertionError if status is NOT 201
        response.then().statusCode(201);

        // jsonPath().getString("accessToken") navigates the JSON response:
        // { "accessToken": "eyJ..." }  →  returns "eyJ..."
        accessToken = response.jsonPath().getString("accessToken");

        return accessToken;
    }
}
```

**What is `jsonPath()`?**
JSON Path is a query language for JSON (like XPath for XML). It lets you extract values from JSON by writing a path expression:

```json
{ "order": { "id": "abc", "book": { "name": "The Russian" } } }
```
```java
response.jsonPath().getString("order.id")         // returns "abc"
response.jsonPath().getString("order.book.name")  // returns "The Russian"
```

---

### StatusTest.java — The Simplest Test

```java
public class StatusTest extends BaseTest {
    // ↑ extends BaseTest means this class inherits requestSpec and BASE_URL

    @Test(description = "API health-check returns HTTP 200 and status OK")
    public void testApiStatusIsUp() {
        RestAssured.given()
                .spec(requestSpec)   // use the shared config (base URL, content type, logging)
        .when()
                .get("/status")      // send GET to https://simple-books-api.click/status
        .then()
                .statusCode(200)                    // assert HTTP status is 200
                .body("status", equalTo("OK"));     // assert JSON body has status=OK
    }
}
```

**How does `.body("status", equalTo("OK"))` work?**
- `"status"` is a JSON Path expression — it navigates to the `status` key in the response body
- `equalTo("OK")` is a Hamcrest matcher — it checks the value equals "OK"
- If the assertion fails, the test fails with a clear error message

---

### BooksTest.java — Query Parameters and Path Parameters

```java
// QUERY PARAMETER: added to the URL with ?key=value
// GET /books?type=fiction
RestAssured.given()
        .spec(requestSpec)
        .queryParam("type", "fiction")   // adds ?type=fiction to the URL
.when()
        .get("/books")
.then()
        .statusCode(200)
        .body("type", everyItem(equalTo("fiction")));
//              ↑             ↑
//         JSON path    Hamcrest matcher
//         "type" in each array item    "every item in the list should equal 'fiction'"
```

```java
// PATH PARAMETER: part of the URL itself
// GET /books/1
RestAssured.given()
        .spec(requestSpec)
        .pathParam("bookId", 1)         // stores value 1 for {bookId} placeholder
.when()
        .get("/books/{bookId}")         // {bookId} is replaced with 1 → /books/1
.then()
        .statusCode(200)
        .body("id", equalTo(1));
```

**Difference between queryParam and pathParam:**
```
queryParam("type", "fiction")  →  /books?type=fiction     (after the ?)
pathParam("bookId", 1)         →  /books/1                (inside the URL)
```

---

### OrdersTest.java — The Full CRUD Lifecycle

This is the most complex test class. Key concepts:

**`@BeforeClass` — runs once before all tests in the class:**
```java
@BeforeClass
public void setupAuth() {
    // Get the token ONCE before any order test runs
    token = AuthHelper.getAccessToken(requestSpec);
}
```

**`dependsOnMethods` — test chaining:**
```java
@Test(description = "Create order")
public void testCreateOrder() {
    // ... creates order, saves orderId
    orderId = response.jsonPath().getString("orderId");
}

@Test(dependsOnMethods = "testCreateOrder")  // ← won't run if testCreateOrder failed
public void testGetOrderById() {
    // uses orderId that was set in testCreateOrder
}
```

**The Authorization header:**
```java
RestAssured.given()
        .spec(requestSpec)
        .header("Authorization", "Bearer " + token)  // ← every order request needs this
        .body(body)
.when()
        .post("/orders")
```

**204 No Content responses (PATCH and DELETE):**
```java
.then()
    .statusCode(204);  // ← IMPORTANT: do NOT try to read the body — there is none
```

---

### testng.xml — Suite Configuration

```xml
<suite name="Simple Books API" parallel="classes" thread-count="3">
```

| Attribute | Value | Meaning |
|---|---|---|
| `parallel` | `"classes"` | Each test class can run in its own thread |
| `thread-count` | `3` | Up to 3 classes run simultaneously |
| `verbose` | `2` | How much detail to print to console (0–10) |

---

## 7.5 How to Run (Java)

```bash
# Navigate to the java project folder
cd d:\Downloads\simple-books-api-tests\java-rest-assured

# Run all tests
mvn test

# Run a specific test class only
mvn test -Dtest=BooksTest

# Run a specific test method
mvn test -Dtest=BooksTest#testGetAllBooks
```

**What you see in the console:**
```
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

# 8. Python — pytest + requests

## 8.1 What is `requests`?

**requests** is a Python library for sending HTTP requests. It is one of the most popular Python libraries ever written.

```python
import requests

# GET request
response = requests.get("https://simple-books-api.click/books")

# POST request with JSON body
response = requests.post(
    "https://simple-books-api.click/orders",
    json={"bookId": 1, "customerName": "John"},    # json= auto-sets Content-Type header
    headers={"Authorization": "Bearer abc123"}
)

# Read the response
print(response.status_code)   # 201
print(response.json())        # {'created': True, 'orderId': 'abc123'}
```

### The Response Object

```python
response = requests.get("https://simple-books-api.click/books")

response.status_code           # integer: 200
response.text                  # raw string: '[{"id": 1, "name": ...}]'
response.json()                # parsed Python object (list or dict)
response.headers               # dict of headers: {'Content-Type': 'application/json', ...}
response.ok                    # True if status_code < 400
```

## 8.2 What is pytest?

**pytest** is Python's most popular test framework. It:
- Automatically finds test files (any file starting with `test_`)
- Automatically finds test functions (any function starting with `test_`)
- Provides a powerful fixture system for setup/teardown
- Generates detailed failure reports

### pytest Discovery Rules

```
tests/
├── test_status.py      ← FOUND (starts with test_)
├── test_books.py       ← FOUND
├── test_orders.py      ← FOUND
└── helper.py           ← IGNORED (doesn't start with test_)
```

Inside each file:
```python
class TestBooks:              ← FOUND (starts with Test)
    def test_get_books(self): ← FOUND (starts with test_)
        ...

    def helper_method(self):  ← IGNORED (doesn't start with test_)
        ...
```

## 8.3 Fixtures — The Most Important pytest Concept

A **fixture** is a function that **provides a value** to tests.

Instead of writing setup code in every test, you write it once as a fixture, and tests simply declare they need it.

```python
# Without fixtures (repetitive — BAD):
def test_get_books():
    BASE_URL = "https://simple-books-api.click"  # repeated everywhere
    response = requests.get(f"{BASE_URL}/books")
    assert response.status_code == 200

def test_get_status():
    BASE_URL = "https://simple-books-api.click"  # repeated again
    response = requests.get(f"{BASE_URL}/status")
    assert response.status_code == 200
```

```python
# With fixtures (clean — GOOD):
# conftest.py
@pytest.fixture(scope="session")
def base_url():
    return "https://simple-books-api.click"

# tests/test_books.py
def test_get_books(base_url):          # pytest sees 'base_url' and injects it
    response = requests.get(f"{base_url}/books")
    assert response.status_code == 200

def test_get_status(base_url):         # same fixture, used again
    response = requests.get(f"{base_url}/status")
    assert response.status_code == 200
```

### Fixture Scope — The Lifecycle

```python
@pytest.fixture(scope="function")  # DEFAULT: runs fresh for EVERY test
@pytest.fixture(scope="class")     # runs once for each TestClass
@pytest.fixture(scope="module")    # runs once per .py file
@pytest.fixture(scope="session")   # runs ONCE for the entire test run
```

**Why does scope matter for tokens?**
```python
# scope="session" means this runs ONCE total:
@pytest.fixture(scope="session")
def auth_token(base_url):
    # Register one client → get one token → reuse for all 7+ order tests
    response = requests.post(f"{base_url}/api-clients/", json={...})
    return response.json()["accessToken"]

# scope="function" would mean:
# → register a new client before EVERY test
# → 7 tests × 1 registration = 7 API clients created
# → wasteful and could hit rate limits
```

### Fixture Chaining — Fixtures Using Other Fixtures

```python
@pytest.fixture(scope="session")
def auth_token(base_url):           # uses base_url fixture
    ...
    return token

@pytest.fixture(scope="session")
def auth_headers(auth_token):       # uses auth_token fixture
    return {"Authorization": f"Bearer {auth_token}"}

# In a test:
def test_create_order(base_url, auth_headers):  # pytest resolves the whole chain
    ...
```

### conftest.py — The Special File

`conftest.py` is a special file that pytest automatically loads. Fixtures defined here are available to **all test files** in the same folder and subfolders.

```
python-pytest/
├── conftest.py            ← fixtures here are available to ALL tests
└── tests/
    ├── test_status.py     ← can use fixtures from conftest.py
    ├── test_books.py      ← can use fixtures from conftest.py
    └── test_orders.py     ← can use fixtures from conftest.py
```

## 8.4 File-by-File Deep Dive

### conftest.py — The Setup Hub

```python
import pytest
import requests
import uuid

BASE_URL = "https://simple-books-api.click"

@pytest.fixture(scope="session")
def base_url():
    return BASE_URL

@pytest.fixture(scope="session")
def auth_token(base_url):
    """
    What happens here:
    1. Generate a unique email using uuid4()[:8] — e.g. "a3f7c2d1"
    2. POST to /api-clients/ with that email
    3. Receive { "accessToken": "eyJ..." }
    4. Return just the token string
    5. pytest caches this for the entire session (scope="session")
    """
    unique_id = str(uuid.uuid4())[:8]
    payload = {
        "clientName": f"TestClient_{unique_id}",
        "clientEmail": f"testclient_{unique_id}@example.com",
    }
    response = requests.post(f"{base_url}/api-clients/", json=payload)
    assert response.status_code == 201, f"Registration failed: {response.text}"
    return response.json()["accessToken"]

@pytest.fixture(scope="session")
def auth_headers(auth_token):
    return {"Authorization": f"Bearer {auth_token}"}
```

---

### test_status.py — The Simplest Tests

```python
import requests

class TestStatus:

    def test_api_is_up(self, base_url):
        """
        self = refers to the TestStatus class instance (Python class convention)
        base_url = pytest injects the base_url fixture value here
        """
        response = requests.get(f"{base_url}/status")
        # f-string: f"{base_url}/status" → "https://simple-books-api.click/status"

        assert response.status_code == 200, \
            f"Expected 200 but got {response.status_code}"
        #   ↑ If False, test FAILS with this message

        data = response.json()  # parse JSON string to Python dict
        assert data["status"] == "OK"
        #      data = {"status": "OK"}
        #      data["status"] = "OK"
        #      "OK" == "OK" → True → test passes
```

---

### test_books.py — Query Parameters and Assertions

```python
def test_filter_books_by_fiction(self, base_url):
    response = requests.get(
        f"{base_url}/books",
        params={"type": "fiction"}   # adds ?type=fiction to the URL
    )
    # URL becomes: https://simple-books-api.click/books?type=fiction

    books = response.json()  # This is a Python list: [{"id":1,...}, {"id":3,...}]

    for book in books:                    # loop through each book in the list
        assert book["type"] == "fiction"  # every book must be type fiction
```

```python
def test_get_single_book_by_id(self, base_url):
    response = requests.get(f"{base_url}/books/1")  # /books/1 directly in URL

    book = response.json()  # This is a Python dict (single object)
    # {
    #   "id": 1,
    #   "name": "The Russian",
    #   "author": "James Patterson",
    #   "available": true
    # }

    assert book["id"] == 1
    assert "name" in book      # check the key EXISTS (not the value)
    assert "author" in book
```

---

### test_orders.py — CRUD with Class State

```python
class TestOrders:

    # Class variable — accessible to all methods as TestOrders.order_id
    # This is how we share the created orderId between test methods
    order_id = None

    def test_01_create_order(self, base_url, auth_headers):
        payload = {
            "bookId": 1,
            "customerName": "John Doe"
        }
        response = requests.post(
            f"{base_url}/orders",
            json=payload,         # json= converts dict to JSON + sets Content-Type
            headers=auth_headers  # {"Authorization": "Bearer eyJ..."}
        )

        assert response.status_code == 201

        data = response.json()
        # data = {"created": True, "orderId": "PF6MflPDcuhWobZcgmJy5"}

        assert data.get("created") is True  # .get() is safer than data["created"]
        assert "orderId" in data

        # Save the orderId for all other test methods to use
        TestOrders.order_id = data["orderId"]


    def test_04_update_order(self, base_url, auth_headers):
        response = requests.patch(
            f"{base_url}/orders/{TestOrders.order_id}",
            json={"customerName": "Jane Smith"},
            headers=auth_headers
        )

        # PATCH returns 204 — no response body
        assert response.status_code == 204
        # DO NOT call response.json() here — there is no body → would raise an error
```

## 8.5 How to Run (Python)

```bash
# Navigate to the python project folder
cd d:\Downloads\simple-books-api-tests\python-pytest

# Install dependencies
pip install -r requirements.txt

# Run all tests
pytest

# Run with verbose output (show each test name)
pytest -v

# Run a specific file
pytest tests/test_orders.py -v

# Run a specific test method
pytest tests/test_orders.py::TestOrders::test_01_create_order -v

# Generate an HTML report (opens report.html in the folder)
pytest --html=report.html

# Run and stop on first failure
pytest -x
```

**What you see:**
```
tests/test_status.py::TestStatus::test_api_is_up          PASSED
tests/test_status.py::TestStatus::test_response_is_json   PASSED
tests/test_books.py::TestBooks::test_get_all_books        PASSED
...
9 passed in 3.41s
```

---

# 9. TypeScript — Playwright API Tests

## 9.1 What is Playwright?

**Playwright** is a modern test automation framework built by Microsoft. Most people know it for **browser automation** (clicking buttons, filling forms), but it also has a powerful **built-in HTTP client** for API testing.

Why use Playwright for API tests?
- Built-in request context — no need to install `axios` or `node-fetch`
- Same tool for UI AND API tests
- Built-in HTML reporter (beautiful visual reports)
- Native TypeScript support
- Parallel test execution out of the box

## 9.2 TypeScript Basics (for Beginners)

TypeScript is JavaScript with **type annotations** — you declare what type a variable holds.

```typescript
// JavaScript (no types):
let token = "abc123";
let orderId;  // could be anything

// TypeScript (with types):
let token: string = "abc123";  // must always be a string
let orderId: string;           // will be assigned later, but must be a string

// This catches mistakes at compile time:
token = 42;  // ERROR: Type 'number' is not assignable to type 'string'
```

### Async/Await — How to Handle HTTP Requests

All HTTP calls in Playwright are **asynchronous** (they take time to complete). We use `async/await` to wait for them:

```typescript
// WITHOUT await (WRONG — response is a Promise, not the actual response):
const response = request.get('/status');
console.log(response.status()); // ERROR: Promise has no .status() method

// WITH await (CORRECT — waits for the HTTP call to finish):
const response = await request.get('/status');
console.log(response.status()); // 200  ✓
```

Any function that uses `await` must be declared as `async`:
```typescript
test('my test', async ({ request }) => {  // ← 'async' is required
    const response = await request.get('/status');  // ← 'await' waits for HTTP
    expect(response.status()).toBe(200);
});
```

## 9.3 Key Playwright Concepts

### The `request` Fixture

Playwright's `request` fixture is an HTTP client. It is automatically provided to every `test()` function — you just declare it in the parameter list:

```typescript
test('my test', async ({ request }) => {
    //                    ↑ Playwright provides this — you don't instantiate it
    const response = await request.get('/status');
});
```

### Making HTTP Requests

```typescript
// GET
const response = await request.get('/books');
const response = await request.get('/books', {
    params: { type: 'fiction', limit: 3 }   // adds ?type=fiction&limit=3
});

// POST with JSON body
const response = await request.post('/orders', {
    headers: { Authorization: `Bearer ${token}` },
    data: { bookId: 1, customerName: 'John Doe' }  // data= is automatically JSON-serialised
});

// PATCH
const response = await request.patch(`/orders/${orderId}`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { customerName: 'Jane Smith' }
});

// DELETE
const response = await request.delete(`/orders/${orderId}`, {
    headers: { Authorization: `Bearer ${token}` }
});
```

### Reading the Response

```typescript
response.status()          // number: 200
response.ok()              // boolean: true if status < 400
response.headers()         // object: { 'content-type': 'application/json', ... }
await response.json()      // parsed JS object (must await)
await response.text()      // raw string body (must await)
```

### Assertions with `expect()`

```typescript
expect(response.status()).toBe(201);                    // exact equality
expect(response.status()).not.toBe(400);                // negation
expect(books.length).toBeGreaterThan(0);                // greater than
expect(books.length).toBeLessThanOrEqualTo(2);          // less than or equal
expect(body).toHaveProperty('orderId');                 // key exists
expect(body.created).toBe(true);                        // boolean check
expect(Array.isArray(books)).toBe(true);                // type check
expect(order.customerName).toBe('Jane Smith');          // string equality
```

## 9.4 Project Structure Explained

```
playwright-typescript/
│
├── package.json           ← npm dependencies and run scripts
├── tsconfig.json          ← TypeScript compiler settings
├── playwright.config.ts   ← Playwright settings (base URL, parallel, reporter)
│
├── utils/
│   └── auth.ts            ← Token registration and caching
│
└── tests/
    └── api/
        ├── status.spec.ts  ← Tests for GET /status
        ├── books.spec.ts   ← Tests for GET /books
        └── orders.spec.ts  ← Full CRUD tests (serial mode)
```

## 9.5 File-by-File Deep Dive

### playwright.config.ts — The Control Centre

```typescript
import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',          // where to find test files

  fullyParallel: true,         // all tests run in parallel by default
                               // (orders.spec.ts overrides this with serial mode)

  reporter: [
    ['html', { open: 'never' }],  // creates playwright-report/index.html
    ['list'],                      // prints results in terminal
  ],

  use: {
    baseURL: 'https://simple-books-api.click',
    // ↑ This means request.get('/books') automatically becomes
    //   request.get('https://simple-books-api.click/books')

    extraHTTPHeaders: {
      'Content-Type': 'application/json',  // sent with EVERY request
      'Accept': 'application/json',
    },
  },
});
```

---

### utils/auth.ts — Token Caching in TypeScript

```typescript
import { APIRequestContext } from '@playwright/test';

// null means "no token yet"
// 'string | null' is a union type — the variable can be either a string OR null
let cachedToken: string | null = null;

export async function getAccessToken(request: APIRequestContext): Promise<string> {
    // If we already have a token, return it without making another HTTP call
    if (cachedToken) {
        return cachedToken;
    }

    // Date.now() = current Unix timestamp in ms (e.g. 1716400000000)
    // .toString(36) = converts to base-36 string (shorter, e.g. "lxyz123")
    const uniqueId = Date.now().toString(36);

    const response = await request.post('/api-clients/', {
        data: {
            clientName: `TestClient_${uniqueId}`,   // template literal (like f-strings in Python)
            clientEmail: `testclient_${uniqueId}@example.com`,
        },
    });

    if (!response.ok()) {
        // throw stops execution and marks the test as failed
        throw new Error(`Registration failed: ${await response.text()}`);
    }

    const body = await response.json();
    cachedToken = body.accessToken;
    return cachedToken!;   // '!' tells TypeScript: "trust me, this is not null"
}
```

---

### tests/api/status.spec.ts — Basic Test Structure

```typescript
import { test, expect } from '@playwright/test';

// test.describe = group related tests together
test.describe('Status API', () => {

    // Each test() is one test case
    test('GET /status — returns 200 and status OK', async ({ request }) => {
        //                                                   ↑
        // { request } is destructuring — we pull 'request' out of the Playwright fixture object

        const response = await request.get('/status');
        // await is REQUIRED — HTTP calls are async and take time

        expect(response.status()).toBe(200);

        const body = await response.json();
        // { "status": "OK" }

        expect(body.status).toBe('OK');
        // body.status = "OK"
        // "OK" === "OK" → test passes
    });

});
```

---

### tests/api/books.spec.ts — Handling Arrays and Parameters

```typescript
test('GET /books?type=fiction — only fiction', async ({ request }) => {
    const response = await request.get('/books', {
        params: { type: 'fiction' },   // Playwright adds ?type=fiction to the URL
    });

    const books = await response.json();
    // books is now a JavaScript array: [{...}, {...}, ...]

    // Loop over each book and check its type
    for (const book of books) {
        expect(book.type).toBe('fiction');
    }
});

test('GET /books/1 — single book details', async ({ request }) => {
    const response = await request.get('/books/1');
    // ↑ bookId is directly in the URL (no params needed)

    const book = await response.json();
    // book is a JavaScript object (dict): { id: 1, name: "...", ... }

    expect(book.id).toBe(1);
    expect(book).toHaveProperty('name');      // check the 'name' key exists
    expect(book).toHaveProperty('available'); // check 'available' key exists
});
```

---

### tests/api/orders.spec.ts — Serial Tests with Shared State

```typescript
// test.describe.serial = the tests in this block run ONE AT A TIME, in order
// (unlike the default which runs everything in parallel)
test.describe.serial('Orders API', () => {

    // Variables declared here are accessible to ALL tests in this describe block
    let token: string;     // the Bearer token
    let orderId: string;   // the ID of the order we create

    // test.beforeAll = runs ONCE before any test in this describe block
    test.beforeAll(async ({ request }) => {
        token = await getAccessToken(request);
        // token is now set and available for all tests below
    });

    test('POST /orders — create order', async ({ request }) => {
        const response = await request.post('/orders', {
            headers: { Authorization: `Bearer ${token}` },
            data: { bookId: 1, customerName: 'John Doe' },
        });

        expect(response.status()).toBe(201);
        const body = await response.json();
        expect(body).toHaveProperty('orderId');

        orderId = body.orderId;
        // orderId is now set and available to all tests that come after this one
    });

    test('PATCH /orders/:id — update name', async ({ request }) => {
        // We can use orderId here because we declared it in the describe scope
        // and the previous test set it
        const response = await request.patch(`/orders/${orderId}`, {
            headers: { Authorization: `Bearer ${token}` },
            data: { customerName: 'Jane Smith' },
        });

        expect(response.status()).toBe(204);
        // 204 = no body — do NOT call response.json() here
    });

});
```

**Why `test.describe.serial` instead of regular `test.describe`?**

| | Regular `test.describe` | `test.describe.serial` |
|---|---|---|
| Execution | Tests run in parallel | Tests run in strict order |
| Speed | Faster | Slower (sequential) |
| When to use | Independent tests | Tests that depend on previous results |
| Our orders tests | WRONG — test 2 starts before test 1 finishes | CORRECT |

## 9.6 How to Run (Playwright TypeScript)

```bash
# Navigate to the playwright project folder
cd d:\Downloads\simple-books-api-tests\playwright-typescript

# Install npm dependencies
npm install

# Run all API tests
npm run test:api

# Or run directly with npx
npx playwright test tests/api

# Run a specific spec file
npx playwright test tests/api/orders.spec.ts

# Open the HTML report in browser
npm run report

# Run in headed mode (shows browser if UI tests were added)
npx playwright test --headed
```

**What you see:**
```
Running 18 tests using 4 workers

  ✓ Status API › GET /status — returns 200 and status OK    (312ms)
  ✓ Books API  › GET /books — returns a non-empty list       (285ms)
  ✓ Orders API › POST /orders — create a new order           (421ms)
  ...

  18 passed (3.2s)
```

---

# 10. Test Design Principles

## 10.1 The Testing Pyramid

```
         /\
        /  \     End-to-End Tests (UI)
       / E2E \   Slow, expensive, few of them
      /________\
     /          \
    /  Integration\  API Tests ← We are here
   /    Tests      \ Fast, reliable, many of them
  /__________________\
 /                    \
/    Unit Tests        \ Fastest, cheapest, most of them
/________________________\
```

API tests sit in the middle — they test the full request/response cycle without the overhead of a browser.

## 10.2 What Makes a Good Test?

### FIRST Principles

| Letter | Principle | Meaning |
|---|---|---|
| **F** | Fast | Tests should run quickly (seconds, not minutes) |
| **I** | Independent | Tests should not depend on each other\* |
| **R** | Repeatable | Same result every time, on any machine |
| **S** | Self-Validating | Pass or fail automatically — no manual checking |
| **T** | Timely | Written at the same time as the code being tested |

\*Exception: CRUD lifecycle tests have intentional ordering (Create before Delete).

### Naming Tests

A good test name tells you EXACTLY what is being tested:

```python
# BAD names:
def test1():                # What does this test?
def test_order():           # What about orders?
def test_api():             # Way too vague

# GOOD names:
def test_create_order_returns_201_with_order_id():
def test_create_order_without_token_returns_401():
def test_get_non_existent_book_returns_404():
```

## 10.3 Arrange — Act — Assert (AAA Pattern)

Every test should have three clear sections:

```python
def test_create_order(self, base_url, auth_headers):

    # ARRANGE: set up the data you need
    payload = {
        "bookId": 1,
        "customerName": "John Doe"
    }

    # ACT: perform the action being tested
    response = requests.post(
        f"{base_url}/orders",
        json=payload,
        headers=auth_headers
    )

    # ASSERT: check the result
    assert response.status_code == 201
    assert response.json()["created"] is True
```

## 10.4 Test Coverage Checklist

For EVERY endpoint, ask these questions:

- [ ] Does the happy path (valid data) return the correct status and body?
- [ ] What happens with missing required fields? (expect 400)
- [ ] What happens with an invalid resource ID? (expect 404)
- [ ] What happens without authentication? (expect 401)
- [ ] What happens with invalid parameter values? (expect 400)
- [ ] Does CRUD correctly change and persist data?

---

# 11. Cheat Sheets & Quick Reference

## 11.1 HTTP Status Code Quick Reference

```
200 OK            → GET succeeded, body has the data
201 Created       → POST succeeded, new resource was created
204 No Content    → PATCH/DELETE succeeded, no body
400 Bad Request   → You sent bad data (missing field, wrong type)
401 Unauthorized  → No token or invalid token
403 Forbidden     → Token valid but you don't have permission
404 Not Found     → Resource doesn't exist at that URL
409 Conflict      → Duplicate (e.g., same email registered twice)
500 Server Error  → Bug on the server, not your fault
```

## 11.2 REST Assured (Java) — Common Patterns

```java
// GET with query params
given().spec(requestSpec).queryParam("type", "fiction")
       .when().get("/books")
       .then().statusCode(200);

// GET with path param
given().spec(requestSpec).pathParam("id", "abc123")
       .when().get("/orders/{id}")
       .then().statusCode(200);

// POST with body and auth
given().spec(requestSpec)
       .header("Authorization", "Bearer " + token)
       .body(map)
       .when().post("/orders")
       .then().statusCode(201).body("created", equalTo(true));

// PATCH
given().spec(requestSpec)
       .header("Authorization", "Bearer " + token)
       .body(updateMap)
       .when().patch("/orders/{id}")
       .then().statusCode(204);

// DELETE
given().spec(requestSpec)
       .header("Authorization", "Bearer " + token)
       .when().delete("/orders/{id}")
       .then().statusCode(204);

// Extract value from response
String orderId = response.jsonPath().getString("orderId");
int bookId     = response.jsonPath().getInt("bookId");
boolean flag   = response.jsonPath().getBoolean("created");
```

## 11.3 requests (Python) — Common Patterns

```python
# GET
response = requests.get(url)
response = requests.get(url, params={"type": "fiction"})

# POST with JSON body
response = requests.post(url, json={"bookId": 1}, headers=headers)

# PATCH
response = requests.patch(url, json={"customerName": "Jane"}, headers=headers)

# DELETE
response = requests.delete(url, headers=headers)

# Reading responses
response.status_code       # int
response.json()            # dict or list
response.json()["key"]     # access a field
response.json().get("key") # safer access (returns None if missing)
response.text              # raw string
response.headers["Content-Type"]  # header value
```

## 11.4 Playwright (TypeScript) — Common Patterns

```typescript
// GET
const r = await request.get('/books');
const r = await request.get('/books', { params: { type: 'fiction' } });

// POST
const r = await request.post('/orders', {
    headers: { Authorization: `Bearer ${token}` },
    data: { bookId: 1, customerName: 'John' }
});

// PATCH
const r = await request.patch(`/orders/${id}`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { customerName: 'Jane' }
});

// DELETE
const r = await request.delete(`/orders/${id}`, {
    headers: { Authorization: `Bearer ${token}` }
});

// Reading responses
r.status()              // number
r.ok()                  // boolean (true if < 400)
await r.json()          // object or array
await r.text()          // raw string
r.headers()['content-type']  // header value

// Common assertions
expect(r.status()).toBe(200);
expect(r.status()).not.toBe(400);
expect(body).toHaveProperty('orderId');
expect(array.length).toBeGreaterThan(0);
expect(array.length).toBeLessThanOrEqualTo(2);
```

## 11.5 How Each Language Handles the Same Test

Here is the exact same test written in all three languages side by side:

**Scenario:** Create an order and verify the response.

**Java (REST Assured):**
```java
@Test
public void testCreateOrder() {
    Map<String, Object> body = new HashMap<>();
    body.put("bookId", 1);
    body.put("customerName", "John Doe");

    Response response = RestAssured.given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + token)
            .body(body)
            .when().post("/orders")
            .then().statusCode(201)
                   .body("created", equalTo(true))
                   .extract().response();

    orderId = response.jsonPath().getString("orderId");
    assertNotNull(orderId);
}
```

**Python (requests + pytest):**
```python
def test_create_order(self, base_url, auth_headers):
    payload = {"bookId": 1, "customerName": "John Doe"}
    response = requests.post(
        f"{base_url}/orders",
        json=payload,
        headers=auth_headers
    )

    assert response.status_code == 201
    data = response.json()
    assert data["created"] is True
    assert "orderId" in data

    TestOrders.order_id = data["orderId"]
```

**TypeScript (Playwright):**
```typescript
test('POST /orders — create order', async ({ request }) => {
    const response = await request.post('/orders', {
        headers: { Authorization: `Bearer ${token}` },
        data: { bookId: 1, customerName: 'John Doe' },
    });

    expect(response.status()).toBe(201);
    const body = await response.json();
    expect(body.created).toBe(true);
    expect(body).toHaveProperty('orderId');

    orderId = body.orderId;
});
```

## 11.6 Command Reference

```bash
# ── JAVA ────────────────────────────────────────────────────────────
mvn test                               # run all tests
mvn test -Dtest=BooksTest              # run one class
mvn test -Dtest=BooksTest#testGetAll   # run one method

# ── PYTHON ──────────────────────────────────────────────────────────
pip install -r requirements.txt        # install dependencies
pytest                                 # run all tests
pytest -v                              # verbose (show test names)
pytest tests/test_orders.py            # run one file
pytest tests/test_orders.py::TestOrders::test_01_create_order  # one method
pytest --html=report.html              # generate HTML report
pytest -x                              # stop on first failure
pytest -k "create or delete"           # run tests matching keyword

# ── PLAYWRIGHT (TypeScript) ──────────────────────────────────────────
npm install                            # install dependencies
npx playwright test                    # run all tests
npx playwright test tests/api          # run api tests folder
npx playwright test tests/api/orders   # run orders spec
npx playwright show-report             # open HTML report
npx playwright test --debug            # interactive debugger
```

---

## Summary

| Topic | Java | Python | TypeScript |
|---|---|---|---|
| HTTP Client | REST Assured | requests | Playwright `request` fixture |
| Test Framework | TestNG | pytest | Playwright Test |
| Fixture/Setup | `@BeforeSuite`, `@BeforeClass` | `@pytest.fixture` in conftest.py | `test.beforeAll` |
| Assertions | Hamcrest matchers | `assert` statement | `expect()` matchers |
| Token caching | Static class variable | `scope="session"` fixture | Module-level variable |
| Parallel config | `testng.xml` `parallel="classes"` | pytest-xdist (optional) | `fullyParallel: true` in config |
| Run command | `mvn test` | `pytest` | `npx playwright test` |
| Report | TestNG HTML report | `pytest-html` | Built-in Playwright HTML report |

---

*Document covers: API fundamentals · HTTP methods & status codes · Authentication · REST Assured + TestNG · pytest + requests · Playwright TypeScript · Test design principles*

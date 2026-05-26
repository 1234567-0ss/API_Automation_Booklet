# Simple Books API — Test Automation Suite

Three complete implementations of the same API test suite for the [Simple Books API](https://simple-books-api.click).

| Implementation | Language | Framework |
|---|---|---|
| `java-rest-assured/` | Java 11 | REST Assured + TestNG |
| `python-pytest/` | Python 3.9+ | pytest + requests |
| `playwright-typescript/` | TypeScript | Playwright Test |

---

## API Under Test

Base URL: `https://simple-books-api.click`

| Method | Endpoint | Auth | Purpose |
|---|---|---|---|
| GET | `/status` | No | Health check |
| GET | `/books` | No | List books (filter by type, limit) |
| GET | `/books/{id}` | No | Get one book |
| POST | `/api-clients/` | No | Register & get access token |
| POST | `/orders` | Yes | Create an order |
| GET | `/orders` | Yes | List your orders |
| GET | `/orders/{id}` | Yes | Get one order |
| PATCH | `/orders/{id}` | Yes | Update order |
| DELETE | `/orders/{id}` | Yes | Delete order |

---

## Java — REST Assured + TestNG

### Prerequisites
- Java 11+
- Maven 3.6+

### Setup & Run

```bash
cd java-rest-assured
mvn test
```

### Project Structure

```
java-rest-assured/
├── pom.xml                         # Maven dependencies
├── testng.xml                      # Test suite configuration (parallel settings)
└── src/test/java/com/simplebooksapi/
    ├── base/BaseTest.java          # Common base URL + request config
    ├── utils/AuthHelper.java       # Token registration + caching
    └── tests/
        ├── StatusTest.java         # GET /status tests
        ├── BooksTest.java          # GET /books tests
        └── OrdersTest.java         # Full CRUD order tests
```

### Key Concepts (REST Assured)

```java
// GIVEN → set up the request
RestAssured.given()
    .spec(requestSpec)                        // apply base config
    .header("Authorization", "Bearer " + token)
    .body(map)                                // JSON body

// WHEN → send the request
.when()
    .post("/orders")

// THEN → assert the response
.then()
    .statusCode(201)
    .body("created", equalTo(true));
```

---

## Python — pytest + requests

### Prerequisites
- Python 3.9+
- pip

### Setup & Run

```bash
cd python-pytest

# Install dependencies
pip install -r requirements.txt

# Run all tests
pytest

# Run with HTML report
pytest --html=report.html

# Run a specific test file
pytest tests/test_orders.py -v
```

### Project Structure

```
python-pytest/
├── requirements.txt       # Dependencies (requests, pytest, pytest-html)
├── pytest.ini             # pytest settings
├── conftest.py            # Shared fixtures (base_url, auth_token, auth_headers)
└── tests/
    ├── test_status.py     # GET /status tests
    ├── test_books.py      # GET /books tests
    └── test_orders.py     # Full CRUD order tests
```

### Key Concepts (pytest + requests)

```python
# conftest.py defines fixtures that tests receive as parameters
@pytest.fixture(scope="session")
def auth_headers(auth_token):
    return {"Authorization": f"Bearer {auth_token}"}

# Tests just declare what they need — pytest injects it
def test_create_order(self, base_url, auth_headers):
    response = requests.post(f"{base_url}/orders", json=payload, headers=auth_headers)
    assert response.status_code == 201
```

---

## TypeScript — Playwright Test

### Prerequisites
- Node.js 18+
- npm

### Setup & Run

```bash
cd playwright-typescript

# Install dependencies
npm install

# Install Playwright browsers (needed for UI tests; optional for API-only)
npx playwright install

# Run API tests
npm run test:api

# Open HTML report
npm run report
```

### Project Structure

```
playwright-typescript/
├── package.json            # npm dependencies
├── playwright.config.ts    # Base URL, parallel settings, reporter config
├── tsconfig.json           # TypeScript config
├── utils/
│   └── auth.ts             # Token registration + caching
└── tests/api/
    ├── status.spec.ts      # GET /status tests
    ├── books.spec.ts       # GET /books tests
    └── orders.spec.ts      # Full CRUD order tests (serial mode)
```

### Key Concepts (Playwright API Testing)

```typescript
// Playwright's 'request' fixture is a built-in HTTP client
test('create order', async ({ request }) => {
  const response = await request.post('/orders', {
    headers: { Authorization: `Bearer ${token}` },
    data: { bookId: 1, customerName: 'John Doe' },  // auto-serialised to JSON
  });

  expect(response.status()).toBe(201);
  const body = await response.json();
  expect(body).toHaveProperty('orderId');
});
```

---

## Test Coverage Summary

| Test Case | Status & Books | Orders |
|---|---|---|
| Happy path (valid data) | ✓ | ✓ |
| Filter / query params | ✓ | — |
| Non-existent resource → 404 | ✓ | ✓ |
| Invalid input → 400 | ✓ | ✓ |
| Missing auth → 401 | — | ✓ |
| Full CRUD lifecycle | — | ✓ |

"""
test_orders.py — Tests for the full order lifecycle (CRUD)
===========================================================

WHAT DOES THIS FILE TEST?
  The complete Create → Read → Update → Delete cycle for the /orders endpoint,
  plus error/security tests (missing auth, missing fields).

CRUD FLOW (7 numbered tests):
  test_01_create_order        → POST   /orders           (creates, saves order ID)
  test_02_get_all_orders      → GET    /orders           (lists all orders)
  test_03_get_order_by_id     → GET    /orders/{id}      (reads the one we created)
  test_04_update_order        → PATCH  /orders/{id}      (changes the customer name)
  test_05_verify_order_updated→ GET    /orders/{id}      (confirms the change)
  test_06_delete_order        → DELETE /orders/{id}      (removes the order)
  test_07_verify_order_deleted→ GET    /orders/{id}      (confirms 404 after delete)

PLUS 2 NEGATIVE TESTS:
  test_08_create_order_no_token_returns_401   → no auth → 401
  test_09_create_order_missing_field_returns_400 → missing field → 400

AUTH REQUIRED: Yes — every /orders operation needs a Bearer token.
  The auth_headers fixture from conftest.py provides this automatically.

IMPORTANT — TEST ORDERING:
  pytest runs test methods IN THE ORDER THEY ARE DEFINED in the file.
  (This is pytest's default for methods within a class — alphabetical/definition order.)
  We INTENTIONALLY name them test_01, test_02... to make the order explicit and clear.

  The shared state (order_id) is stored as a CLASS VARIABLE so all methods can access it.
  Python class variables are shared across all instances of the class.
"""

import requests
# HTTP client — sends GET, POST, PATCH, DELETE requests.


# ══════════════════════════════════════════════════════════════════════════════
# TEST CLASS
# ══════════════════════════════════════════════════════════════════════════════

class TestOrders:

    # ── CLASS VARIABLE ─────────────────────────────────────────────────────────
    # order_id: shared between ALL test methods in this class.
    #
    # In Python:
    #   - An INSTANCE variable (self.order_id) belongs to one specific object.
    #   - A CLASS variable (TestOrders.order_id) belongs to the CLASS ITSELF
    #     and is shared by all instances.
    #
    # pytest creates one TestOrders instance per test method.
    # Using a class variable means test_01 can SET it and test_02..07 can READ it,
    # even though they run in different instances.
    #
    # Initial value is None ("nothing yet") — set by test_01_create_order.
    order_id = None

    # ══════════════════════════════════════════════════════════════════════════
    # STEP 1: CREATE AN ORDER
    # ══════════════════════════════════════════════════════════════════════════

    def test_01_create_order(self, base_url, auth_headers):
        # auth_headers: fixture from conftest.py
        # Returns: {"Authorization": "Bearer eyJhbGci..."}
        # Automatically injected by pytest — we just declare the parameter.
        """POST /orders — place a new book order."""

        # Build the request body as a Python dict.
        # The requests library will serialise this to a JSON string.
        payload = {
            "bookId": 1,               # Which book to order (must exist and be available)
            "customerName": "John Doe" # Name of the person ordering (required field)
        }

        # requests.post() — sends an HTTP POST request.
        # json=payload: serialises the dict to JSON AND sets Content-Type: application/json
        #               automatically (more convenient than passing body as a string).
        # headers=auth_headers: adds the Authorization: Bearer ... header.
        response = requests.post(
            f"{base_url}/orders",   # POST https://simple-books-api.click/orders
            json=payload,           # Request body: {"bookId":1,"customerName":"John Doe"}
            headers=auth_headers    # Authorization header with Bearer token
        )

        # 201 = Created — the server successfully created a new resource.
        assert response.status_code == 201, (
            f"Expected 201 Created, got {response.status_code}: {response.text}"
        )

        # Parse the JSON response body into a Python dict.
        # Example: {"created": true, "orderId": "LkOMyoQFLRMs7zf"}
        data = response.json()

        # .get("created") reads the "created" key safely (returns None if missing).
        # 'is True' checks it's exactly the boolean True, not just truthy.
        assert data.get("created") is True, "Response should confirm order was created"

        # "orderId" must be in the response — we need it for all subsequent tests.
        assert "orderId" in data, "Response should include the new order's ID"

        # Save the orderId to the CLASS variable using 'TestOrders.order_id' (not 'self.order_id').
        # Using the class name ensures ALL instances of TestOrders share this value.
        # If you used self.order_id, only THIS instance would have the value;
        # the next test (different pytest instance) would see None.
        TestOrders.order_id = data["orderId"]

        # Print to terminal so you can see the ID during the test run.
        print(f"\n[TestOrders] Created order — ID: {TestOrders.order_id}")

    # ══════════════════════════════════════════════════════════════════════════
    # STEP 2: READ ALL ORDERS
    # ══════════════════════════════════════════════════════════════════════════

    def test_02_get_all_orders(self, base_url, auth_headers):
        """GET /orders — retrieve all orders for this client."""

        # requests.get() — sends an HTTP GET request.
        # headers=auth_headers — include the Authorization header.
        response = requests.get(f"{base_url}/orders", headers=auth_headers)

        assert response.status_code == 200

        # Parse the response — /orders returns a JSON array of order objects.
        orders = response.json()

        # isinstance(orders, list) — confirm it's a list, not a dict or string.
        assert isinstance(orders, list), "Expected a list of orders"

        # Must contain at least the order we just created.
        assert len(orders) > 0, "Should have at least one order (the one we just created)"

    # ══════════════════════════════════════════════════════════════════════════
    # STEP 3: READ ONE ORDER
    # ══════════════════════════════════════════════════════════════════════════

    def test_03_get_order_by_id(self, base_url, auth_headers):
        """GET /orders/{id} — retrieve the specific order we created."""

        # Defensive check: if test_01 failed, order_id is still None.
        # This assert gives a clear error message instead of a confusing 404 or crash.
        assert TestOrders.order_id, "order_id is not set — did test_01 pass?"

        # Build the URL with the saved order ID embedded in the path.
        # f"{base_url}/orders/{TestOrders.order_id}" produces e.g.:
        #   "https://simple-books-api.click/orders/LkOMyoQFLRMs7zf"
        response = requests.get(
            f"{base_url}/orders/{TestOrders.order_id}",
            headers=auth_headers
        )

        assert response.status_code == 200

        # Parse the single order object.
        order = response.json()

        # Verify the response matches what we created in test_01.
        assert order["id"] == TestOrders.order_id, "Returned order ID should match"
        assert order["bookId"] == 1,               "Book ID should be 1"
        assert order["customerName"] == "John Doe","Customer name should be John Doe"

    # ══════════════════════════════════════════════════════════════════════════
    # STEP 4: UPDATE THE ORDER
    # ══════════════════════════════════════════════════════════════════════════

    def test_04_update_order(self, base_url, auth_headers):
        """PATCH /orders/{id} — change the customer name."""

        # PATCH: update ONLY the fields you specify.
        # We only send customerName — other fields (bookId, etc.) stay the same.
        # This is different from PUT, which would REPLACE the entire order object.
        payload = {"customerName": "Jane Smith"}  # New name to replace "John Doe"

        # requests.patch() — sends an HTTP PATCH request.
        response = requests.patch(
            f"{base_url}/orders/{TestOrders.order_id}",
            json=payload,          # Body: {"customerName": "Jane Smith"}
            headers=auth_headers   # Auth header with Bearer token
        )

        # 204 = No Content.
        # The operation succeeded but the server has nothing to return.
        # This is standard for PATCH and DELETE — success without a response body.
        # If you see 200 for a PATCH, the API is returning the updated resource (also valid).
        assert response.status_code == 204, (
            f"Expected 204 No Content, got {response.status_code}: {response.text}"
        )

    def test_05_verify_order_updated(self, base_url, auth_headers):
        """GET /orders/{id} — confirm the name was changed to Jane Smith."""

        # After every write operation (POST/PATCH/DELETE), always VERIFY the change
        # with a subsequent GET. This is called "read-after-write verification"
        # and confirms the operation actually persisted.
        response = requests.get(
            f"{base_url}/orders/{TestOrders.order_id}",
            headers=auth_headers
        )

        assert response.status_code == 200
        order = response.json()

        # The name should now be "Jane Smith", not "John Doe".
        # If the PATCH was silently ignored, this assertion catches the bug.
        assert order["customerName"] == "Jane Smith", (
            f"Expected 'Jane Smith' but got: {order['customerName']}"
        )

    # ══════════════════════════════════════════════════════════════════════════
    # STEP 5: DELETE THE ORDER
    # ══════════════════════════════════════════════════════════════════════════

    def test_06_delete_order(self, base_url, auth_headers):
        """DELETE /orders/{id} — remove the order."""

        # requests.delete() — sends an HTTP DELETE request.
        # No body needed for deletion — the URL identifies what to delete.
        response = requests.delete(
            f"{base_url}/orders/{TestOrders.order_id}",
            headers=auth_headers
        )

        # 204 = No Content — deletion was successful.
        assert response.status_code == 204, (
            f"Expected 204 No Content, got {response.status_code}: {response.text}"
        )

    def test_07_verify_order_deleted(self, base_url, auth_headers):
        """GET /orders/{id} — confirm the deleted order no longer exists."""

        # Try to GET the order we just deleted.
        # The server should tell us it no longer exists.
        response = requests.get(
            f"{base_url}/orders/{TestOrders.order_id}",
            headers=auth_headers
        )

        # 404 = Not Found — the resource no longer exists after deletion.
        # This CONFIRMS the delete actually worked.
        # If we got 200 here, the DELETE was a no-op (serious bug in the API).
        assert response.status_code == 404, (
            f"Expected 404 Not Found after deletion, got {response.status_code}"
        )

    # ══════════════════════════════════════════════════════════════════════════
    # NEGATIVE / SECURITY TESTS
    # ══════════════════════════════════════════════════════════════════════════

    # ──────────────────────────────────────────────────────────────────────────
    # NEGATIVE TEST 1: No authentication token
    # ──────────────────────────────────────────────────────────────────────────

    def test_08_create_order_no_token_returns_401(self, base_url):
        # Note: no auth_headers parameter — this test deliberately sends NO auth.
        """POST /orders without a token should be rejected with 401 Unauthorized."""

        payload = {"bookId": 1, "customerName": "No Auth User"}

        # No headers= parameter — we do NOT include the Authorization header.
        # This simulates an unauthenticated client trying to access a protected endpoint.
        response = requests.post(f"{base_url}/orders", json=payload)
        # requests.post() without headers= uses only the default requests headers.

        # 401 = Unauthorized — credentials were missing or invalid.
        # The API should ALWAYS reject requests without a valid token.
        # This is a SECURITY test — passing it means the API is properly protected.
        assert response.status_code == 401, (
            f"Expected 401 Unauthorized, got {response.status_code}"
        )

    # ──────────────────────────────────────────────────────────────────────────
    # NEGATIVE TEST 2: Missing required field
    # ──────────────────────────────────────────────────────────────────────────

    def test_09_create_order_missing_field_returns_400(self, base_url, auth_headers):
        """POST /orders without customerName should return 400 Bad Request."""

        # Build an INCOMPLETE payload — customerName is intentionally missing.
        # The API requires both bookId AND customerName. Sending only bookId
        # should trigger server-side VALIDATION and return an error.
        payload = {"bookId": 1}   # customerName is deliberately omitted

        response = requests.post(
            f"{base_url}/orders",
            json=payload,          # Incomplete body — missing required field
            headers=auth_headers   # Valid auth — this is NOT an auth test
        )

        # 400 = Bad Request — the client sent malformed or incomplete data.
        # Input validation should catch this BEFORE any order is created.
        assert response.status_code == 400, (
            f"Expected 400 Bad Request for missing field, got {response.status_code}"
        )

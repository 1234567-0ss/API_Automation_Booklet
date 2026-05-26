"""
test_status.py — Tests for the GET /status endpoint
=====================================================

WHAT DOES THIS FILE TEST?
  The health-check endpoint: GET /status
  It simply tells us whether the API is running and ready to accept requests.

ENDPOINT DETAILS:
  URL    : GET https://simple-books-api.click/status
  Auth   : NOT required (anyone can call it)
  Response:
    HTTP 200 OK
    Content-Type: application/json
    Body: { "status": "OK" }

NAMING CONVENTION:
  pytest discovers test files whose names START WITH "test_" or END WITH "_test".
  Inside, it discovers:
    - Functions starting with "test_"
    - Methods starting with "test_" inside classes starting with "Test"
  This file follows both conventions: filename test_status.py, class TestStatus,
  methods test_api_is_up and test_response_is_json.

RUN THIS FILE ALONE:
  pytest tests/test_status.py -v
"""

import requests
# The requests library — sends real HTTP requests to the API.
# Imported from the 'requests' package installed via requirements.txt.


# ══════════════════════════════════════════════════════════════════════════════
# TEST CLASS
# ══════════════════════════════════════════════════════════════════════════════

class TestStatus:
    # Grouping tests in a class is optional in pytest (unlike JUnit/TestNG in Java).
    # Benefits of using a class:
    #   - Organises related tests together logically
    #   - Makes the test report output cleaner (shows TestStatus::test_api_is_up)
    #   - Allows sharing class-level setup (though we don't need it here)
    #
    # NOTE: Python test classes do NOT extend a base class (unlike Java's BaseTest).
    # Instead, fixtures from conftest.py are injected as METHOD PARAMETERS.

    # ──────────────────────────────────────────────────────────────────────────
    # TEST 1: HTTP status and response body
    # ──────────────────────────────────────────────────────────────────────────

    def test_api_is_up(self, base_url):
        # 'self' — required first parameter for methods in a Python class.
        #          It refers to the current instance of TestStatus.
        #          You never pass it manually; Python does that automatically.
        #
        # 'base_url' — a pytest FIXTURE from conftest.py.
        #              pytest sees this parameter name, finds the matching fixture,
        #              runs it, and injects the return value ("https://simple-books-api.click").
        #              The test method never needs to call or import conftest.py itself.

        """Verify the API health-check returns HTTP 200 and status OK."""

        # requests.get(url) — sends an HTTP GET request to the given URL.
        # Returns a Response object containing:
        #   response.status_code  → integer, e.g. 200
        #   response.json()       → parsed JSON body as dict/list
        #   response.text         → raw response body as string
        #   response.headers      → response headers dict
        #
        # f"{base_url}/status" is an f-string:
        #   base_url = "https://simple-books-api.click"
        #   result   = "https://simple-books-api.click/status"
        response = requests.get(f"{base_url}/status")

        # assert CONDITION, "message if condition is False"
        # If response.status_code is anything other than 200, this line FAILS the test
        # and prints the message showing the actual code received.
        # pytest enhances 'assert' to show you the actual vs expected values.
        assert response.status_code == 200, f"Expected 200 but got {response.status_code}"

        # response.json() parses the response body.
        # The JSON string { "status": "OK" } becomes a Python dict: {"status": "OK"}
        # We store it in 'data' to check its contents.
        data = response.json()

        # data["status"] reads the value at key "status" from the dict.
        # If the key doesn't exist, Python raises a KeyError (test fails).
        # If the value is not "OK", this assertion fails.
        # The error message shows what was actually in the response for debugging.
        assert data["status"] == "OK", f"Expected status=OK but got: {data}"

    # ──────────────────────────────────────────────────────────────────────────
    # TEST 2: Content-Type header
    # ──────────────────────────────────────────────────────────────────────────

    def test_response_is_json(self, base_url):
        """Verify the API returns a JSON Content-Type header."""

        # Another GET /status request.
        # Each test method is INDEPENDENT — it sets up its own state.
        # We don't reuse the response from test_api_is_up because
        # tests should not depend on each other's internal variables.
        response = requests.get(f"{base_url}/status")

        # response.headers is a case-insensitive dict-like object.
        # .get("Content-Type", "") tries to get the header value.
        # The second argument "" is the DEFAULT if the header doesn't exist,
        # preventing a KeyError crash if the header is missing.
        # Example value: "application/json; charset=utf-8"
        content_type = response.headers.get("Content-Type", "")

        # 'in' checks if "application/json" appears ANYWHERE in the string.
        # We use 'in' (not ==) because the actual header is often:
        #   "application/json; charset=utf-8"
        # Using 'in' means the assertion still passes even with the charset suffix.
        assert "application/json" in content_type, (
            f"Expected JSON content-type but got: {content_type}"
        )
        # The parentheses around the multi-line string is Python's way of
        # writing a long string across two lines without a backslash.

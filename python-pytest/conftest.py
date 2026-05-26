"""
conftest.py — pytest's Special Configuration and Fixtures File
==============================================================

WHAT IS conftest.py?
  This is a SPECIAL file that pytest automatically reads before running any tests.
  It has two main purposes:
    1. Define FIXTURES — reusable setup functions that tests can request as parameters
    2. Configure pytest HOOKS — advanced customisation (not used here)

  pytest searches for conftest.py files by walking up from the test file's folder.
  Any fixtures defined here are available to ALL test files in the same folder and below.
  You do NOT need to import conftest.py — pytest handles that automatically.

═══════════════════════════════════════════════════════════════════════════════
WHAT IS A FIXTURE?
═══════════════════════════════════════════════════════════════════════════════
  A fixture is a REUSABLE SETUP FUNCTION decorated with @pytest.fixture.

  Instead of copying setup code into every test, you:
    1. Define it ONCE in conftest.py with @pytest.fixture
    2. Ask for it by adding its NAME as a PARAMETER in any test function

  pytest automatically detects the parameter name, finds the matching fixture,
  runs it, and injects its return value into the test.

  Example:
    # In conftest.py:
    @pytest.fixture
    def base_url():
        return "https://example.com"

    # In a test file:
    def test_something(base_url):           ← pytest injects "https://example.com" here
        response = requests.get(base_url)   ← base_url is already the string
        ...

  This is called DEPENDENCY INJECTION — the test declares what it needs,
  and pytest provides it. The test does not need to know HOW to get it.

═══════════════════════════════════════════════════════════════════════════════
FIXTURE SCOPES
═══════════════════════════════════════════════════════════════════════════════
  Scope controls HOW OFTEN the fixture function runs:

  scope="function"  → runs BEFORE EVERY SINGLE TEST (default if omitted)
                      Use for: creating temporary data, resetting state
  scope="class"     → runs ONCE per TEST CLASS
  scope="module"    → runs ONCE per TEST FILE (.py file)
  scope="session"   → runs ONCE for the ENTIRE test run (all files)
                      Use for: expensive setup like getting an auth token

  Here we use "session" for all three fixtures because:
    - The base URL never changes — no need to compute it more than once
    - Registering a new API client for every test would be wasteful and slow
    - The token is valid for 7 days — one per session is enough
"""

import pytest     # The pytest library — provides @pytest.fixture and other tools
import requests   # The HTTP client — used to call POST /api-clients/ to get a token
import uuid       # Python's built-in UUID generator — creates unique random strings


# The root URL of the API. Defined once here so it's easy to change if needed.
# All fixtures and tests that need the URL get it from base_url() below.
BASE_URL = "https://simple-books-api.click"


# ══════════════════════════════════════════════════════════════════════════════
# FIXTURE 1: base_url
# ══════════════════════════════════════════════════════════════════════════════

@pytest.fixture(scope="session")
# @pytest.fixture(scope="session"):
#   Marks this function as a pytest fixture.
#   scope="session" means it runs ONCE for the whole test session.
#   The same string value is reused for every test that requests base_url.

def base_url():
    """Provides the API base URL to all tests that need it.

    HOW TESTS USE THIS:
      def test_something(base_url):
          response = requests.get(f"{base_url}/status")
          ...
      pytest sees 'base_url' parameter, runs THIS function,
      and passes the returned string to the test.
    """
    # Simply return the constant string.
    # Every test that declares 'base_url' as a parameter receives "https://simple-books-api.click"
    return BASE_URL


# ══════════════════════════════════════════════════════════════════════════════
# FIXTURE 2: auth_token
# ══════════════════════════════════════════════════════════════════════════════

@pytest.fixture(scope="session")
# scope="session": the token is registered ONCE and reused by all tests.
# Registering a new client for each test would hit the API many times
# and leave behind junk registrations.

def auth_token(base_url):
    # Note: 'base_url' is listed as a PARAMETER here.
    # pytest sees this and automatically injects the base_url fixture's return value.
    # Fixtures CAN depend on other fixtures this way.
    """
    Registers a new API client and returns the Bearer access token.

    WHY DO WE NEED TO REGISTER?
      The /orders endpoints are protected. To use them, you need to prove
      you are a known client. Registration is the process of telling the API
      "I am a client, here's my name and email" and getting a token back.

    WHY uuid?
      The API returns HTTP 409 Conflict if you register an email that already exists.
      uuid.uuid4() generates a random unique ID every run, guaranteeing a fresh email.
      Example: "6ba7b810-9dad-11d1-80b4-00c04fd430c8"
    """

    # uuid.uuid4() generates a random UUID (Version 4 = random)
    # str(...) converts the UUID object to a string
    # [:8] takes only the first 8 characters for a short unique suffix
    # Example result: "6ba7b810"
    unique_id = str(uuid.uuid4())[:8]

    # Build the registration payload.
    # Python dicts use {key: value} syntax — similar to JSON objects.
    # The requests library automatically converts this dict to a JSON string
    # when we pass it as json=payload below.
    payload = {
        "clientName":  f"TestClient_{unique_id}",          # e.g. "TestClient_6ba7b810"
        "clientEmail": f"testclient_{unique_id}@example.com",  # e.g. "testclient_6ba7b810@example.com"
    }
    # f"..." is an f-string (formatted string) — the {unique_id} part is replaced
    # with the actual value of the unique_id variable at runtime.

    # Send POST /api-clients/ to register.
    # json=payload: the dict is serialised to JSON and Content-Type is set automatically.
    # f"{base_url}/api-clients/" builds the full URL: https://simple-books-api.click/api-clients/
    response = requests.post(f"{base_url}/api-clients/", json=payload)

    # assert checks a condition at runtime.
    # If the condition is False, pytest marks the test (or fixture) as FAILED
    # and shows the message after the comma.
    # Here: if the status code is NOT 201, the assertion fails with a helpful message
    # showing the actual status code and response body.
    assert response.status_code == 201, (
        f"Token registration failed with {response.status_code}: {response.text}"
    )

    # response.json() parses the response body from JSON string to a Python dict.
    # ["accessToken"] retrieves the value at key "accessToken".
    # Example response body: {"accessToken": "eyJhbGciOiJIUzI1NiI..."}
    token = response.json()["accessToken"]

    # Print confirmation to the terminal (visible because log_cli = true in pytest.ini).
    # Only show the first 20 characters — never log full tokens (security best practice).
    print(f"\n[conftest] Registered API client — token starts with: {token[:20]}...")

    # Return the token string. pytest stores this and injects it into any
    # fixture or test that declares 'auth_token' as a parameter.
    return token


# ══════════════════════════════════════════════════════════════════════════════
# FIXTURE 3: auth_headers
# ══════════════════════════════════════════════════════════════════════════════

@pytest.fixture(scope="session")
# scope="session": built once, reused for all authenticated tests.

def auth_headers(auth_token):
    # auth_token: another fixture injected by pytest.
    # auth_headers DEPENDS ON auth_token — pytest calls auth_token first,
    # then passes its return value here as the 'auth_token' parameter.
    """
    Returns a headers dictionary with the Authorization header pre-populated.

    WHY THIS FIXTURE?
      Every authenticated request needs the same header:
        {"Authorization": "Bearer eyJhbGci..."}
      Without this fixture, every test would have to build this dict manually.
      With it, tests just declare 'auth_headers' and get the dict automatically.

    USAGE IN TESTS:
      def test_get_orders(base_url, auth_headers):
          response = requests.get(f"{base_url}/orders", headers=auth_headers)
          ...
    """

    # Build and return the headers dict.
    # The HTTP Authorization header format for Bearer tokens is always:
    #   Authorization: Bearer <token>
    # There is a SPACE between "Bearer" and the actual token — this is the standard.
    return {"Authorization": f"Bearer {auth_token}"}
    # f"Bearer {auth_token}" produces e.g. "Bearer eyJhbGciOiJIUzI1NiI..."

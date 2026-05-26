# ══════════════════════════════════════════════════════════════════════════════
# __init__.py — Python Package Marker File
# ══════════════════════════════════════════════════════════════════════════════
#
# WHAT IS THIS FILE?
#   This is an empty (or near-empty) file that tells Python:
#   "treat the folder I'm in as a PACKAGE."
#
# WHAT IS A PACKAGE?
#   In Python, a PACKAGE is a folder that contains Python modules (.py files).
#   The presence of __init__.py is what makes a folder a package.
#   Without it, Python treats the folder as just a directory, not importable code.
#
# WHY DO WE NEED IT FOR PYTEST?
#   pytest uses Python's import system to load test files and fixtures.
#   When conftest.py is in the parent folder (python-pytest/), pytest needs to
#   be able to IMPORT fixtures from it into test files inside tests/.
#   Making tests/ a package (by having __init__.py) ensures pytest can find
#   and correctly import conftest.py fixtures in all test files here.
#
# WHY IS IT EMPTY?
#   __init__.py can contain package-level imports or initialisation code,
#   but for test packages you almost never need that.
#   An empty file is all that's needed to mark the folder as a package.
#
# FILES EXPLAINED:
#   tests/
#   ├── __init__.py       ← this file (marks 'tests' as a package)
#   ├── test_status.py    ← tests for GET /status
#   ├── test_books.py     ← tests for GET /books
#   └── test_orders.py    ← tests for /orders CRUD lifecycle

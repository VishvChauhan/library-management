# LIB-15: Audit Trail   — createdAt / updatedAt populated automatically
# LIB-4:  Book Status   — defaults to AVAILABLE, can be set to RESERVED
# LIB-13: Input Validation — 400 on invalid body, 404 on unknown id, 409 on duplicate ISBN, 201 on success

Feature: Book management — creation, status, audit trail, and input validation

  Background:
    Given the library service is running
    And the book repository is empty

  # ── LIB-15: Audit trail ─────────────────────────────────────────────────────

  Scenario: [LIB-15] createdAt and updatedAt are populated automatically on book creation
    When I send a POST request to "/api/books" with body:
      | title         | Clean Code        |
      | author        | Robert C. Martin  |
      | isbn          | 978-0132350884    |
      | genre         | TECHNOLOGY        |
      | publishedYear | 2008              |
    Then the response status is 201
    And the response body contains a non-null "createdAt" timestamp
    And the response body contains a non-null "updatedAt" timestamp
    And "createdAt" equals "updatedAt"

  # ── LIB-4: Book status ──────────────────────────────────────────────────────

  Scenario: [LIB-4] Book status defaults to AVAILABLE when a new book is created
    When I send a POST request to "/api/books" with body:
      | title         | The Pragmatic Programmer |
      | author        | David Thomas             |
      | isbn          | 978-0135957059           |
      | genre         | TECHNOLOGY               |
      | publishedYear | 1999                     |
    Then the response status is 201
    And the response body field "status" is "AVAILABLE"

  Scenario: [LIB-4] Book status can be set to RESERVED via POST /api/books/{id}/reserve
    Given a book exists with isbn "978-0134685991" and status "AVAILABLE"
    When I send a POST request to "/api/books/{id}/reserve"
    Then the response status is 200
    And the response body field "status" is "RESERVED"

  # ── LIB-13: Input validation ─────────────────────────────────────────────────

  Scenario: [LIB-13] POST /api/books with a missing required field returns 400
    When I send a POST request to "/api/books" with body:
      | author        | Unknown Author    |
      | isbn          | 978-0000000010    |
      | genre         | FICTION           |
      | publishedYear | 2000              |
    Then the response status is 400
    And the response body field "error" is "Bad Request"
    And the response body field "message" is "Validation failed"
    And the response body contains a validation error for field "title" with message "Title is required"

  Scenario: [LIB-13] POST /api/books with a malformed JSON body returns 400
    When I send a POST request to "/api/books" with raw body "{ invalid json }"
    Then the response status is 400
    And the response body field "message" is "Request body is invalid or malformed"

  Scenario: [LIB-13] POST /api/books with an ISBN that fails the pattern constraint returns 400
    When I send a POST request to "/api/books" with body:
      | title         | Bad ISBN Book |
      | author        | Some Author   |
      | isbn          | BADISBN!!!    |
      | genre         | FICTION       |
      | publishedYear | 2000          |
    Then the response status is 400
    And the response body contains a validation error for field "isbn" with message "ISBN must be 10 to 17 characters and contain only digits, X, or hyphen"

  Scenario: [LIB-13] GET /api/books/{id} for an unknown id returns 404
    When I send a GET request to "/api/books/99999"
    Then the response status is 404
    And the response body field "error" is "Not Found"

  Scenario: [LIB-13] POST /api/books with a duplicate ISBN returns 409
    Given a book already exists with isbn "978-0134685991"
    When I send a POST request to "/api/books" with body:
      | title         | Another Book      |
      | author        | Another Author    |
      | isbn          | 978-0134685991    |
      | genre         | NON_FICTION       |
      | publishedYear | 2015              |
    Then the response status is 409
    And the response body field "error" is "Conflict"
    And the response body field "message" contains "A book with ISBN 978-0134685991 already exists"

  Scenario: [LIB-13] POST /api/books with a valid body returns 201 with the created book
    When I send a POST request to "/api/books" with body:
      | title         | Domain-Driven Design |
      | author        | Eric Evans           |
      | isbn          | 978-0321125217       |
      | genre         | TECHNOLOGY           |
      | publishedYear | 2003                 |
    Then the response status is 201
    And the response body field "title" is "Domain-Driven Design"
    And the response body field "author" is "Eric Evans"
    And the response body field "isbn" is "978-0321125217"
    And the response body field "status" is "AVAILABLE"
    And the response body contains a non-null "id"
    And the response body contains a non-null "createdAt"
    And the response body contains a non-null "updatedAt"

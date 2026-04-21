# Library Management

Spring Boot sample application for managing books in a library.

## Features

- CRUD APIs for books
- Search and pagination
- Borrow and return workflows
- Overdue fine calculation
- H2 in-memory database
- Swagger UI / OpenAPI docs
- Startup seeding with 8 sample books

## Main Endpoints

- `GET /api/books`
- `GET /api/books/{id}`
- `GET /api/books/overdue`
- `POST /api/books`
- `PUT /api/books/{id}`
- `DELETE /api/books/{id}`
- `POST /api/books/{id}/borrow`
- `POST /api/books/{id}/return`

## Run

```bash
./gradlew bootRun
```

## Test

```bash
./gradlew test
```

## API Docs

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`


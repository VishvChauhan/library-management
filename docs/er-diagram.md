```mermaid
erDiagram
    BOOK ||--o{ LENDING : "has many"
    BORROWER ||--o { LENDING : "has many"

BOOK {
    Long id PK
    String title
    String author
    String isbn
    String genre
    BookStatus status
    LocalDateTime createdAt
    LocalDateTime updatedAt
}

BORROWER {
    Long id PK
    String name
    String email
    String phone
    LocalDateTime createdAt
}

LENDING 
    Long id PK
    Long bookId FK
    Long borrowerId FK
    LocalDate issueDate
    LocalDate dueDate
    LocalDate returnDate
    Double fineAmount
    LendingStatus status
}
```

# API Specification

> *Note: Listed endpoints cover the baseline Controllers (superset of the minimum 15).*

#| BookController (6 endpoints)

| HTTP Method | Path | Description | Request Body | Response | Status Code |
||----|----|---|----|---|----|
| POST | /api/books | Create a book | JSON BookCreateRequest | Book | 201 |
| GET | /api/books | List books (paged) | – | Page<Book> | 200 |
|GET | /api/books/{id} | Get book by id | — | Book | 200/404 |
| PUT | /api/books/{id} | Update book | JSON BookUpdateRequest | Book | 200/404 |
|DELETE | /api/books/{id} | Delete book | — | — | 204/404 |
| GET | /api/books/search | Search books (author, genre, status, isbn) | Query params | List<Book> | 200 |

# BorrowerController (3 endpoints)

| HTTP Method | Path | Description | Request Body | Response | Status Code |
||----|----|---|----|---|----|
| POST | /api/borrowers | Create register borrower | JSON BorrowerCreateRequest | Borrower | 201 |
| GET | /api/borrowers | List borrowers (paged) | — | Page<Borrower> | 200 |
| GET | /api/borrowers/{id} | Get borrower by id | – | Borrower | 200/404 |

# LendingController (6 endpoints)

| HTTP Method | Path | Description | Request Body | Response | Status Code |
||----|----|-----|-----|-----|-----|
| POST | /api/lending/issue | Issue a book to a borrower | JSON IssueRequest {bookId, borrowerId} | Lending | 201/400 |
| POST | /api/lending/{id}/return | Return a book | — | Lending (max include fineAmount) | 200/404 |
|GET | /api/lending | List lending records | Query params: status, borrowerId, bookId | List<Lending> | 200 |
| GET | /api/lending/{id} | Get lending by id | – | Lending | 200/404 |
| POST | /api/lending/{id}/renew | Renew a lending (extend dueDate) | – | Lending | 200/400/404 |
| POST | /api/lending/{id}/pay-fine | **LIB-12** Pay fine for a lending record | JSON PayFineRequest {paymentMethod} | FinePaymentResult | 200/404/400 |

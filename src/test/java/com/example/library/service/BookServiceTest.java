package com.example.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.library.dto.BookDTO;
import com.example.library.exception.BookNotAvailableException;
import com.example.library.exception.BookNotFoundException;
import com.example.library.exception.DuplicateIsbnException;
import com.example.library.model.Book;
import com.example.library.model.Book.BookStatus;
import com.example.library.model.Book.Genre;
import com.example.library.repository.BookRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    // ── LIB-13: duplicate ISBN → 409 ─────────────────────────────────────────

    @Test
    void createBook_throwsDuplicateIsbnException_whenIsbnAlreadyExists() {
        BookDTO.BookRequest request = new BookDTO.BookRequest(
                "Clean Architecture",
                "Robert C. Martin",
                "978-0134494166",
                Genre.TECHNOLOGY,
                2017
        );

        when(bookRepository.existsByIsbnIgnoreCase("978-0134494166")).thenReturn(true);

        assertThatThrownBy(() -> bookService.createBook(request))
                .isInstanceOf(DuplicateIsbnException.class)
                .hasMessageContaining("978-0134494166");
    }

    // ── LIB-4: default status AVAILABLE ──────────────────────────────────────

    @Test
    void createBook_setsStatusToAvailable_byDefault() {
        BookDTO.BookRequest request = new BookDTO.BookRequest(
                "The Pragmatic Programmer",
                "David Thomas",
                "978-0135957059",
                Genre.TECHNOLOGY,
                1999
        );

        Book saved = Book.builder()
                .id(1L)
                .title(request.title())
                .author(request.author())
                .isbn(request.isbn())
                .genre(request.genre())
                .publishedYear(request.publishedYear())
                .status(BookStatus.AVAILABLE)
                .build();
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());

        when(bookRepository.existsByIsbnIgnoreCase(request.isbn())).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenReturn(saved);

        BookDTO.BookResponse response = bookService.createBook(request);

        assertThat(response.status()).isEqualTo(BookStatus.AVAILABLE);
    }

    // ── LIB-15: audit trail ───────────────────────────────────────────────────

    @Test
    void createBook_populatesCreatedAtAndUpdatedAt_andTheyAreEqual() {
        BookDTO.BookRequest request = new BookDTO.BookRequest(
                "Clean Code",
                "Robert C. Martin",
                "978-0132350884",
                Genre.TECHNOLOGY,
                2008
        );

        LocalDateTime now = LocalDateTime.now();
        Book saved = Book.builder()
                .id(2L)
                .title(request.title())
                .author(request.author())
                .isbn(request.isbn())
                .genre(request.genre())
                .publishedYear(request.publishedYear())
                .status(BookStatus.AVAILABLE)
                .build();
        saved.setCreatedAt(now);
        saved.setUpdatedAt(now);

        when(bookRepository.existsByIsbnIgnoreCase(request.isbn())).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenReturn(saved);

        BookDTO.BookResponse response = bookService.createBook(request);

        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
        assertThat(response.createdAt()).isEqualTo(response.updatedAt());
    }

    // ── LIB-13: unknown id → 404 ─────────────────────────────────────────────

    @Test
    void getBookById_throwsBookNotFoundException_whenIdDoesNotExist() {
        when(bookRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(99999L))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("99999");
    }

    // ── Borrow / return ───────────────────────────────────────────────────────

    @Test
    void borrowBook_throwsBookNotAvailableException_whenBookAlreadyBorrowed() {
        Book borrowedBook = sampleBook();
        borrowedBook.setStatus(BookStatus.BORROWED);
        borrowedBook.setBorrowerName("Alex");
        borrowedBook.setBorrowedAt(LocalDate.now());
        borrowedBook.setDueDate(LocalDate.now().plusDays(7));

        when(bookRepository.findById(1L)).thenReturn(Optional.of(borrowedBook));

        assertThatThrownBy(() -> bookService.borrowBook(1L, new BookDTO.BorrowRequest("Jamie", 10)))
                .isInstanceOf(BookNotAvailableException.class)
                .hasMessageContaining("currently borrowed");
    }

    @Test
    void returnBook_calculatesOverdueFine_andResetsBorrowingState() {
        Book borrowedBook = sampleBook();
        borrowedBook.setStatus(BookStatus.BORROWED);
        borrowedBook.setBorrowerName("Taylor");
        borrowedBook.setBorrowedAt(LocalDate.now().minusDays(10));
        borrowedBook.setDueDate(LocalDate.now().minusDays(3));

        when(bookRepository.findById(1L)).thenReturn(Optional.of(borrowedBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookDTO.ReturnResponse response = bookService.returnBook(1L);

        assertThat(response.fineCharged()).isEqualByComparingTo("7.50");
        assertThat(response.book().status()).isEqualTo(BookStatus.AVAILABLE);
        assertThat(response.book().borrowerName()).isNull();
        assertThat(response.book().borrowedAt()).isNull();
        assertThat(response.book().dueDate()).isNull();
        verify(bookRepository).save(borrowedBook);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Book sampleBook() {
        Book book = Book.builder()
                .id(1L)
                .title("Domain-Driven Design")
                .author("Eric Evans")
                .isbn("978-0321125217")
                .genre(Genre.TECHNOLOGY)
                .publishedYear(2003)
                .status(BookStatus.AVAILABLE)
                .build();
        book.setCreatedAt(LocalDateTime.now().minusDays(5));
        book.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return book;
    }
}

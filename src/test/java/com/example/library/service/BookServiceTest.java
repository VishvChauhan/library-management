package com.example.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.library.dto.BookDTO;
import com.example.library.exception.BookNotAvailableException;
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

    @Test
    void createBookThrowsWhenIsbnAlreadyExists() {
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

    @Test
    void borrowBookThrowsWhenBookAlreadyBorrowed() {
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
    void returnBookCalculatesOverdueFineAndResetsBorrowingState() {
        Book borrowedBook = sampleBook();
        borrowedBook.setStatus(BookStatus.BORROWED);
        borrowedBook.setBorrowerName("Taylor");
        borrowedBook.setBorrowedAt(LocalDate.now().minusDays(10));
        borrowedBook.setDueDate(LocalDate.now().minusDays(3));

        when(bookRepository.findById(1L)).thenReturn(Optional.of(borrowedBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookDTO.ReturnResponse response = bookService.returnBook(1L);

        assertThat(response.fineCharged()).isEqualByComparingTo("7.50");
        assertThat(response.book().status()).isEqualTo(BookStatus.AVAILABLE);
        assertThat(response.book().borrowerName()).isNull();
        assertThat(response.book().borrowedAt()).isNull();
        assertThat(response.book().dueDate()).isNull();
        verify(bookRepository).save(borrowedBook);
    }

    private Book sampleBook() {
        return Book.builder()
                .id(1L)
                .title("Domain-Driven Design")
                .author("Eric Evans")
                .isbn("978-0321125217")
                .genre(Genre.TECHNOLOGY)
                .publishedYear(2003)
                .status(BookStatus.AVAILABLE)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }
}


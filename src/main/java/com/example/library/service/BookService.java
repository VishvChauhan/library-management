package com.example.library.service;

import com.example.library.dto.BookDTO;
import com.example.library.exception.BookNotAvailableException;
import com.example.library.exception.BookNotFoundException;
import com.example.library.exception.DuplicateIsbnException;
import com.example.library.model.Book;
import com.example.library.model.Book.BookStatus;
import com.example.library.model.Book.Genre;
import com.example.library.repository.BookRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookService {

    private static final BigDecimal DAILY_FINE = new BigDecimal("2.50");
    private static final int DEFAULT_BORROWING_DAYS = 14;

    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public Page<BookDTO.BookResponse> getAllBooks(String keyword, BookStatus status, Genre genre, Pageable pageable) {
        return bookRepository.search(keyword, status, genre, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BookDTO.BookResponse getBookById(Long id) {
        return toResponse(getEntityById(id));
    }

    public BookDTO.BookResponse createBook(BookDTO.BookRequest request) {
        String normalizedIsbn = normalizeIsbn(request.isbn());
        if (bookRepository.existsByIsbnIgnoreCase(normalizedIsbn)) {
            throw new DuplicateIsbnException(normalizedIsbn);
        }

        Book book = Book.builder()
                .title(request.title().trim())
                .author(request.author().trim())
                .isbn(normalizedIsbn)
                .genre(request.genre())
                .publishedYear(request.publishedYear())
                .status(BookStatus.AVAILABLE)
                .build();

        return toResponse(bookRepository.save(book));
    }

    public BookDTO.BookResponse updateBook(Long id, BookDTO.BookRequest request) {
        Book book = getEntityById(id);
        String normalizedIsbn = normalizeIsbn(request.isbn());

        bookRepository.findByIsbnIgnoreCase(normalizedIsbn)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateIsbnException(normalizedIsbn);
                });

        book.setTitle(request.title().trim());
        book.setAuthor(request.author().trim());
        book.setIsbn(normalizedIsbn);
        book.setGenre(request.genre());
        book.setPublishedYear(request.publishedYear());

        return toResponse(bookRepository.save(book));
    }

    public void deleteBook(Long id) {
        Book book = getEntityById(id);
        bookRepository.delete(book);
    }

    public BookDTO.BookResponse borrowBook(Long id, BookDTO.BorrowRequest request) {
        Book book = getEntityById(id);
        if (book.getStatus() == BookStatus.BORROWED) {
            throw new BookNotAvailableException("Book is currently borrowed by another reader");
        }

        LocalDate borrowedAt = LocalDate.now();
        int borrowingDays = request.borrowingDays() == null ? DEFAULT_BORROWING_DAYS : request.borrowingDays();

        book.setStatus(BookStatus.BORROWED);
        book.setBorrowerName(request.borrowerName().trim());
        book.setBorrowedAt(borrowedAt);
        book.setDueDate(borrowedAt.plusDays(borrowingDays));

        return toResponse(bookRepository.save(book));
    }

    public BookDTO.ReturnResponse returnBook(Long id) {
        Book book = getEntityById(id);
        if (book.getStatus() != BookStatus.BORROWED) {
            throw new BookNotAvailableException("Book is not currently borrowed");
        }

        BigDecimal fine = calculateFine(book.getDueDate(), LocalDate.now());
        book.setStatus(BookStatus.AVAILABLE);
        book.setBorrowerName(null);
        book.setBorrowedAt(null);
        book.setDueDate(null);

        Book savedBook = bookRepository.save(book);
        return new BookDTO.ReturnResponse(
                fine.compareTo(BigDecimal.ZERO) > 0 ? "Book returned with overdue fine" : "Book returned successfully",
                fine,
                toResponse(savedBook)
        );
    }

    @Transactional(readOnly = true)
    public java.util.List<BookDTO.BookResponse> getOverdueBooks() {
        return bookRepository.findAllByStatusAndDueDateBefore(BookStatus.BORROWED, LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Book getEntityById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    private String normalizeIsbn(String isbn) {
        return isbn.trim().toUpperCase();
    }

    private BookDTO.BookResponse toResponse(Book book) {
        return new BookDTO.BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getGenre(),
                book.getPublishedYear(),
                book.getStatus(),
                book.getBorrowerName(),
                book.getBorrowedAt(),
                book.getDueDate(),
                calculateFine(book.getDueDate(), LocalDate.now()),
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }

    private BigDecimal calculateFine(LocalDate dueDate, LocalDate currentDate) {
        if (dueDate == null || currentDate == null || !currentDate.isAfter(dueDate)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long overdueDays = ChronoUnit.DAYS.between(dueDate, currentDate);
        return DAILY_FINE.multiply(BigDecimal.valueOf(overdueDays)).setScale(2, RoundingMode.HALF_UP);
    }
}


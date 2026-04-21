package com.example.library.dto;

import com.example.library.model.Book.BookStatus;
import com.example.library.model.Book.Genre;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class BookDTO {

    private BookDTO() {
    }

    public record BookRequest(
            @NotBlank(message = "Title is required")
            String title,
            @NotBlank(message = "Author is required")
            String author,
            @NotBlank(message = "ISBN is required")
            @Pattern(regexp = "^[0-9X-]{10,17}$", message = "ISBN must be 10 to 17 characters and contain only digits, X, or hyphen")
            String isbn,
            @NotNull(message = "Genre is required")
            Genre genre,
            @NotNull(message = "Published year is required")
            @Min(value = 1450, message = "Published year must be after 1450")
            @Max(value = 2100, message = "Published year must be before 2100")
            Integer publishedYear
    ) {
    }

    public record BorrowRequest(
            @NotBlank(message = "Borrower name is required")
            String borrowerName,
            @Min(value = 1, message = "Borrowing days must be at least 1")
            @Max(value = 60, message = "Borrowing days cannot exceed 60")
            Integer borrowingDays
    ) {
    }

    public record BookResponse(
            Long id,
            String title,
            String author,
            String isbn,
            Genre genre,
            Integer publishedYear,
            BookStatus status,
            String borrowerName,
            LocalDate borrowedAt,
            LocalDate dueDate,
            BigDecimal currentFine,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record ReturnResponse(
            String message,
            BigDecimal fineCharged,
            BookResponse book
    ) {
    }
}


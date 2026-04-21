package com.example.library.controller;

import com.example.library.dto.BookDTO;
import com.example.library.model.Book.BookStatus;
import com.example.library.model.Book.Genre;
import com.example.library.service.BookService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<Page<BookDTO.BookResponse>> getBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BookStatus status,
            @RequestParam(required = false) Genre genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort.Direction sortDirection = Sort.Direction.fromOptionalString(direction).orElse(Sort.Direction.ASC);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(bookService.getAllBooks(keyword, status, genre, pageRequest));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<BookDTO.BookResponse>> getOverdueBooks() {
        return ResponseEntity.ok(bookService.getOverdueBooks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDTO.BookResponse> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @PostMapping
    public ResponseEntity<BookDTO.BookResponse> createBook(@Valid @RequestBody BookDTO.BookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBook(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookDTO.BookResponse> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookDTO.BookRequest request) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/borrow")
    public ResponseEntity<BookDTO.BookResponse> borrowBook(
            @PathVariable Long id,
            @Valid @RequestBody BookDTO.BorrowRequest request) {
        return ResponseEntity.ok(bookService.borrowBook(id, request));
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<BookDTO.ReturnResponse> returnBook(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.returnBook(id));
    }
}


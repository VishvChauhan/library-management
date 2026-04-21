package com.example.library.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.dto.BookDTO;
import com.example.library.exception.GlobalExceptionHandler;
import com.example.library.model.Book.BookStatus;
import com.example.library.model.Book.Genre;
import com.example.library.service.BookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookController.class)
@Import(GlobalExceptionHandler.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookService bookService;

    @Test
    void getBooksReturnsPagedResponse() throws Exception {
        BookDTO.BookResponse response = new BookDTO.BookResponse(
                1L,
                "Clean Code",
                "Robert C. Martin",
                "978-0132350884",
                Genre.TECHNOLOGY,
                2008,
                BookStatus.AVAILABLE,
                null,
                null,
                null,
                new BigDecimal("0.00"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
        );

        when(bookService.getAllBooks(eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(response), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.content[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createBookReturnsCreatedResponse() throws Exception {
        BookDTO.BookRequest request = new BookDTO.BookRequest(
                "Refactoring",
                "Martin Fowler",
                "978-0201485677",
                Genre.TECHNOLOGY,
                1999
        );

        BookDTO.BookResponse response = new BookDTO.BookResponse(
                10L,
                request.title(),
                request.author(),
                request.isbn(),
                request.genre(),
                request.publishedYear(),
                BookStatus.AVAILABLE,
                null,
                null,
                null,
                new BigDecimal("0.00"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(bookService.createBook(any(BookDTO.BookRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.isbn").value("978-0201485677"))
                .andExpect(jsonPath("$.genre").value("TECHNOLOGY"));
    }
}



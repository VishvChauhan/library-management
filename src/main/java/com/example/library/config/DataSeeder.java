package com.example.library.config;

import com.example.library.model.Book;
import com.example.library.model.Book.Genre;
import com.example.library.repository.BookRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedBooks(BookRepository bookRepository) {
        return args -> {
            if (bookRepository.count() > 0) {
                return;
            }

            List<Book> books = List.of(
                    createBook("The Pragmatic Programmer", "Andrew Hunt", "978-0201616224", Genre.TECHNOLOGY, 1999),
                    createBook("Clean Code", "Robert C. Martin", "978-0132350884", Genre.TECHNOLOGY, 2008),
                    createBook("1984", "George Orwell", "978-0451524935", Genre.FICTION, 1949),
                    createBook("Dune", "Frank Herbert", "978-0441172719", Genre.SCIENCE_FICTION, 1965),
                    createBook("The Hobbit", "J.R.R. Tolkien", "978-0547928227", Genre.FANTASY, 1937),
                    createBook("Sapiens", "Yuval Noah Harari", "978-0062316097", Genre.HISTORY, 2011),
                    createBook("Becoming", "Michelle Obama", "978-1524763138", Genre.BIOGRAPHY, 2018),
                    createBook("Atomic Habits", "James Clear", "978-0735211292", Genre.SELF_HELP, 2018)
            );

            bookRepository.saveAll(books);
        };
    }

    private Book createBook(String title, String author, String isbn, Genre genre, int publishedYear) {
        return Book.builder()
                .title(title)
                .author(author)
                .isbn(isbn)
                .genre(genre)
                .publishedYear(publishedYear)
                .status(Book.BookStatus.AVAILABLE)
                .build();
    }
}


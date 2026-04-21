package com.example.library.repository;

import com.example.library.model.Book;
import com.example.library.model.Book.BookStatus;
import com.example.library.model.Book.Genre;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbnIgnoreCase(String isbn);

    boolean existsByIsbnIgnoreCase(String isbn);

    @Query("""
            select b from Book b
            where (:status is null or b.status = :status)
              and (:genre is null or b.genre = :genre)
              and (
                    :keyword is null
                    or trim(:keyword) = ''
                    or lower(b.title) like lower(concat('%', :keyword, '%'))
                    or lower(b.author) like lower(concat('%', :keyword, '%'))
                    or lower(b.isbn) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<Book> search(@Param("keyword") String keyword,
                      @Param("status") BookStatus status,
                      @Param("genre") Genre genre,
                      Pageable pageable);

    List<Book> findAllByStatusAndDueDateBefore(BookStatus status, LocalDate dueDate);
}


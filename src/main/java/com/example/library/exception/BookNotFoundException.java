package com.example.library.exception;

public class BookNotFoundException extends ResourceNotFoundException {

    public BookNotFoundException(Long id) {
        super("Book with id " + id + " was not found");
    }
}


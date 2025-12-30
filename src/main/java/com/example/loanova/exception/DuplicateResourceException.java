package com.example.loanova.exception;

/* Untuk exception jika ada data yang duplikat */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}

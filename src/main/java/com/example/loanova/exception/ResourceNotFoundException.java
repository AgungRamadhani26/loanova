package com.example.loanova.exception;

/* Untuk exception jika data tidak ditemukan */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

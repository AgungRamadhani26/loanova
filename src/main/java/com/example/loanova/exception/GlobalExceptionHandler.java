package com.example.loanova.exception;

import com.example.loanova.base.ApiResponse;
import com.example.loanova.util.ResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /* Untuk exception jika data tidak ditemukan 404 */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(
            ResourceNotFoundException ex) {
        return ResponseUtil.error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /* Untuk exception jika ada data yang duplikat 409 */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicate(
            DuplicateResourceException ex) {
        return ResponseUtil.error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /* Untuk exception validasi input 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationError(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseUtil.error(
                HttpStatus.BAD_REQUEST,
                "Validasi gagal",
                Map.of("errors", errors));
    }

    /* Untuk exception jika ada kesalahan bisnis / validasi 400 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(
            BusinessException ex) {
        return ResponseUtil.error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handle Access Denied - 403 Forbidden
     * Ketika user tidak punya akses ke endpoint (role tidak sesuai)
     * Contoh: MARKETING coba akses endpoint SUPERADMIN
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(
            AccessDeniedException ex) {
        return ResponseUtil.error(
                HttpStatus.FORBIDDEN,
                "Anda tidak memiliki akses untuk mengakses resource ini");
    }

    /**
     * Handle Authentication - 401 Unauthorized
     * Ketika user belum login / token invalid
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthentication(
            AuthenticationException ex) {
        return ResponseUtil.error(
                HttpStatus.UNAUTHORIZED,
                "Autentikasi gagal. Silakan login terlebih dahulu");
    }

    /* Untuk exception lainnya */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneral(Exception ex) {
        return ResponseUtil.error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Terjadi kesalahan pada server");
    }
}

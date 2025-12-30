package com.example.loanova.controller;

import com.example.loanova.base.ApiResponse;
import com.example.loanova.dto.request.LoginRequest;
import com.example.loanova.dto.request.LogoutRequest;
import com.example.loanova.dto.request.RefreshTokenRequest;
import com.example.loanova.dto.response.AuthResponse;
import com.example.loanova.service.AuthService;
import com.example.loanova.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

   private final AuthService authService;

   /**
    * LOGIN ENDPOINT
    * 
    * Endpoint untuk login user
    * 
    * Request:
    * POST /api/auth/login
    * Body: { "username": "...", "password": "..." }
    * 
    * Response:
    * {
    *   "success": true,
    *   "message": "Login berhasil",
    *   "data": {
    *     "accessToken": "eyJhbGc...",
    *     "refreshToken": "abc123...",
    *     "type": "Bearer",
    *     "username": "SUMUT01",
    *     "roles": ["ADMIN"]
    *   }
    * }
    */
   @PostMapping("/login")
   public ResponseEntity<ApiResponse<AuthResponse>> login(
         @Valid @RequestBody LoginRequest request) {
      AuthResponse authResponse = authService.login(request);
      return ResponseUtil.success(authResponse, "Login berhasil", HttpStatus.OK);
   }

   /**
    * REFRESH TOKEN ENDPOINT
    * 
    * Endpoint untuk generate access token baru pakai refresh token
    * 
    * Flow:
    * 1. Access token expired (15 menit)
    * 2. Frontend detect 401 error
    * 3. Frontend call POST /api/auth/refresh dengan refresh token
    * 4. Backend validate refresh token (signature, expiration, blacklist)
    * 5. Backend generate access token baru
    * 6. Frontend pakai access token baru
    * 
    * Request:
    * POST /api/auth/refresh
    * Body: { "refreshToken": "abc123..." }
    * 
    * Response Success:
    * {
    *   "success": true,
    *   "message": "Token berhasil di-refresh",
    *   "data": {
    *     "accessToken": "eyJhbGc...",  // Access token BARU
    *     "refreshToken": "abc123...",   // Refresh token SAMA (tidak perlu generate ulang)
    *     "type": "Bearer",
    *     "username": "SUMUT01",
    *     "roles": ["ADMIN"]
    *   }
    * }
    * 
    * Response Error (401):
    * - Refresh token invalid/expired
    * - Refresh token sudah di-logout (blacklist)
    * - User tidak ditemukan
    */
   @PostMapping("/refresh")
   public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
         @Valid @RequestBody RefreshTokenRequest request) {
      AuthResponse authResponse = authService.refreshAccessToken(request.getRefreshToken());
      return ResponseUtil.success(authResponse, "Token berhasil di-refresh", HttpStatus.OK);
   }

   /**
    * LOGOUT ENDPOINT
    * 
    * Endpoint untuk logout user (delete refresh token dari database)
    * 
    * Flow:
    * 1. User click logout
    * 2. Frontend kirim POST /api/auth/logout dengan refresh token di body
    * 3. Backend delete refresh token dari database
    * 4. Refresh token tidak bisa dipakai lagi untuk generate access token baru
    * 
    * Note: Access token masih bisa dipakai sampai expired (15 menit).
    * Untuk security lebih baik, sebaiknya pakai token blacklist dengan Redis.
    * 
    * Request:
    * POST /api/auth/logout
    * Body: { "refreshToken": "abc123..." }
    * 
    * Response Success:
    * {
    *   "success": true,
    *   "message": "Logout berhasil",
    *   "data": null
    * }
    */
   @PostMapping("/logout")
   public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
      // Delete refresh token dari database
      authService.logout(request.getRefreshToken());
      
      return ResponseUtil.success(null, "Logout berhasil", HttpStatus.OK);
   }
}

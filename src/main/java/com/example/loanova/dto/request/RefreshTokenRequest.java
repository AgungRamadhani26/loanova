package com.example.loanova.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REFRESH TOKEN REQUEST DTO
 * 
 * DTO untuk request refresh token (generate access token baru)
 * 
 * Flow:
 * 1. Access token expired (15 menit)
 * 2. Frontend detect 401 error
 * 3. Frontend call POST /api/auth/refresh dengan refresh token
 * 4. Backend validate refresh token
 * 5. Backend generate access token baru
 * 6. Frontend pakai access token baru untuk request berikutnya
 * 
 * Kenapa perlu refresh token?
 * - Access token lifetime pendek (15 menit) untuk security
 * - Refresh token lifetime panjang (7 hari) untuk UX
 * - User tidak perlu login ulang setiap 15 menit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

   /**
    * Refresh token dari login response
    * 
    * Validasi:
    * - @NotBlank: tidak boleh null, empty, atau whitespace
    */
   @NotBlank(message = "Refresh token tidak boleh kosong")
   private String refreshToken;
}

package com.example.loanova.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT SERVICE - Service untuk generate dan validate JWT tokens
 * 
 * JWT (JSON Web Token) terdiri dari 3 bagian:
 * 1. Header: Algorithm & token type
 * 2. Payload: Claims (data user, expiration, dll)
 * 3. Signature: Verify token tidak diubah
 * 
 * Fungsi service ini:
 * 1. Generate access token (15 menit)
 * 2. Generate refresh token (7 hari)
 * 3. Validate token (signature, expiration, username)
 * 4. Extract data dari token (username, claims, expiration)
 */
@Service
public class JwtService {

    // Secret key untuk sign & verify JWT
    // HARUS minimal 256-bit (32 karakter) untuk HMAC-SHA256
    // Di production: simpan di environment variable, JANGAN hardcode!
    @Value("${jwt.secret}")
    private String secret;

    // Durasi access token dalam milliseconds
    // Default: 900000ms = 15 menit
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    // Durasi refresh token dalam milliseconds
    // Default: 604800000ms = 7 hari
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * GENERATE ACCESS TOKEN - Token untuk akses API (short-lived)
     * 
     * Access token:
     * - Durasi: 15 menit
     * - Dipakai di header Authorization untuk setiap request
     * - Kalau expired: client harus generate baru pakai refresh token
     * 
     * @param userDetails User yang login
     * @return JWT access token
     */
    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, accessTokenExpiration);
    }

    /**
     * GENERATE ACCESS TOKEN dengan custom claims
     * 
     * Custom claims = data tambahan yang mau disimpan di JWT
     * Contoh: email, fullName, userId, permissions, dll
     * 
     * PERHATIAN:
     * - Jangan simpan data sensitif (password, credit card)
     * - JWT bisa di-decode (not encrypted, only signed)
     * - Semakin banyak claims, semakin besar size token
     * 
     * @param extraClaims Data tambahan untuk disimpan di token
     * @param userDetails User yang login
     * @return JWT access token dengan custom claims
     */
    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return generateToken(extraClaims, userDetails, accessTokenExpiration);
    }

    /**
     * GENERATE REFRESH TOKEN - Token untuk generate access token baru (long-lived)
     * 
     * Refresh token:
     * - Durasi: 7 hari
     * - Disimpan di database untuk bisa di-revoke
     * - Dipakai di endpoint /api/auth/refresh untuk generate access token baru
     * - Lebih aman karena bisa di-track & revoke
     * 
     * @param userDetails User yang login
     * @return JWT refresh token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, refreshTokenExpiration);
    }

    /**
     * GENERATE TOKEN - Method generic untuk generate JWT
     * 
     * JWT Structure:
     * Header.Payload.Signature
     * 
     * Payload (Claims):
     * - extraClaims: custom data (roles, email, dll)
     * - subject: username/email user
     * - issuedAt: waktu token dibuat
     * - expiration: waktu token expired
     * 
     * Signature:
     * - Sign dengan HMAC-SHA256 + secret key
     * - Verify token tidak diubah
     * 
     * @param extraClaims Custom data untuk disimpan di token
     * @param userDetails User info (username, roles)
     * @param expiration  Durasi token dalam milliseconds
     * @return JWT token string
     */
    private String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration) {
        return Jwts.builder()
                .claims(extraClaims) // Custom claims (optional)
                .subject(userDetails.getUsername()) // Subject = username/email (registered claim)
                .issuedAt(new Date(System.currentTimeMillis())) // iat = issued at (registered claim)
                .expiration(new Date(System.currentTimeMillis() + expiration)) // exp = expiration (registered claim)
                .signWith(getSigningKey()) // Sign dengan HMAC-SHA256
                .compact(); // Build JWT string
    }

    /**
     * VALIDATE TOKEN - Check apakah token valid
     * 
     * Validation checks:
     * 1. Signature: apakah token di-sign dengan secret key kita?
     * 2. Username: apakah username di token = username di DB?
     * 3. Expiration: apakah token sudah expired?
     * 
     * @param token       JWT token string
     * @param userDetails User dari database
     * @return true kalau valid, false kalau invalid
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        // Check username match DAN token belum expired
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * EXTRACT USERNAME - Ambil username dari token
     * 
     * Username disimpan di claim "subject" (sub)
     * 
     * @param token JWT token string
     * @return Username dari token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * EXTRACT EXPIRATION - Ambil expiration date dari token
     * 
     * @param token JWT token string
     * @return Expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * EXTRACT SPECIFIC CLAIM - Ambil claim tertentu dari token
     * 
     * Claims = data yang disimpan di JWT payload
     * Contoh claims:
     * - subject (sub): username
     * - expiration (exp): waktu expired
     * - issuedAt (iat): waktu dibuat
     * - custom claims: email, roles, dll
     * 
     * @param token          JWT token string
     * @param claimsResolver Function untuk extract claim
     * @return Claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * EXTRACT ALL CLAIMS - Parse JWT dan ambil semua claims
     * 
     * Process:
     * 1. Parse JWT string
     * 2. Verify signature dengan secret key
     * 3. Return payload (claims)
     * 
     * Exception:
     * - SignatureException: kalau signature invalid (token diubah)
     * - ExpiredJwtException: kalau token sudah expired
     * - MalformedJwtException: kalau format token salah
     * 
     * @param token JWT token string
     * @return All claims dari token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // Verify signature
                .build()
                .parseSignedClaims(token) // Parse JWT
                .getPayload(); // Get claims (payload)
    }

    /**
     * CHECK TOKEN EXPIRED - Apakah token sudah expired?
     * 
     * @param token JWT token string
     * @return true kalau expired, false kalau masih valid
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * GET TOKEN EXPIRATION IN SECONDS - Hitung sisa waktu sampai token expired
     * 
     * Method ini dipakai untuk set TTL di Redis blacklist:
     * - Saat logout, token disimpan di blacklist dengan TTL = sisa waktu token
     * - Setelah token expired, auto-delete dari Redis (hemat memory)
     * 
     * @param token JWT token string
     * @return Sisa waktu dalam detik sampai token expired (0 kalau sudah expired)
     */
    public long getTokenExpirationInSeconds(String token) {
        // Ambil expiration date dari token
        Date expirationDate = extractExpiration(token);

        // Hitung selisih waktu: expiration - sekarang
        long expirationTimeMillis = expirationDate.getTime() - System.currentTimeMillis();

        // Convert milliseconds ke seconds
        // Math.max(0, ...) untuk pastikan return >= 0 (kalau token sudah expired)
        return Math.max(0, expirationTimeMillis / 1000);
    }

    /**
     * GET SIGNING KEY - Convert secret string ke SecretKey untuk HMAC-SHA256
     * 
     * Process:
     * 1. Convert secret string ke byte array dengan UTF-8 encoding
     * 2. Create SecretKey untuk HMAC-SHA256 algorithm
     * 
     * PENTING:
     * - Secret HARUS minimal 256-bit (32 karakter UTF-8)
     * - Secret disimpan plain text di application.properties (NOT Base64)
     * - Di production: pakai environment variable!
     * 
     * @return SecretKey untuk sign & verify JWT
     */
    private SecretKey getSigningKey() {
        // Convert secret string ke byte array dengan UTF-8 encoding
        // BUKAN Base64 decode! Secret disimpan plain text.
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        // Create HMAC-SHA256 key dari byte array
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

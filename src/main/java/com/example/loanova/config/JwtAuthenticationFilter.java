package com.example.loanova.config;

import com.example.loanova.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT AUTHENTICATION FILTER
 * 
 * Filter ini DIJALANKAN DI SETIAP REQUEST sebelum masuk ke controller
 * 
 * Fungsi filter ini:
 * 1. Extract JWT token dari header "Authorization: Bearer <token>"
 * 2. Validate JWT token (signature, expiration, dll)
 * 3. Kalau valid: Set Authentication di SecurityContext
 * 4. Kalau invalid/tidak ada: Request tetap lanjut (Spring Security yang block
 * kalau endpoint protected)
 * 
 * OncePerRequestFilter = Filter ini dijamin cuma jalan 1x per request
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Service untuk generate & validate JWT
    private final JwtService jwtService;

    // Service untuk load user dari database
    // Pakai interface UserDetailsService, bukan class AuthService
    // Supaya tidak circular dependency
    private final UserDetailsService userDetailsService;

    /**
     * METHOD UTAMA FILTER - Dipanggil otomatis setiap ada request
     * 
     * Flow:
     * 1. Extract token dari header
     * 2. Check blacklist (token sudah logout?)
     * 3. Validate token (signature, expiration)
     * 4. Set authentication kalau valid
     * 5. Lanjut ke filter berikutnya / controller
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // STEP 1: Ambil header Authorization
        // Format: "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // STEP 2: Check apakah header Authorization ada dan formatnya benar
        // Kalau tidak ada / format salah → skip filter ini, lanjut ke next filter
        // Spring Security akan block kalau endpoint nya protected
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // STEP 3: Extract JWT token (hapus "Bearer " prefix)
        // "Bearer eyJhbGc..." → "eyJhbGc..."
        jwt = authHeader.substring(7);

        try {
            // STEP 4: Extract username dari JWT
            // JWT di-decode, ambil claim "subject" (username/email)
            username = jwtService.extractUsername(jwt);

            // STEP 5: Check apakah username valid dan user belum di-authenticate
            // SecurityContext.getAuthentication() == null artinya user belum login di
            // request ini
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // STEP 6: Load user dari database
                // userDetailsService akan call authService.loadUserByUsername()
                // Return UserDetails dengan username, password, roles
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // STEP 7: Validate JWT token
                // Check: username di token = username di DB?
                // Check: token sudah expired atau belum?
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // STEP 8: Create Authentication object
                    // UsernamePasswordAuthenticationToken adalah implementation dari Authentication
                    // Parameters:
                    // - userDetails: info user (username, roles)
                    // - null: credentials (tidak perlu, sudah validated via JWT)
                    // - authorities: roles user (ROLE_ADMIN, ROLE_USER, dll)
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities() // Roles untuk authorization
                    );

                    // Set additional details (IP address, session ID, dll)
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    // STEP 9: Set authentication di SecurityContext
                    // Setelah ini, Spring Security tahu user sudah login
                    // @PreAuthorize bisa check roles dari getAuthorities()
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Kalau ada error (invalid token, user not found, dll)
            // Log error tapi tidak throw exception
            // Biar request tetap lanjut, Spring Security yang handle authorization
            logger.error("Cannot set user authentication: {}", e);
        }

        // STEP 10: Lanjutkan ke filter berikutnya / controller
        // Kalau authentication berhasil di-set, user bisa akses protected endpoints
        // Kalau gagal, Spring Security akan return 401/403
        filterChain.doFilter(request, response);
    }
}

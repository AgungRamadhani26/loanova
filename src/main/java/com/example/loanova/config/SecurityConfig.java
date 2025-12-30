package com.example.loanova.config;

import com.example.loanova.exception.CustomAccessDeniedHandler;
import com.example.loanova.exception.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SECURITY CONFIGURATION - Main konfigurasi Spring Security
 * 
 * Fungsi file ini:
 * 1. Enable Spring Security untuk seluruh aplikasi (@EnableWebSecurity)
 * 2. Enable method-level security untuk @PreAuthorize (@EnableMethodSecurity)
 * 3. Configure URL mana yang public, mana yang perlu authentication
 * 4. Setup JWT filter untuk validate token di setiap request
 * 5. Set session jadi STATELESS (no server-side session, pakai JWT)
 */
@Configuration
@EnableWebSecurity  // Enable Spring Security
@EnableMethodSecurity  // Enable @PreAuthorize, @Secured, dll
@RequiredArgsConstructor
public class SecurityConfig {

    // Inject AuthenticationProvider dari AuthenticationConfig
    // Provider ini yang melakukan validasi username/password
    private final AuthenticationProvider authenticationProvider;
    
    // Custom handler untuk 403 Forbidden
    private final CustomAccessDeniedHandler accessDeniedHandler;
    
    // Custom handler untuk 401 Unauthorized
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Bean untuk AuthenticationManager
     * Dipakai di AuthService untuk authenticate user saat login
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * SECURITY FILTER CHAIN - Konfigurasi utama security
     * 
     * Method ini define:
     * - URL mana yang public (permitAll)
     * - URL mana yang perlu authentication
     * - Filter apa yang dipakai
     * - Session management
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
                // Disable CSRF karena kita pakai JWT (stateless)
                // CSRF protection tidak diperlukan untuk REST API dengan JWT
                .csrf(csrf -> csrf.disable())
                
                // AUTHORIZATION RULES - Define endpoint mana yang public/protected
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - Tidak perlu login
                        // Semua endpoint di /api/auth/** bisa diakses tanpa token
                        // Contoh: /api/auth/login, /api/auth/register, /api/auth/logout
                        .requestMatchers("/api/auth/**").permitAll()
                        
                        // Protected endpoints - Harus login (punya valid JWT token)
                        // Semua endpoint lain butuh authentication
                        // Contoh: /api/users, /api/branches, dll
                        .anyRequest().authenticated())
                
                // SESSION MANAGEMENT - Set jadi STATELESS
                // STATELESS = tidak ada session di server
                // Semua info user disimpan di JWT token
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // EXCEPTION HANDLING - Custom error response untuk 401 & 403
                // Tanpa ini, Spring Security return HTML error page
                .exceptionHandling(exception -> exception
                        // 403 Forbidden - User tidak punya akses (role tidak sesuai)
                        .accessDeniedHandler(accessDeniedHandler)
                        // 401 Unauthorized - User belum login / token invalid
                        .authenticationEntryPoint(authenticationEntryPoint))
                
                // Set authentication provider (dari AuthenticationConfig)
                .authenticationProvider(authenticationProvider)
                
                // ADD JWT FILTER - Filter ini jalan sebelum Spring Security check authentication
                // JwtAuthenticationFilter akan:
                // 1. Extract JWT dari header Authorization
                // 2. Validate JWT
                // 3. Set Authentication di SecurityContext
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

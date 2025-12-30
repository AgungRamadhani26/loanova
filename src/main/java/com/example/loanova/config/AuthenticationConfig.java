package com.example.loanova.config;

import com.example.loanova.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * AUTHENTICATION CONFIGURATION
 * 
 * File ini dibuat TERPISAH dari SecurityConfig untuk menghindari circular dependency
 * 
 * Fungsi file ini:
 * 1. Define PasswordEncoder (BCrypt) untuk encrypt/compare password
 * 2. Define AuthenticationProvider untuk validasi login
 * 3. Link antara Spring Security dengan database kita (via AuthService)
 */
@Configuration
@RequiredArgsConstructor
public class AuthenticationConfig {

   // AuthService implements UserDetailsService
   // Dipakai untuk load user dari database saat authentication
   private final AuthService authService;

   /**
    * PASSWORD ENCODER - Untuk encrypt dan validate password
    * 
    * BCrypt adalah algoritma hashing yang secure untuk password
    * Saat register: password di-hash pakai BCrypt sebelum disimpan ke DB
    * Saat login: password input di-compare dengan hash di DB
    */
   @Bean
   public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
   }

   /**
    * AUTHENTICATION PROVIDER - Yang validate username & password saat login
    * 
    * DaoAuthenticationProvider akan:
    * 1. Load user dari DB via authService.loadUserByUsername()
    * 2. Compare password input dengan password di DB pakai passwordEncoder
    * 3. Return Authentication kalau valid, throw exception kalau invalid
    */
   @Bean
   public AuthenticationProvider authenticationProvider() {
      // Constructor injection untuk Spring Security 7.0.2+
      DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(authService);
      authProvider.setPasswordEncoder(passwordEncoder());
      return authProvider;
   }
}

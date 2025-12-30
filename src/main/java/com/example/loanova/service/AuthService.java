package com.example.loanova.service;

import com.example.loanova.dto.request.LoginRequest;
import com.example.loanova.dto.response.AuthResponse;
import com.example.loanova.entity.RefreshToken;
import com.example.loanova.entity.Role;
import com.example.loanova.entity.User;
import com.example.loanova.exception.BusinessException;
import com.example.loanova.repository.RefreshTokenRepository;
import com.example.loanova.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * AUTH SERVICE - Service untuk handle authentication (login, logout, refresh,
 * dll)
 * 
 * Class ini implements UserDetailsService:
 * - Interface dari Spring Security untuk load user dari database
 * - Method loadUserByUsername() dipanggil saat authentication
 * 
 * Fungsi service ini:
 * 1. Handle login user
 * 2. Handle refresh token (generate access token baru)
 * 3. Generate access token & refresh token
 * 4. Load user dari database untuk Spring Security
 * 5. Convert entity User → UserDetails (format Spring Security)
 */
@Service
public class AuthService implements UserDetailsService {

      private final UserRepository userRepository;
      private final RefreshTokenRepository refreshTokenRepository;
      private final JwtService jwtService;
      private final AuthenticationManager authenticationManager;

      /**
       * Constructor dengan manual injection
       * 
       * @Lazy pada AuthenticationManager untuk avoid circular dependency:
       *       AuthService → SecurityConfig → AuthenticationManager → AuthService
       */
      public AuthService(
                  UserRepository userRepository,
                  RefreshTokenRepository refreshTokenRepository,
                  JwtService jwtService,
                  @Lazy AuthenticationManager authenticationManager) {
            this.userRepository = userRepository;
            this.refreshTokenRepository = refreshTokenRepository;
            this.jwtService = jwtService;
            this.authenticationManager = authenticationManager;
      }

      /**
       * LOGIN USER - Handle login dan generate JWT tokens
       * 
       * Flow:
       * 1. Validate username & password via AuthenticationManager
       * 2. Load user dari database
       * 3. Generate access token (15 menit) & refresh token (7 hari)
       * 4. Save refresh token ke database
       * 5. Return tokens ke client
       * 
       * @param request LoginRequest dengan username & password
       * @return AuthResponse dengan access token & refresh token
       */
      @Transactional
      public AuthResponse login(LoginRequest request) {
            try {
                  // STEP 1: Authenticate user (validate username & password)
                  // AuthenticationManager akan:
                  // - Call loadUserByUsername() untuk load user dari DB
                  // - Compare password input dengan password di DB pakai BCrypt
                  // - Throw BadCredentialsException kalau salah
                  authenticationManager.authenticate(
                              new UsernamePasswordAuthenticationToken(
                                          request.getUsername(),
                                          request.getPassword()));

                  // STEP 2: Get user dari database
                  // Kalau sampai sini, berarti username & password BENAR
                  User user = userRepository.findByUsername(request.getUsername())
                              .orElseThrow(() -> new BusinessException("User tidak ditemukan"));

                  // STEP 3: Check apakah user aktif
                  if (Boolean.FALSE.equals(user.getIsActive())) {
                        throw new BusinessException("User tidak aktif");
                  }

                  // STEP 4: Load UserDetails (format Spring Security)
                  // UserDetails berisi username, password, roles
                  UserDetails userDetails = loadUserByUsername(request.getUsername());

                  // STEP 5: Generate JWT tokens
                  // Access Token: 15 menit (untuk akses API)
                  // Refresh Token: 7 hari (untuk generate access token baru)
                  String accessToken = jwtService.generateAccessToken(userDetails);
                  String refreshTokenString = jwtService.generateRefreshToken(userDetails);

                  // STEP 6: Save refresh token ke database
                  // Disimpan supaya bisa di-revoke (logout, security breach, dll)
                  RefreshToken refreshToken = RefreshToken.builder()
                              .token(refreshTokenString)
                              .user(user)
                              .expiryDate(LocalDateTime.now().plusDays(7)) // 7 days
                              .build();
                  refreshTokenRepository.save(refreshToken);

                  // STEP 7: Build response
                  return AuthResponse.builder()
                              .accessToken(accessToken) // Token untuk akses API (header Authorization)
                              .refreshToken(refreshTokenString) // Token untuk generate access token baru
                              .type("Bearer") // Token type untuk header
                              .username(user.getUsername())
                              .roles(user.getRoles().stream()
                                          .map(Role::getRoleName)
                                          .collect(Collectors.toSet()))
                              .build();

            } catch (BadCredentialsException e) {
                  // Exception ini di-throw kalau username/password salah
                  throw new BusinessException("Username atau password salah");
            }
      }

      /**
       * LOAD USER BY USERNAME - Method dari interface UserDetailsService
       * 
       * Method ini dipanggil oleh Spring Security saat:
       * 1. Login - untuk validate password
       * 2. Setiap request - untuk validate JWT token
       * 
       * Convert entity User → UserDetails:
       * - User = entity JPA kita (database)
       * - UserDetails = interface Spring Security (standard format)
       * 
       * @param username Username/email user
       * @return UserDetails dengan info user & roles
       * @throws UsernameNotFoundException kalau user tidak ditemukan
       */
      @Override
      public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            // Load user dari database
            User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan: " + username));

            // Convert entity User → Spring Security User (implements UserDetails)
            // org.springframework.security.core.userdetails.User = class dari Spring
            // Security
            // BUKAN entity User kita!
            return new org.springframework.security.core.userdetails.User(
                        user.getUsername(), // Username untuk authentication
                        user.getPassword(), // Password (BCrypt hash) untuk compare
                        user.getIsActive(), // enabled: apakah user aktif?
                        true, // accountNonExpired: akun tidak expired
                        true, // credentialsNonExpired: password tidak expired
                        true, // accountNonLocked: akun tidak di-lock
                        // authorities: roles user untuk authorization
                        // Map Role entity → GrantedAuthority dengan prefix "ROLE_"
                        // Contoh: "ADMIN" → "ROLE_ADMIN"
                        // Prefix "ROLE_" otomatis ditambah supaya bisa pakai hasRole('ADMIN')
                        user.getRoles().stream()
                                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleName()))
                                    .toList());
      }

      /**
       * REFRESH ACCESS TOKEN - Generate access token baru pakai refresh token
       * 
       * Flow:
       * 1. Validate refresh token (signature, expiration)
       * 2. Check refresh token di database
       * 3. Generate access token baru
       * 4. Return access token baru
       * 
       * Kenapa perlu refresh?
       * - Access token lifetime pendek (15 menit) untuk security
       * - Refresh token lifetime panjang (7 hari) untuk UX
       * - User tidak perlu login ulang setiap 15 menit
       * 
       * @param refreshTokenString Refresh token dari login response
       * @return AuthResponse dengan access token baru
       */
      @Transactional
      public AuthResponse refreshAccessToken(String refreshTokenString) {
            // STEP 1: Validate refresh token (signature & expiration)
            // Extract username dari token
            String username;
            try {
                  username = jwtService.extractUsername(refreshTokenString);
            } catch (Exception e) {
                  throw new BusinessException("Refresh token tidak valid");
            }

            // STEP 2: Load user dari database
            User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new BusinessException("User tidak ditemukan"));

            // STEP 3: Load UserDetails untuk validate token
            UserDetails userDetails = loadUserByUsername(username);

            // STEP 4: Validate token dengan user
            if (!jwtService.isTokenValid(refreshTokenString, userDetails)) {
                  throw new BusinessException("Refresh token tidak valid atau expired");
            }

            // STEP 5: Check refresh token di database
            // Refresh token harus ada di database dan belum expired
            RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                        .orElseThrow(() -> new BusinessException("Refresh token tidak ditemukan"));

            // STEP 6: Check expiration date di database
            if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                  // Hapus refresh token yang expired dari database
                  refreshTokenRepository.delete(refreshToken);
                  throw new BusinessException("Refresh token sudah expired, silakan login ulang");
            }

            // STEP 7: Generate access token baru
            String newAccessToken = jwtService.generateAccessToken(userDetails);

            // STEP 8: Return response dengan access token baru
            // Refresh token tetap sama (tidak perlu generate ulang)
            return AuthResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(refreshTokenString) // Refresh token lama masih valid
                        .type("Bearer")
                        .username(user.getUsername())
                        .roles(user.getRoles().stream()
                                    .map(Role::getRoleName)
                                    .collect(Collectors.toSet()))
                        .build();
      }

      /**
       * LOGOUT USER - Delete refresh token dari database
       * 
       * Flow:
       * 1. Delete refresh token dari database
       * 
       * Note: Tanpa blacklist, token masih bisa dipakai sampai expired.
       * Untuk security lebih baik, sebaiknya pakai token blacklist dengan Redis.
       * 
       * @param refreshTokenString Refresh token dari request body
       */
      @Transactional
      public void logout(String refreshTokenString) {
            try {
                  // Delete refresh token dari database
                  refreshTokenRepository.findByToken(refreshTokenString)
                              .ifPresent(refreshTokenRepository::delete);
            } catch (Exception e) {
                  // Kalau ada error, tetap return success (user sudah logout)
            }
      }
}

# 🔐 Tutorial Implementasi JWT Authentication - Loanova

> **Dokumentasi lengkap implementasi JWT Authentication dengan Spring Boot 4.0.1 & Spring Security 7.0.2**

---

## 📋 Daftar Isi

1. [Pengenalan JWT](#pengenalan-jwt)
2. [Setup Dependencies](#setup-dependencies)
3. [Konfigurasi Application Properties](#konfigurasi-application-properties)
4. [Struktur File & Penjelasan Kode](#struktur-file--penjelasan-kode)
5. [Flow Autentikasi](#flow-autentikasi)
6. [Cara Menggunakan API](#cara-menggunakan-api)
7. [Troubleshooting](#troubleshooting)

---

## 🎯 Pengenalan JWT

### Apa itu JWT?

**JWT (JSON Web Token)** adalah standar terbuka (RFC 7519) untuk mengamankan transfer informasi antara client dan server dalam format JSON. JWT terdiri dari 3 bagian yang dipisahkan dengan titik (.):

```
header.payload.signature
```

**Contoh JWT:**

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZ3VuZyIsImlhdCI6MTcwMzc1MjgwMH0.abc123xyz
```

### Access Token vs Refresh Token

| Aspek          | Access Token            | Refresh Token                       |
| -------------- | ----------------------- | ----------------------------------- |
| **Durasi**     | 15 menit (short-lived)  | 7 hari (long-lived)                 |
| **Fungsi**     | Akses API resources     | Mendapatkan access token baru       |
| **Penggunaan** | Di Authorization header | Disimpan client, kirim saat refresh |
| **Storage**    | Memory/session storage  | Database + secure client storage    |

### Kenapa Menggunakan JWT?

✅ **Stateless** - Server tidak perlu menyimpan session  
✅ **Scalable** - Mudah untuk horizontal scaling  
✅ **Secure** - Token di-sign dengan secret key  
✅ **Cross-domain** - Bekerja di berbagai domain

---

## 🛠️ Setup Dependencies

### 1. Tambahkan JWT Dependencies di `pom.xml`

```xml
<!-- JWT Authentication -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Spring Security (sudah termasuk di starter-security) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### 2. Compile Project

```bash
mvn clean compile
```

---

## ⚙️ Konfigurasi Application Properties

Tambahkan konfigurasi JWT di `src/main/resources/application.properties`:

```properties
# JWT Configuration
jwt.secret=loanova2024SecretKeyForJWTAuthenticationVeryLongAndSecureKey123456
jwt.access-token-expiration=900000
jwt.refresh-token-expiration=604800000

# Server Configuration
server.port=9091

# Database Configuration
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=loanova_db;encrypt=true;trustServerCertificate=true
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

**Penjelasan:**

- `jwt.secret` - Secret key untuk sign JWT (minimal 256 bit / 32 karakter)
- `jwt.access-token-expiration` - Durasi access token dalam ms (900000 = 15 menit)
- `jwt.refresh-token-expiration` - Durasi refresh token dalam ms (604800000 = 7 hari)

---

## 📂 Struktur File & Penjelasan Kode

### Struktur Direktori

```
src/main/java/com/example/loanova/
├── config/
│   ├── AuthenticationConfig.java        # Bean PasswordEncoder & AuthenticationProvider
│   ├── SecurityConfig.java              # Konfigurasi Spring Security
│   └── JwtAuthenticationFilter.java     # Filter untuk validasi JWT
├── controller/
│   └── AuthController.java              # REST endpoint login
├── dto/
│   ├── request/
│   │   └── LoginRequest.java            # DTO login request
│   └── response/
│       └── AuthResponse.java            # DTO authentication response
├── entity/
│   ├── BaseEntity.java                  # Base entity dengan soft delete
│   ├── User.java                        # Entity user
│   ├── Role.java                        # Entity role
│   └── RefreshToken.java                # Entity refresh token
├── repository/
│   ├── UserRepository.java              # Repository user
│   └── RefreshTokenRepository.java      # Repository refresh token
└── service/
    ├── AuthService.java                 # Business logic authentication
    └── JwtService.java                  # Service generate & validate JWT
```

---

### 1️⃣ Entity: RefreshToken.java

**Lokasi**: `src/main/java/com/example/loanova/entity/RefreshToken.java`

```java
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;  // JWT refresh token

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // Relasi ke User

    @Column(nullable = false)
    private LocalDateTime expiryDate;  // Waktu kadaluarsa

    @Column(nullable = false)
    private LocalDateTime createdAt;  // Waktu dibuat

    @Column
    private LocalDateTime revokedAt;  // Waktu di-revoke (untuk logout)

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }

    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }
}
```

**📝 Penjelasan Kode:**

- `@Column(unique = true)` - Token harus unik di database
- `@ManyToOne(fetch = FetchType.LAZY)` - Lazy loading untuk optimasi
- `isValid()` - Check token masih valid (tidak expired & tidak di-revoke)
- `revoke()` - Method untuk logout (set revokedAt)

**💡 Kenapa Simpan di Database?**

- Agar bisa di-revoke saat logout
- Tracking refresh token yang masih aktif
- Security audit trail

---

### 2️⃣ Service: JwtService.java

**Lokasi**: `src/main/java/com/example/loanova/service/JwtService.java`

```java
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;  // Inject dari application.properties

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;  // 15 menit (dalam milliseconds)

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;  // 7 hari (dalam milliseconds)

    /**
     * Generate Access Token (short-lived)
     */
    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, accessTokenExpiration);
    }

    /**
     * Generate Access Token dengan custom claims
     */
    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return generateToken(extraClaims, userDetails, accessTokenExpiration);
    }

    /**
     * Generate Refresh Token (long-lived)
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, refreshTokenExpiration);
    }

    /**
     * Generate Token (generic method)
     */
    private String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts.builder()
                .claims(extraClaims)                      // Custom claims
                .subject(userDetails.getUsername())       // Username sebagai subject
                .issuedAt(new Date(System.currentTimeMillis()))  // Waktu dibuat
                .expiration(new Date(System.currentTimeMillis() + expiration))  // Waktu expired
                .signWith(getSigningKey())                // Sign dengan secret key
                .compact();                               // Build JWT string
    }

    /**
     * Validasi JWT token
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Extract username dari JWT token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Check token sudah expired?
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extract expiration date dari token
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic method extract claim dari token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract semua claims dari token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())  // Verify signature
                .build()
                .parseSignedClaims(token)     // Parse token
                .getPayload();                // Get claims
    }

    /**
     * Get signing key dari secret
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

**📝 Penjelasan Kode:**

1. **`@Value` Annotation**

   - Inject konfigurasi dari `application.properties`
   - `secret` - Secret key string untuk signing JWT
   - `accessTokenExpiration` - Durasi access token dalam milliseconds
   - `refreshTokenExpiration` - Durasi refresh token dalam milliseconds

2. **`generateToken()` Method**

   - Generic method yang bisa generate access token atau refresh token
   - Parameter `extraClaims` untuk custom claims (roles, permissions, dll)
   - Parameter `userDetails` untuk ambil username
   - Parameter `expiration` untuk set durasi token
   - `.subject(userDetails.getUsername())` - Set username sebagai subject JWT
   - `.signWith(getSigningKey())` - Sign token dengan HMAC-SHA256

3. **`getSigningKey()` Method**

   - Convert secret string ke byte array dengan **UTF-8 encoding**
   - **BUKAN** Base64 decode (secret disimpan plain text)
   - Create `SecretKey` untuk HMAC-SHA256 algorithm
   - **PENTING:** Secret harus minimal 256-bit (32 karakter)

4. **`isTokenValid()` Method**
   - Check username match
   - Check token belum expired

**🔐 Security Notes:**

- Secret key harus minimal 256-bit untuk HS256 algorithm
- Token di-sign untuk prevent tampering
- Expiration time mencegah replay attack

---

### 3️⃣ Service: AuthService.java

**Lokasi**: `src/main/java/com/example/loanova/service/AuthService.java`

```java
@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // Constructor dengan @Lazy untuk menghindari circular dependency
    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            @Lazy AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Login user dan generate tokens
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            // 1️⃣ Authenticate dengan Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // 2️⃣ Get user dari database
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new BusinessException("User tidak ditemukan"));

            // 3️⃣ Check user aktif
            if (Boolean.FALSE.equals(user.getIsActive())) {
                throw new BusinessException("User tidak aktif");
            }

            // 4️⃣ Convert User ke UserDetails
            UserDetails userDetails = convertToUserDetails(user);

            // 5️⃣ Generate tokens
            String accessToken = jwtService.generateAccessToken(userDetails);
            String refreshTokenStr = jwtService.generateRefreshToken(userDetails);

            // 6️⃣ Save refresh token ke database
            RefreshToken refreshToken = RefreshToken.builder()
                    .token(refreshTokenStr)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusDays(7))
                    .createdAt(LocalDateTime.now())
                    .build();
            refreshTokenRepository.save(refreshToken);

            // 7️⃣ Build response
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshTokenStr)
                    .type("Bearer")
                    .username(user.getUsername())
                    .roles(user.getRoles().stream()
                            .map(Role::getName)
                            .collect(Collectors.toList()))
                    .build();

        } catch (BadCredentialsException e) {
            throw new BusinessException("Username atau password salah");
        }
    }

    /**
     * Load user by username - digunakan Spring Security
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan: " + username));

        return convertToUserDetails(user);
    }

    /**
     * Convert User entity ke UserDetails
     */
    private UserDetails convertToUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())  // Already BCrypt hashed
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList()))
                .build();
    }
}
```

**📝 Penjelasan Kode:**

1. **`@Lazy` pada AuthenticationManager**

   - Menghindari circular dependency
   - Spring akan create bean saat pertama kali dipanggil, bukan saat startup

2. **Flow Login:**

   - Authenticate → Get User → Check Active → Generate Tokens → Save Refresh Token → Return Response

3. **Password Verification**

   - Password sudah di-hash dengan BCrypt di database
   - Spring Security otomatis compare dengan `authenticationManager.authenticate()`
   - Tidak perlu manual compare password

4. **`@Transactional`**
   - Memastikan semua operasi database success atau rollback
   - Save refresh token akan rollback jika ada error

**🔐 Security:**

- Password di-hash, tidak pernah disimpan plain text
- BadCredentialsException di-catch untuk generic error message
- Check user active untuk prevent disabled user login

---

### 4️⃣ Config: AuthenticationConfig.java

**Lokasi**: `src/main/java/com/example/loanova/config/AuthenticationConfig.java`

```java
@Configuration
@RequiredArgsConstructor
public class AuthenticationConfig {

    private final AuthService authService;

    /**
     * Password Encoder Bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication Provider
     * Menghubungkan UserDetailsService dengan PasswordEncoder
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        // Spring Security 7.0.2 menggunakan constructor injection
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(authService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
}
```

**📝 Penjelasan Kode:**

1. **`PasswordEncoder` Bean**

   - BCrypt hashing algorithm (industry standard)
   - Otomatis salt setiap hash
   - Digunakan untuk hash password saat register & verify saat login

2. **`AuthenticationProvider` Bean**

   - `DaoAuthenticationProvider` - implementation default Spring Security
   - Constructor injection `authService` sebagai UserDetailsService
   - Set password encoder untuk verify credentials

3. **Kenapa Terpisah dari SecurityConfig?**
   - Menghindari circular dependency
   - Separation of concerns
   - Lebih mudah di-test

**⚠️ Catatan Spring Security 7.0.2:**

- Constructor-based injection: `new DaoAuthenticationProvider(authService)`
- Bukan setter: ~~`authProvider.setUserDetailsService(authService)`~~

---

### 5️⃣ Config: SecurityConfig.java

**Lokasi**: `src/main/java/com/example/loanova/config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationProvider authenticationProvider;

    /**
     * AuthenticationManager Bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Security Filter Chain Configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())  // Disable CSRF untuk stateless REST API
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()  // Public endpoints
                        .anyRequest().authenticated()  // Protected endpoints
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // No session
                )
                .authenticationProvider(authenticationProvider)  // Custom auth provider
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);  // JWT filter

        return http.build();
    }
}
```

**📝 Penjelasan Kode:**

1. **`@EnableWebSecurity`**

   - Aktifkan Spring Security
   - Enable security filter chain

2. **`@EnableMethodSecurity`**

   - Enable method-level security
   - Untuk `@PreAuthorize`, `@Secured`, dll

3. **`csrf().disable()`**

   - Disable CSRF protection
   - Aman untuk stateless REST API (tidak ada cookie/session)
   - JWT token di Authorization header tidak vulnerable ke CSRF

4. **`authorizeHttpRequests()`**

   - `/api/auth/**` - Public (login, register, dll)
   - `.anyRequest().authenticated()` - Semua endpoint lain butuh authentication

5. **`SessionCreationPolicy.STATELESS`**

   - Tidak create session di server
   - Setiap request independent
   - Authentication via JWT token

6. **`authenticationProvider()`**

   - Set custom authentication provider
   - Menghubungkan dengan UserDetailsService dan PasswordEncoder

7. **`addFilterBefore()`**
   - Tambah JWT filter sebelum default filter
   - JWT filter run dulu untuk validate token
   - Set authentication context sebelum controller

**Method Injection Parameter:**

- `JwtAuthenticationFilter jwtAuthFilter` - Spring inject otomatis
- Menghindari circular dependency dengan constructor injection

---

### 6️⃣ Filter: JwtAuthenticationFilter.java

**Lokasi**: `src/main/java/com/example/loanova/config/JwtAuthenticationFilter.java`

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1️⃣ Get Authorization header
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 2️⃣ Check if Authorization header exists and starts with Bearer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3️⃣ Extract JWT token (remove "Bearer " prefix)
        jwt = authHeader.substring(7);

        try {
            // 4️⃣ Extract username from JWT
            username = jwtService.extractUsername(jwt);

            // 5️⃣ If username exists and user not authenticated yet
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 6️⃣ Load user details from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 7️⃣ Validate token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // 8️⃣ Create authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // 9️⃣ Set authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Log error (token invalid, expired, dll)
            logger.error("Cannot set user authentication: {}", e);
        }

        // 🔟 Continue filter chain
        filterChain.doFilter(request, response);
    }
}
```

**📝 Penjelasan Kode:**

1. **`OncePerRequestFilter`**

   - Extend base class untuk filter yang run sekali per request
   - Prevent duplicate execution

2. **Extract Token Flow:**

   - Get header → Check format → Remove "Bearer " prefix

3. **Validation Flow:**

   - Extract username → Load user → Validate token → Set authentication

4. **`SecurityContextHolder`**

   - Thread-local storage untuk authentication
   - Set authentication agar bisa diakses di controller via `@AuthenticationPrincipal`

5. **Error Handling:**

   - Try-catch untuk handle invalid token
   - Log error tapi tidak block request
   - Request tetap lanjut ke controller (akan di-reject di SecurityFilterChain)

6. **Check Authentication Null:**
   - `getAuthentication() == null` - Cegah re-authentication
   - Optimasi performance

**🔄 Filter Execution Order:**

```
Request → JwtAuthenticationFilter (validate token)
       → UsernamePasswordAuthenticationFilter (default)
       → Controller (authorized)
```

---

### 7️⃣ Controller: AuthController.java

**Lokasi**: `src/main/java/com/example/loanova/controller/AuthController.java`

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(
                ResponseUtil.success("Login berhasil", response)
        );
    }
}
```

**📝 Penjelasan Kode:**

1. **`@RestController`**

   - Kombinasi `@Controller` + `@ResponseBody`
   - Otomatis serialize response ke JSON

2. **`@RequestMapping("/api/auth")`**

   - Base path untuk semua endpoint di controller ini
   - Login akan jadi `/api/auth/login`

3. **`@Valid`**

   - Trigger validation rules di `LoginRequest`
   - Otomatis return 400 jika validation fail

4. **`@RequestBody`**

   - Binding JSON request body ke object
   - Jackson otomatis deserialize

5. **`ResponseUtil.success()`**
   - Utility method untuk consistent response format
   - Return `ApiResponse<T>` wrapper

**Response Format:**

```json
{
  "success": true,
  "message": "Login berhasil",
  "data": { ... },
  "code": 200,
  "timestamp": "2025-12-28T..."
}
```

---

### 8️⃣ DTOs

#### LoginRequest.java

```java
@Data
public class LoginRequest {

    @NotBlank(message = "Username tidak boleh kosong")
    private String username;

    @NotBlank(message = "Password tidak boleh kosong")
    private String password;
}
```

**Validasi:**

- `@NotBlank` - Field tidak boleh null, empty, atau whitespace
- Custom error messages untuk user-friendly response

#### AuthResponse.java

```java
@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String type;  // "Bearer"
    private String username;
    private List<String> roles;
}
```

**📝 Penjelasan:**

- `@Builder` - Pattern builder untuk construct object
- Consistent structure untuk authentication response

---

## 🔄 Flow Autentikasi

### Flow Lengkap Login sampai Akses Protected Endpoint

```
┌─────────┐
│ Client  │
└────┬────┘
     │
     │ 1. POST /api/auth/login
     │    {username: "agung", password: "Password123!"}
     ▼
┌────────────────────────────────────────┐
│  AuthController.login()                │
│  - Receive & validate request          │
└────┬───────────────────────────────────┘
     │
     │ 2. authService.login(request)
     ▼
┌────────────────────────────────────────┐
│  AuthService.login()                   │
│  ┌──────────────────────────────────┐ │
│  │ 2a. authenticationManager        │ │
│  │     .authenticate()              │ │
│  │     - BCrypt compare password    │ │
│  └──────────────────────────────────┘ │
│  ┌──────────────────────────────────┐ │
│  │ 2b. Get user from DB             │ │
│  │ 2c. Check user.isActive          │ │
│  └──────────────────────────────────┘ │
│  ┌──────────────────────────────────┐ │
│  │ 2d. jwtService.generateTokens()  │ │
│  │     - Generate access token      │ │
│  │     - Generate refresh token     │ │
│  └──────────────────────────────────┘ │
│  ┌──────────────────────────────────┐ │
│  │ 2e. Save refresh token to DB     │ │
│  └──────────────────────────────────┘ │
└────┬───────────────────────────────────┘
     │
     │ 3. Return tokens
     ▼
┌─────────────────────────────────────────┐
│ Client receives response                │
│ {                                       │
│   "accessToken": "eyJhbGc...",          │
│   "refreshToken": "eyJhbGc...",         │
│   "username": "agung",                  │
│   "roles": ["ADMIN"]                    │
│ }                                       │
│                                         │
│ → Store tokens in memory/storage        │
└────┬────────────────────────────────────┘
     │
     │ 4. GET /api/users
     │    Authorization: Bearer <accessToken>
     ▼
┌──────────────────────────────────────────┐
│  JwtAuthenticationFilter                 │
│  ┌────────────────────────────────────┐ │
│  │ 4a. Extract token from header      │ │
│  │     "Bearer eyJhbGc..."            │ │
│  └────────────────────────────────────┘ │
│  ┌────────────────────────────────────┐ │
│  │ 4b. jwtService.extractUsername()   │ │
│  │     → "agung"                      │ │
│  └────────────────────────────────────┘ │
│  ┌────────────────────────────────────┐ │
│  │ 4c. userDetailsService             │ │
│  │     .loadUserByUsername("agung")   │ │
│  │     → Get user from DB             │ │
│  └────────────────────────────────────┘ │
│  ┌────────────────────────────────────┐ │
│  │ 4d. jwtService.isTokenValid()      │ │
│  │     - Check expiration             │ │
│  │     - Verify signature             │ │
│  │     → true                         │ │
│  └────────────────────────────────────┘ │
│  ┌────────────────────────────────────┐ │
│  │ 4e. Set authentication in          │ │
│  │     SecurityContext                │ │
│  └────────────────────────────────────┘ │
└────┬─────────────────────────────────────┘
     │
     │ 5. Continue to controller
     ▼
┌──────────────────────────────────────────┐
│  UserController.getAllUsers()            │
│  - User authenticated ✅                  │
│  - Process business logic                │
│  - Return response                       │
└────┬─────────────────────────────────────┘
     │
     │ 6. Response
     ▼
┌─────────────────────────────────────────┐
│ Client receives data                    │
│ {                                       │
│   "success": true,                      │
│   "data": [ ... ],                      │
│   "message": "Data berhasil diambil"    │
│ }                                       │
└─────────────────────────────────────────┘
```

---

## 📖 Cara Menggunakan API

### Step 1: Login untuk Mendapatkan Token

**Request:**

```bash
curl -X POST http://localhost:9091/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "agung",
    "password": "Password123!"
  }'
```

**Response:**

```json
{
  "success": true,
  "message": "Login berhasil",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZ3VuZyIsImlhdCI6MTcwMzc2NjQwMCwiZXhwIjoxNzAzNzY3MzAwfQ.Xjt3v9KdLQ_a1b2c3d4e5f",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZ3VuZyIsImlhdCI6MTcwMzc2NjQwMCwiZXhwIjoxNzA0MzcxMjAwfQ.Abc123xyz",
    "type": "Bearer",
    "username": "agung",
    "roles": ["ADMIN", "USER"]
  },
  "code": 200,
  "timestamp": "2025-12-28T17:30:00"
}
```

**📝 Simpan accessToken:**

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZ3VuZyIsImlhdCI6MTcwMzc2NjQwMCwiZXhwIjoxNzAzNzY3MzAwfQ.Xjt3v9KdLQ_a1b2c3d4e5f
```

---

### Step 2: Gunakan Access Token untuk Akses Protected Endpoint

**❌ Tanpa Token (403 Forbidden):**

```bash
curl -X GET http://localhost:9091/api/users
```

**✅ Dengan Access Token (200 OK):**

```bash
curl -X GET http://localhost:9091/api/users \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZ3VuZyIsImlhdCI6MTcwMzc2NjQwMCwiZXhwIjoxNzAzNzY3MzAwfQ.Xjt3v9KdLQ_a1b2c3d4e5f"
```

**Response:**

```json
{
  "success": true,
  "message": "Data berhasil diambil",
  "data": [
    {
      "id": 1,
      "username": "agung",
      "email": "agung@example.com",
      "isActive": true,
      "roles": ["ADMIN"]
    }
  ],
  "code": 200
}
```

---

### Step 3: Testing di Postman

#### A. Create Collection "Loanova API"

#### B. Login Request

1. **New Request** → Name: "Login"
2. **Method**: POST
3. **URL**: `http://localhost:9091/api/auth/login`
4. **Headers**:
   ```
   Content-Type: application/json
   ```
5. **Body** → raw → JSON:
   ```json
   {
     "username": "agung",
     "password": "Password123!"
   }
   ```
6. **Send** → Copy `accessToken` dari response

#### C. Get Users Request

1. **New Request** → Name: "Get All Users"
2. **Method**: GET
3. **URL**: `http://localhost:9091/api/users`
4. **Authorization** tab:
   - Type: Bearer Token
   - Token: `<paste_accessToken_disini>`
5. **Send** → Berhasil dapat data users

---

### Step 4: Handle Token Expiration

**Access Token Expired (setelah 15 menit):**

```json
{
  "timestamp": "2025-12-28T17:45:00",
  "status": 403,
  "error": "Forbidden",
  "message": "JWT token is expired",
  "path": "/api/users"
}
```

**Solusi Saat Ini:**

- Login ulang untuk mendapat token baru

**Solusi Future (Refresh Token Endpoint):**

```bash
curl -X POST http://localhost:9091/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<refresh_token_dari_login>"
  }'
```

---

## 🔧 Troubleshooting

## 🔧 Troubleshooting

### 1. Circular Dependency Error

**Error:**

```
The dependencies of some of the beans in the application context form a cycle
```

**Penyebab:**

- Bean saling bergantung satu sama lain
- Spring tidak bisa menentukan urutan pembuatan bean

**Solusi yang Sudah Diimplementasikan:**

1. **`@Lazy` pada AuthenticationManager** di AuthService
2. **Method injection** JwtAuthenticationFilter di SecurityConfig
3. **Pisah konfigurasi** AuthenticationConfig dan SecurityConfig

**Kode Fix:**

```java
// AuthService.java
public AuthService(
    UserRepository userRepository,
    RefreshTokenRepository refreshTokenRepository,
    JwtService jwtService,
    @Lazy AuthenticationManager authenticationManager  // ← @Lazy annotation
) {
    // ...
}

// SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(
    HttpSecurity http,
    JwtAuthenticationFilter jwtAuthFilter  // ← Method injection, bukan constructor
) throws Exception {
    // ...
}
```

---

### 2. "Invalid JWT token" Error

**Penyebab:**

- Token format salah (tidak ada "Bearer " prefix)
- Token signature invalid
- Secret key tidak match dengan saat generate

**Solusi:**

- Pastikan format header: `Authorization: Bearer <token>`
- Check `jwt.secret` di application.properties sama dengan saat generate
- Generate token baru dengan login ulang

---

### 3. "JWT token is expired" Error

**Penyebab:**

- Access token sudah lewat 15 menit
- System time tidak sync

**Solusi:**

- Login ulang untuk mendapat token baru
- Implement refresh token endpoint (next phase)
- Perbesar `jwt.access-token-expiration` untuk development

---

### 4. "Access Denied" / 403 Forbidden

**Penyebab:**

- Tidak ada Authorization header
- Token tidak dikirim
- Token format salah

**Solusi:**

```bash
# ❌ SALAH - Tanpa header
curl http://localhost:9091/api/users

# ❌ SALAH - Format salah
curl -H "Authorization: eyJhbGc..." http://localhost:9091/api/users

# ✅ BENAR - Dengan Bearer prefix
curl -H "Authorization: Bearer eyJhbGc..." http://localhost:9091/api/users
```

---

### 5. "Username atau password salah"

**Penyebab:**

- Credentials tidak match di database
- Password tidak di-hash dengan BCrypt
- User belum ada di database

**Solusi:**

```sql
-- Check user di database
SELECT * FROM users WHERE username = 'agung';

-- Check password hash
-- Password harus dimulai dengan $2a$ atau $2b$ (BCrypt)
SELECT password FROM users WHERE username = 'agung';
-- Should be: $2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

---

### 6. Application Tidak Bisa Start

**Check:**

1. Port 9091 sudah digunakan aplikasi lain?
2. Database connection berhasil?
3. Ada compilation error?

**Solusi:**

```bash
# Check port
netstat -ano | findstr :9091

# Test database connection
sqlcmd -S localhost -d loanova_db -U sa -P your_password

# Compile ulang
mvn clean compile
```

---

### 7. Database Connection Error

**Error:**

```
Unable to open JDBC Connection
```

**Check:**

1. SQL Server running?
2. Database `loanova_db` exists?
3. Username & password correct?

**Solusi:**

```properties
# application.properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=loanova_db;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=YourPassword
```

---

## ✅ Best Practices

### Security

- ✅ Gunakan HTTPS di production
- ✅ Simpan JWT secret di environment variable
- ✅ Implementasi rate limiting untuk login endpoint
- ✅ Log semua authentication attempts
- ✅ Short-lived access token (5-15 menit)
- ✅ Revoke refresh token saat logout/change password

### Code Quality

- ✅ Separation of concerns (Config, Service, Controller terpisah)
- ✅ Use DTOs untuk request/response
- ✅ Consistent error handling
- ✅ Proper validation dengan Bean Validation
- ✅ Transaction management dengan `@Transactional`

### Testing

- ✅ Unit test untuk service layer
- ✅ Integration test untuk API endpoints
- ✅ Security testing dengan berbagai skenario
- ✅ Load testing untuk performance

---

## 🚀 Next Steps

### Fitur yang Bisa Ditambahkan:

#### 1. Refresh Token Endpoint

```java
@PostMapping("/refresh")
public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody RefreshTokenRequest request) {
    AuthResponse response = authService.refreshAccessToken(request.getRefreshToken());
    return ResponseEntity.ok(ResponseUtil.success("Token refreshed", response));
}
```

#### 2. Logout Endpoint

```java
@PostMapping("/logout")
public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserDetails userDetails) {
    authService.logout(userDetails.getUsername());
    return ResponseEntity.ok(ResponseUtil.success("Logout berhasil", null));
}
```

#### 3. Role-Based Access Control

```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/users/{id}")
public ResponseEntity<?> deleteUser(@PathVariable Long id) {
    // Only ADMIN can delete users
}
```

#### 4. Two-Factor Authentication

- Send OTP via email/SMS
- Verify OTP before generate tokens

#### 5. OAuth2 Integration

- Login dengan Google, Facebook, GitHub
- Social login flow

---

## 📚 Kesimpulan

### Apa yang Sudah Diimplementasikan?

✅ **Authentication & Authorization**

- Login dengan username & password
- JWT access token & refresh token generation
- Password encryption dengan BCrypt
- Token validation di setiap request
- Stateless authentication

✅ **Security**

- Spring Security 7.0.2 configuration
- CSRF protection disabled untuk REST API
- Stateless session management
- Token-based authentication

✅ **Code Quality**

- Clean architecture (separation of concerns)
- DTOs untuk request/response
- Proper exception handling
- Bean validation
- Transaction management

✅ **Troubleshooting**

- Circular dependency resolved dengan @Lazy
- Comprehensive error messages
- Logging untuk debugging

### Siap untuk Production?

**Sudah Ada:**

- ✅ Secure password hashing
- ✅ Token expiration
- ✅ Proper error handling
- ✅ Validation
- ✅ Soft delete (audit trail)

**Perlu Ditambahkan:**

- ⚠️ HTTPS/TLS
- ⚠️ Rate limiting
- ⚠️ Refresh token endpoint
- ⚠️ Logout endpoint
- ⚠️ Environment variables untuk secrets
- ⚠️ Comprehensive testing
- ⚠️ Monitoring & logging
- ⚠️ API documentation (Swagger/OpenAPI)

---

## 📞 Support

**Dokumentasi ini mencakup:**

- ✅ Pengenalan JWT
- ✅ Setup dependencies
- ✅ Konfigurasi lengkap
- ✅ Penjelasan kode detail per file
- ✅ Flow autentikasi lengkap
- ✅ Cara menggunakan API
- ✅ Troubleshooting common issues
- ✅ Best practices
- ✅ Next steps

**Tech Stack:**

- Spring Boot 4.0.1
- Spring Security 7.0.2
- JWT (jjwt) 0.12.6
- SQL Server
- Java 21

---

**Dibuat oleh**: GitHub Copilot  
**Tanggal**: 28 Desember 2025  
**Project**: Loanova - Loan Innovation Platform  
**Version**: 1.0.0

# 📋 CACHE IMPLEMENTATION GUIDE - User Cache with Redis

## 🎯 Tujuan

Implementasi Redis cache untuk endpoint `GET /api/users` supaya:

- ✅ Response lebih cepat (5-20ms vs 100-500ms)
- ✅ Reduce database load
- ✅ Auto-refresh saat data berubah (create/update/delete)

---

## 📦 Dependencies Required

### 1. pom.xml

Tambahkan dependencies:

```xml
<!-- Redis Cache -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

---

## ⚙️ Configuration Files

### 2. application.properties

Tambahkan Redis configuration:

```properties
# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=60000
spring.cache.type=redis
spring.cache.redis.time-to-live=3600000
```

### 3. docker-compose.yml

Setup Redis dengan RedisInsight (GUI):

```yaml
services:
  redis:
    image: redis:latest
    container_name: redis_cache
    ports:
      - "6379:6379"
    command: redis-server --loglevel warning
    volumes:
      - redis_data:/data
    networks:
      - redis_network

  redisinsight:
    image: redis/redisinsight:latest
    container_name: redis_insight
    ports:
      - "5540:5540"
    depends_on:
      - redis
    volumes:
      - redisinsight_data:/data
    networks:
      - redis_network

volumes:
  redis_data:
  redisinsight_data:

networks:
  redis_network:
    driver: bridge
```

---

## 🔧 Code Changes

### 4. LoanovaApplication.java

Enable caching di main application:

```java
@SpringBootApplication
@EnableCaching  // ← TAMBAHKAN INI
public class LoanovaApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoanovaApplication.class, args);
    }
}
```

### 5. CacheConfig.java (NEW FILE)

Create cache configuration:

```java
@Configuration
public class CacheConfig {

    /**
     * Customize cache configuration untuk userCache
     * TTL: 5 menit (data user sering berubah)
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(){
        return builder -> builder
                .withCacheConfiguration("userCache",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(5)));
    }

    /**
     * Default cache configuration untuk semua cache
     * - TTL: 60 menit
     * - Tidak cache nilai null
     * - Serialize dengan JSON
     */
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(RedisSerializer.json()));
    }
}
```

### 6. UserResponse.java (DTO)

Tambahkan Serializable untuk Redis:

```java
@Data
@Builder
@NoArgsConstructor  // ← TAMBAHKAN
@AllArgsConstructor  // ← TAMBAHKAN
public class UserResponse implements Serializable {  // ← TAMBAHKAN implements Serializable
    private static final long serialVersionUID = 1L;  // ← TAMBAHKAN

    private Long id;
    private String username;
    private String email;
    private String branchCode;
    private Boolean isActive;
    private Set<String> roles;
}
```

### 7. UserService.java

Tambahkan cache annotations:

```java
@Service
public class UserService {

    /**
     * GET ALL USERS - Cached
     *
     * Request pertama: Hit database, save to cache
     * Request kedua dst: Return from cache (NO database query)
     * Cache TTL: 5 menit
     */
    @Cacheable(value = "userCache")  // ← TAMBAHKAN
    public List<UserResponse> getAllUser() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * CREATE USER - Clear cache
     *
     * Setelah create user baru, cache di-clear supaya
     * next GET request dapat data yang include user baru
     */
    @CacheEvict(value = "userCache", allEntries = true)  // ← TAMBAHKAN
    public UserResponse createUser(UserRequest request) {
        // ... existing code
    }

    /**
     * UPDATE USER - Clear cache
     *
     * Setelah update user, cache di-clear supaya
     * next GET request dapat data yang sudah diupdate
     */
    @CacheEvict(value = "userCache", allEntries = true)  // ← TAMBAHKAN
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        // ... existing code
    }

    /**
     * DELETE USER - Clear cache
     *
     * Setelah delete user, cache di-clear supaya
     * next GET request tidak include user yang sudah dihapus
     */
    @CacheEvict(value = "userCache", allEntries = true)  // ← TAMBAHKAN
    public void deleteUser(Long id) {
        // ... existing code
    }
}
```

---

## 📝 LIST FILE YANG DIUPDATE

### Files Modified:

1. ✅ `pom.xml` - Added Redis dependencies
2. ✅ `src/main/resources/application.properties` - Added Redis config
3. ✅ `src/main/java/com/example/loanova/LoanovaApplication.java` - Added @EnableCaching
4. ✅ `src/main/java/com/example/loanova/dto/response/UserResponse.java` - Added Serializable
5. ✅ `src/main/java/com/example/loanova/service/UserService.java` - Added cache annotations

### Files Created:

6. ✅ `docker-compose.yml` - Redis & RedisInsight setup
7. ✅ `src/main/java/com/example/loanova/config/CacheConfig.java` - Cache configuration

---

## 🚀 How to Run

### 1. Start Redis

```powershell
docker-compose up -d
```

### 2. Verify Redis Running

```powershell
docker exec -it redis_cache redis-cli ping
# Expected: PONG
```

### 3. Start Spring Boot Application

```powershell
mvn spring-boot:run
```

### 4. Access RedisInsight (Optional - GUI)

Open browser: http://localhost:5540

---

## 🧪 Testing Cache

### Test 1: Cold Cache (First Request)

```http
GET http://localhost:9091/api/users
Authorization: Bearer <your-token>
```

**Expected:**

- Console log: `Hibernate: select ... from users` (SQL query)
- Response: 200 OK
- Time: ~200-500ms

### Test 2: Warm Cache (Second Request)

```http
GET http://localhost:9091/api/users
Authorization: Bearer <your-token>
```

**Expected:**

- Console log: NO SQL query (data from cache)
- Response: 200 OK (same data)
- Time: ~5-20ms ⚡

### Test 3: Cache Eviction (Create User)

```http
POST http://localhost:9091/api/users
Authorization: Bearer <your-token>
Body: { "username": "newuser", ... }
```

**Expected:**

- User created successfully
- Cache cleared automatically

### Test 4: Fresh Data (After Eviction)

```http
GET http://localhost:9091/api/users
Authorization: Bearer <your-token>
```

**Expected:**

- Console log: SQL query (cache was cleared)
- Response: 200 OK with NEW user included
- Cache repopulated

---

## 🔍 Verify Cache in Redis

### Using Redis CLI:

```powershell
docker exec -it redis_cache redis-cli

# List all keys
127.0.0.1:6379> KEYS *
1) "userCache::"

# Check TTL (time to live)
127.0.0.1:6379> TTL "userCache::"
(integer) 285  # 5 menit = 300 detik

# View cache data (optional)
127.0.0.1:6379> HGETALL "userCache::"
```

### Using RedisInsight GUI:

1. Open http://localhost:5540
2. Add database: `redis` (host), port `6379`
3. Browse keys → See `userCache::`
4. View JSON data, TTL, memory usage

---

## 📊 Cache Flow Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    GET /api/users                       │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
                ┌───────────────────────┐
                │  @Cacheable Check     │
                │  Key: "userCache::"   │
                └───────────────────────┘
                            │
                ┌───────────┴───────────┐
                ▼                       ▼
        ┌───────────┐           ┌──────────┐
        │ Cache HIT │           │Cache MISS│
        └───────────┘           └──────────┘
                │                       │
                │                       ▼
                │               ┌──────────────┐
                │               │ Query DB     │
                │               └──────────────┘
                │                       │
                │                       ▼
                │               ┌──────────────┐
                │               │ Save to Cache│
                │               │ TTL: 5 min   │
                │               └──────────────┘
                │                       │
                └───────────┬───────────┘
                            ▼
                    ┌───────────────┐
                    │ Return Data   │
                    └───────────────┘

┌─────────────────────────────────────────────────────────┐
│         POST/PUT/DELETE /api/users                      │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
                ┌───────────────────────┐
                │  Execute Method       │
                │  (DB Operation)       │
                └───────────────────────┘
                            │
                            ▼
                ┌───────────────────────┐
                │  @CacheEvict          │
                │  Clear "userCache::"  │
                └───────────────────────┘
                            │
                            ▼
                ┌───────────────────────┐
                │  Return Result        │
                └───────────────────────┘
```

---

## 🎯 Benefits

### Performance Improvement:

- **Cold Cache:** 200-500ms (database query)
- **Warm Cache:** 5-20ms (Redis memory)
- **Improvement:** 10-100x faster! ⚡

### Database Load Reduction:

- **Without Cache:** 100 requests = 100 database queries
- **With Cache:** 100 requests = 1 database query (first) + 99 cache hits
- **Reduction:** 99% less database load! 🎯

### Auto Cache Invalidation:

- Create user → Cache cleared → Next GET = fresh data ✅
- Update user → Cache cleared → Next GET = updated data ✅
- Delete user → Cache cleared → Next GET = without deleted user ✅

---

## ⚠️ Important Notes

### 1. Cache HANYA untuk UserResponse (DTO)

- ✅ Yang di-cache: `List<UserResponse>` (DTO yang sudah di-transform)
- ❌ BUKAN: User entity (JPA entity)

### 2. Cache Key

- Key: `userCache::`
- Semua request GET users pakai key yang sama
- Clear cache = hapus semua users cache

### 3. TTL (Time To Live)

- Default: 5 menit
- Setelah 5 menit, cache auto-expire
- Next request akan query database lagi

### 4. Cache Eviction Strategy

- `allEntries = true` → Hapus semua cache di "userCache"
- Alternative: per-ID caching (lebih complex, tapi lebih granular)

---

## 🔧 Troubleshooting

### Redis Connection Error

```
Could not get resource from pool
```

**Solution:**

```powershell
# Check Redis running
docker ps | Select-String redis

# Start Redis if stopped
docker-compose up -d
```

### Cache Not Working

```
Still seeing SQL queries every request
```

**Solution:**

1. Check `@EnableCaching` di LoanovaApplication.java
2. Check Redis running: `docker exec -it redis_cache redis-cli ping`
3. Restart application: `mvn spring-boot:run`
4. Check console log for Redis connection

### Data Not Fresh After Update

```
GET still returns old data after update
```

**Solution:**

1. Check `@CacheEvict` di method update/create/delete
2. Verify cache cleared: `docker exec -it redis_cache redis-cli KEYS *`
3. Manual clear: `docker exec -it redis_cache redis-cli FLUSHALL`

---

## 📚 References

- Spring Cache Documentation: https://spring.io/guides/gs/caching/
- Redis Documentation: https://redis.io/documentation
- Spring Data Redis: https://spring.io/projects/spring-data-redis

---

## ✅ Checklist Implementation

- [x] Install Redis dependencies (pom.xml)
- [x] Configure Redis connection (application.properties)
- [x] Setup Redis Docker container (docker-compose.yml)
- [x] Enable caching (@EnableCaching)
- [x] Create cache configuration (CacheConfig.java)
- [x] Make DTO Serializable (UserResponse.java)
- [x] Add @Cacheable to GET method (UserService.java)
- [x] Add @CacheEvict to WRITE methods (UserService.java)
- [x] Test cache working (GET request 2x)
- [x] Verify cache invalidation (CREATE → GET)
- [x] Check Redis keys & TTL

---

**Implementation Complete! 🎉**

Cache sudah berjalan untuk endpoint `GET /api/users` dengan:

- ✅ Fast response (5-20ms from cache)
- ✅ Auto-refresh on data changes
- ✅ TTL 5 menit
- ✅ Redis monitoring via RedisInsight

# Database Migration Guide: H2 to PostgreSQL

## Overview

This guide provides step-by-step instructions for migrating the MusicChat application from H2 database (default for local development) to PostgreSQL (recommended for production).

The application is designed with database abstraction in mind, using JPA/Hibernate exclusively with no vendor-specific SQL, making database migration straightforward.

---

## Prerequisites

### H2 (Default - Development)
✅ Already configured  
✅ No installation required  
✅ File-based: `./data/musicchat.mv.db`

### PostgreSQL (Production)
- PostgreSQL 12+ installed and running
- Database created
- User with appropriate permissions

---

## Migration Options

### Option 1: Switch to PostgreSQL (No Data Migration)
**Use Case**: Fresh start with PostgreSQL, no need to preserve H2 data

### Option 2: Migrate Existing Data from H2 to PostgreSQL
**Use Case**: Preserve existing users, messages, tracks, playlists, etc.

---

## Option 1: Fresh PostgreSQL Setup (Recommended)

### Step 1: Install PostgreSQL

**Windows**:
```bash
# Download from https://www.postgresql.org/download/windows/
# Or use chocolatey
choco install postgresql
```

**macOS**:
```bash
brew install postgresql@15
brew services start postgresql@15
```

**Linux (Ubuntu/Debian)**:
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

### Step 2: Create Database and User

```bash
# Connect to PostgreSQL
sudo -u postgres psql

# Create database
CREATE DATABASE musicchat;

# Create user (optional, can use default postgres user)
CREATE USER musicchat_user WITH PASSWORD 'your_secure_password';

# Grant privileges
GRANT ALL PRIVILEGES ON DATABASE musicchat TO musicchat_user;

# Exit
\q
```

### Step 3: Update Application Configuration

The PostgreSQL configuration already exists in `src/main/resources/application-postgres.properties`:

```properties
# PostgreSQL Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/musicchat
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.username=postgres
spring.datasource.password=postgres

# JPA/Hibernate Configuration for PostgreSQL
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

**Update the credentials**:
```properties
spring.datasource.username=musicchat_user
spring.datasource.password=your_secure_password
```

### Step 4: Run Application with PostgreSQL

**Using Maven**:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

**Using JAR**:
```bash
java -jar target/musicchat-0.0.1-SNAPSHOT.jar --spring.profiles.active=postgres
```

**Using Environment Variable**:
```bash
export SPRING_PROFILES_ACTIVE=postgres
mvn spring-boot:run
```

### Step 5: Verify Database Schema

The application will automatically create all tables on first startup thanks to `spring.jpa.hibernate.ddl-auto=update`.

**Connect to PostgreSQL and verify**:
```bash
psql -U musicchat_user -d musicchat

# List tables
\dt

# Expected tables:
# - users
# - chat_messages
# - audio_tracks
# - playlists
# - playlist_tracks
# - ratings
# - favourites
```

**Expected Schema**:
```sql
-- Users table
users (id, username, email, password, role, created_at)

-- Chat messages
chat_messages (id, sender, text, created_at, previous_id)

-- Audio tracks
audio_tracks (id, title, artist, filename, original_filename, uploaded_at, 
              duration_seconds, file_size_bytes, content_type, user_id)

-- Playlists
playlists (id, owner_id, name, created_at, description)

-- Playlist tracks (join table)
playlist_tracks (id, playlist_id, track_id, position, added_at)

-- Ratings
ratings (id, user_id, track_id, rating_value, created_at, updated_at)

-- Favourites
favourites (id, user_id, track_id, created_at)
```

### Step 6: Test Application

1. **Access the application**: http://localhost:8080
2. **Create a user**
3. **Upload a track**
4. **Send a chat message**
5. **Create a playlist**
6. **Verify data in PostgreSQL**:

```sql
SELECT * FROM users;
SELECT * FROM chat_messages ORDER BY created_at;
SELECT * FROM audio_tracks;
SELECT * FROM playlists;
```

---

## Option 2: Migrate Data from H2 to PostgreSQL

### Step 1: Export Data from H2

**Method A: Using H2 Console**

1. Start application with H2: `mvn spring-boot:run`
2. Access H2 Console: http://localhost:8080/h2-console
   - JDBC URL: `jdbc:h2:file:./data/musicchat`
   - Username: `sa`
   - Password: (leave empty)

3. Export each table:
```sql
-- Export users
SCRIPT TO 'users.sql' TABLE users;

-- Export chat_messages
SCRIPT TO 'chat_messages.sql' TABLE chat_messages;

-- Export audio_tracks
SCRIPT TO 'audio_tracks.sql' TABLE audio_tracks;

-- Export playlists
SCRIPT TO 'playlists.sql' TABLE playlists;

-- Export playlist_tracks
SCRIPT TO 'playlist_tracks.sql' TABLE playlist_tracks;

-- Export ratings
SCRIPT TO 'ratings.sql' TABLE ratings;

-- Export favourites
SCRIPT TO 'favourites.sql' TABLE favourites;
```

**Method B: Using Application Code**

Create a migration utility that reads from H2 and writes to PostgreSQL:

```java
// MigrationUtility.java (example)
@Service
public class DataMigrationService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private AudioTrackRepository audioTrackRepository;
    
    // ... other repositories
    
    public void exportToJson() {
        List<User> users = userRepository.findAll();
        List<ChatMessage> messages = chatMessageRepository.findAll();
        List<AudioTrack> tracks = audioTrackRepository.findAll();
        
        // Write to JSON files
        // Use Jackson ObjectMapper
    }
}
```

### Step 2: Set Up PostgreSQL

Follow Steps 1-2 from Option 1.

### Step 3: Import Data to PostgreSQL

1. Start application with PostgreSQL profile to create schema:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

2. Stop the application

3. Connect to PostgreSQL:
```bash
psql -U musicchat_user -d musicchat
```

4. Import data (after converting H2 SQL to PostgreSQL syntax):
```sql
-- Import users
\i users_postgres.sql

-- Import chat_messages
\i chat_messages_postgres.sql

-- Continue for other tables...
```

**Note**: You may need to adjust the SQL syntax differences between H2 and PostgreSQL:
- H2 uses `AUTO_INCREMENT`, PostgreSQL uses `SERIAL` or `IDENTITY`
- Sequence handling differs
- Data type differences (e.g., TIMESTAMP handling)

### Step 4: Verify Data Migration

```sql
-- Check record counts
SELECT 'users' AS table_name, COUNT(*) AS count FROM users
UNION ALL
SELECT 'chat_messages', COUNT(*) FROM chat_messages
UNION ALL
SELECT 'audio_tracks', COUNT(*) FROM audio_tracks
UNION ALL
SELECT 'playlists', COUNT(*) FROM playlists
UNION ALL
SELECT 'ratings', COUNT(*) FROM ratings
UNION ALL
SELECT 'favourites', COUNT(*) FROM favourites;
```

---

## Database Abstraction Verification

The application maintains database abstraction through:

### 1. JPA Repositories Only
```java
// All database access goes through JPA repositories
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

### 2. No Native SQL Queries
```java
// ✅ GOOD: Using JPA methods
List<AudioTrack> tracks = audioTrackRepository.findByTitleContainingIgnoreCase(query);

// ❌ BAD: Native SQL (we don't do this)
@Query(value = "SELECT * FROM audio_tracks WHERE title LIKE ?", nativeQuery = true)
```

### 3. Standard JPA Annotations
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
}
```

### 4. Hibernate Dialects Handle Database-Specific Syntax
```properties
# H2
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# PostgreSQL
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

---

## Production Deployment Checklist

### Security
- [ ] Change default PostgreSQL password
- [ ] Use environment variables for credentials
- [ ] Enable SSL/TLS for database connections
- [ ] Restrict database network access (firewall rules)

### Performance
- [ ] Set appropriate connection pool size
- [ ] Configure `spring.jpa.hibernate.ddl-auto=validate` (not `update`)
- [ ] Create database indexes for frequently queried columns
- [ ] Enable query caching if needed

### Backup
- [ ] Set up automated PostgreSQL backups
- [ ] Test backup restoration process
- [ ] Configure point-in-time recovery if needed

### Monitoring
- [ ] Enable PostgreSQL query logging
- [ ] Monitor slow queries
- [ ] Set up database connection monitoring

---

## Environment-Specific Configuration

### Development (H2)
```properties
# application.properties (default)
spring.datasource.url=jdbc:h2:file:./data/musicchat
spring.jpa.hibernate.ddl-auto=update
```

### Staging (PostgreSQL)
```properties
# application-staging.properties
spring.datasource.url=jdbc:postgresql://staging-db:5432/musicchat
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
```

### Production (PostgreSQL)
```properties
# application-prod.properties
spring.datasource.url=jdbc:postgresql://prod-db:5432/musicchat
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Connection pooling
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
```

### Using Environment Variables
```bash
# Set environment variables
export DB_USERNAME=musicchat_user
export DB_PASSWORD=secure_password
export SPRING_PROFILES_ACTIVE=postgres

# Run application
java -jar musicchat.jar
```

---

## Schema Migration with Flyway (Optional)

For production environments, consider using Flyway for versioned schema migrations:

### Step 1: Add Dependency
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

### Step 2: Create Migration Scripts
```
src/main/resources/db/migration/
├── V1__Initial_schema.sql
├── V2__Add_favourites_table.sql
└── V3__Add_ratings_table.sql
```

### Step 3: Configure Flyway
```properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.jpa.hibernate.ddl-auto=validate
```

---

## Rollback Procedure

If migration fails or issues are discovered:

### 1. Stop the Application
```bash
# Kill the running process
Ctrl+C
```

### 2. Restore H2 Profile
```bash
# Remove postgres profile
mvn spring-boot:run
```

### 3. Restore H2 Data (if needed)
```bash
# H2 data is in ./data/musicchat.mv.db
# If you have a backup, restore it
cp ./data/musicchat.mv.db.backup ./data/musicchat.mv.db
```

---

## Testing Database Compatibility

Run the PostgreSQL compatibility test suite:

```bash
# Requires Docker for Testcontainers
mvn test -Dtest="PostgreSQLCompatibilityTest"
```

This test:
- Spins up a PostgreSQL 15 container
- Creates all entities
- Tests CRUD operations
- Verifies queries work identically
- Tests aggregation functions (AVG, COUNT)
- Confirms date/time handling

**9/9 tests should pass**, proving complete PostgreSQL compatibility.

---

## Troubleshooting

### Issue: Connection Refused
**Solution**: Ensure PostgreSQL is running
```bash
# Check PostgreSQL status
sudo systemctl status postgresql

# Start if not running
sudo systemctl start postgresql
```

### Issue: Authentication Failed
**Solution**: Check credentials in `application-postgres.properties`
```bash
# Test connection
psql -U musicchat_user -d musicchat
```

### Issue: Tables Not Created
**Solution**: Check `ddl-auto` setting
```properties
# Should be 'update' for first run
spring.jpa.hibernate.ddl-auto=update
```

### Issue: Sequence/ID Generation Errors
**Solution**: PostgreSQL uses sequences differently than H2
```java
// Ensure entities use IDENTITY strategy
@GeneratedValue(strategy = GenerationType.IDENTITY)
```

---

## Performance Comparison

| Metric | H2 (Development) | PostgreSQL (Production) |
|--------|------------------|-------------------------|
| Startup Time | ~3 seconds | ~3-4 seconds |
| Query Speed | Very Fast (in-memory) | Fast (disk-based) |
| Concurrent Users | Limited | Excellent |
| Data Persistence | File-based | Robust ACID |
| Scalability | Single-user | Multi-user, clusterable |
| Backup/Recovery | File copy | Sophisticated tools |

---

## Conclusion

The MusicChat application is **fully database-agnostic** thanks to:
- ✅ JPA/Hibernate abstractions
- ✅ No vendor-specific SQL
- ✅ Proper dialect configuration
- ✅ Tested compatibility (Testcontainers)

**Migration is as simple as**:
1. Set up PostgreSQL
2. Update configuration
3. Run with `postgres` profile

**No code changes required!**

---

**Last Updated**: October 2, 2025  
**Version**: 1.0  
**Tested**: H2 ↔ PostgreSQL migration verified


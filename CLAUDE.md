# Project Structure & Architecture Guide

## Overview
Spring Boot 3.4.1 backend application with Java 17, featuring multi-tenant SaaS architecture with JWT authentication, role-based access control, and comprehensive business management.

## Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot 3.4.1
- **Build Tool**: Maven 3.9
- **Database**: PostgreSQL
- **Authentication**: JWT (io.jsonwebtoken)
- **Mapping**: MapStruct 1.6.3
- **Documentation**: OpenAPI/Swagger (springdoc-openapi)
- **Caching**: Caffeine Cache
- **Email**: Spring Mail
- **Container**: Docker & Docker Compose

## Project Structure

```
src/main/java/com/backend/
├── config/                          # Global configurations
│   ├── ApplicationConfig.java        # Beans, async, security auditor
│   ├── AsyncConfig.java             # Async/scheduling config
│   ├── CacheConfig.java             # Caffeine cache configuration
│   ├── DataInitializationService.java # Initial data setup
│   └── OpenApiConfig.java           # Swagger/OpenAPI documentation
├── enums/                           # Global enum definitions
│   ├── common/                      # Shared enums (Status, etc)
│   └── user/                        # User-specific enums
├── exception/                       # Exception handling
│   ├── GlobalExceptionHandler.java  # Global exception mapper
│   └── custom/                      # Custom exception classes
├── features/                        # Feature modules (scalable)
│   ├── auth/                        # Authentication feature
│   │   ├── controller/              # REST endpoints
│   │   ├── dto/                     # Request/response DTOs
│   │   ├── mapper/                  # MapStruct mappers
│   │   ├── models/                  # JPA entities
│   │   ├── repository/              # Data access layer
│   │   └── service/                 # Business logic
│   │       └── impl/                # Service implementations
│   └── setting/                     # Settings feature (example)
│       ├── controller/
│       ├── dto/
│       ├── mapper/
│       ├── models/
│       ├── repository/
│       └── service/
├── security/                        # Security components
│   ├── SecurityConfig.java          # Spring Security configuration
│   ├── SecurityUtils.java           # Security utility methods
│   ├── CustomUserDetailsService.java # Custom user details provider
│   └── jwt/                         # JWT implementation
│       ├── JwtAuthEntryPoint.java   # JWT entry point handler
│       ├── JWTAuthenticationFilter.java # JWT filter
│       ├── JWTGenerator.java        # Token generation
│       ├── TokenBlacklistService.java # Token blacklist management
│       └── impl/                    # JWT implementations
├── shared/                          # Shared utilities & commons
│   ├── constants/                   # Global constants
│   ├── domain/                      # Base domain classes
│   ├── dto/                         # Shared DTOs (responses, filters)
│   ├── generate/                    # Reference/ID generators
│   ├── mapper/                      # Shared mappers
│   ├── models/                      # Shared model entities
│   ├── pagination/                  # Pagination utilities
│   ├── repository/                  # Base/shared repositories
│   ├── retry/                       # Retry mechanisms
│   ├── utils/                       # Utility functions
│   └── validation/                  # Custom validators
└── BackendApplication.java          # Main Spring Boot application

src/main/resources/
├── application.yml                  # Spring configuration
├── application-dev.yml              # Dev profile config
├── application-prod.yml             # Production config
└── db/migration/                    # Flyway migrations
```

## Key Architectural Patterns

### 1. Feature-Based Organization
Each feature (`auth`, `setting`, etc.) is self-contained with:
- **Controller**: HTTP endpoints
- **DTO**: Request/Response data transfer objects
- **Mapper**: MapStruct mappers for entity ↔ DTO conversion
- **Models**: JPA entities
- **Repository**: Data access layer
- **Service**: Business logic (interface + implementation)

### 2. Shared Components
- **Constants**: Centralized application constants
- **DTOs**: Base response, pagination, filter classes
- **Validation**: Custom constraint validators
- **Pagination**: PaginationUtils, PaginationResponse
- **Retry**: Optimistic lock retry aspect
- **Generate**: Reference number generators

### 3. Security
- JWT-based authentication
- Role-based access control (RBAC)
- Token blacklist for logout
- Custom user details service
- Security context utilities

### 4. Cross-Cutting Concerns
- Global exception handler
- AOP-based retry mechanism
- Async task execution
- Cache configuration (Caffeine)
- Audit trail (JPA auditing)

## Configuration Files

### pom.xml
Maven configuration with:
- Spring Boot 3.4.1 parent POM
- Java 17 target
- All dependencies declared
- Maven compiler plugin for Java 17
- Annotation processor paths for MapStruct + Lombok

### Dockerfile
Two-stage Docker build:
1. **Build stage**: Maven 3.9 with Java 17 temurin
2. **Runtime stage**: Eclipse Temurin JRE 17

### Properties Files
- `application.yml`: Main configuration
- `application-dev.yml`: Development overrides
- `application-prod.yml`: Production overrides

## Naming Conventions

### Packages
- `com.backend.features.{featureName}` - Feature modules
- `com.backend.config` - Configuration classes
- `com.backend.security` - Security components
- `com.backend.shared` - Shared utilities

### Classes
- Controllers: `{Resource}Controller`
- Services: `{Domain}Service` (interface) + `{Domain}ServiceImpl` (implementation)
- Repositories: `{Entity}Repository`
- Mappers: `{Entity}Mapper`
- DTOs: `{Purpose}{DTO}` (e.g., `UserLoginRequestDto`)
- Entities: `{Entity}` (e.g., `User`, `Product`)

### Methods
- Service methods: `get*`, `create*`, `update*`, `delete*`, `find*`
- Controller methods: `get*`, `post*`, `update*`, `delete*`

## Common Development Tasks

### Adding a New Feature
1. Create feature package: `src/main/java/com/backend/features/{featureName}`
2. Create sub-packages: `controller`, `dto`, `mapper`, `models`, `repository`, `service`
3. Implement entity in `models`
4. Create repository extending `BaseRepository`
5. Create DTOs in `dto` with proper request/response separation
6. Create mapper interface extending `EntityMapper`
7. Create service interface and implementation
8. Create controller with REST endpoints
9. Add validation annotations to DTOs

### Database Changes
1. Create migration file in `src/main/resources/db/migration`
2. Use Flyway naming convention: `V{version}__{description}.sql`
3. Migrations auto-run on startup

### Adding Global Exception
1. Create custom exception in `exception/custom`
2. Add handler method in `GlobalExceptionHandler`
3. Return appropriate HTTP status and `ApiResponse`

### Pagination
Use `BaseFilterRequest` in controller:
```java
@PostMapping("/list")
public ResponseEntity<ApiResponse<List<UserDto>>> listUsers(
    @RequestBody BaseFilterRequest filter) {
    // Use PaginationUtils for pagination logic
}
```

## Java 17 Specific Features Used
- Text blocks ("""...""") for multiline strings
- Records (if any)
- Sealed classes (if any)
- Pattern matching (if any)

## Building & Running

### Development
```bash
mvn clean package -DskipTests
java -jar target/tiffany-cambodia-1.0.0.jar
```

### Docker
```bash
docker build -t backend:latest .
docker run -p 8080:8080 backend:latest
```

### Docker Compose
```bash
docker-compose -f docker-compose.build.yml up
```

## Quality & Best Practices
- Consistent code formatting
- MapStruct for type-safe mapping
- Lombok for reducing boilerplate
- Spring Security for comprehensive auth
- OpenAPI for API documentation
- Caffeine for caching
- JPA auditing for audit trails
- Custom validators for business rules
- Global exception handling
- Async task execution for long-running operations

## Environment-Specific Configuration
- Database URL, username, password
- JWT secret and expiration
- Email and Telegram settings
- Cache settings
- Async executor settings
- Logging levels

Use profiles (`dev`, `prod`) for different environments.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Claude Code Hooks - Notification Service**, a cross-platform Spring Boot application that provides real-time notifications for Claude Code permission requests and task completions. It replaces macOS-specific AppleScript solutions with a containerized web-based system.

**Core Purpose**: Get developer attention when Claude needs permission or completes tasks through audio alerts, OS notifications, and a web dashboard.

## Build and Development Commands

### Essential Commands
- **Start development server**: `./gradlew bootRun`
- **Build project**: `./gradlew build` 
- **Run tests**: `./gradlew test`
- **Clean build**: `./gradlew clean build`
- **Build JAR**: `./gradlew bootJar`
- **Lint and Format**: Kotlin formatting is enforced via build configuration

### Docker Commands
- **Build Docker image**: `./gradlew dockerBuild`
- **Start with Docker Compose**: `./gradlew dockerUp`
- **Stop Docker containers**: `./gradlew dockerDown`

### Script Management
- **Deploy Claude hooks scripts**: Run `src/main/resources/scripts/claude-hooks/deploy-claude-hooks.sh`
- **Update configuration**: Run `src/main/resources/scripts/claude-hooks/update-claude-hooks-config.sh`
- **Script libraries**: Located in `src/main/resources/scripts/claude-hooks/lib/` with utilities for logging, validation, JSON processing, and API calls

### Testing Commands
- **Run all tests**: `./gradlew test`
- **Run integration tests**: `./gradlew test --tests "*IntegrationTest"`
- **Run modularity tests**: `./gradlew test --tests "*ModularityTest"`
- **Run single test**: `./gradlew test --tests "NotificationHookIntegrationTest"`
- **Run specific test class**: `./gradlew test --tests "StopHookIntegrationTest"`
- **Run tests with detailed output**: `./gradlew test --info`
- **Run tests matching pattern**: `./gradlew test --tests "*Test"`
- **Run tests continuously**: `./gradlew test --continuous`

### Development Environment
- **JDK**: 21+ required
- **Gradle**: 8+ (use wrapper `./gradlew`)
- **Main class**: `com.cordona.claudecodehooks.Application`
- **Default port**: 8080 (development), 8085 (Docker)
- **Dashboard URL**: `http://localhost:8080` (development), `http://localhost:8085` (Docker)

### Environment Variables
- **SERVER_SENT_EVENTS_TIMEOUT_MILLIS**: SSE connection timeout in milliseconds (0 = no timeout)
- **SERVER_SENT_EVENTS_MAX_CONNECTIONS**: Maximum concurrent SSE connections
- **SSE_CACHE_MAX_USERS**: Maximum users in cache (controls memory usage)
- **SSE_CACHE_MAX_CONNECTIONS**: Maximum connections in cache
- **SSE_HEARTBEAT_ENABLED**: Enable/disable heartbeat system (`true`/`false`)
- **SSE_HEARTBEAT_INTERVAL_MILLIS**: Heartbeat interval in milliseconds
- **Environment files**: Local development uses `.env/local.env` (loaded automatically by bootRun task)

### Key Configuration Files
- **Application config**: `src/main/resources/application.yml`
- **Service config**: `src/main/resources/config/service-config.yml`
- **Security config**: `src/main/resources/config/security-config.yml`
- **Persistence config**: `src/main/resources/config/persistence-config.yml`
- **Dependencies**: `gradle/libs.versions.toml`
- **Build config**: `build.gradle.kts`
- **Docker stack**: `docker/docker-compose.yml`
- **Environment vars**: `.env` files for local development

## Architecture Overview

### SpringModulith Architecture
The application uses **SpringModulith** with strict module boundaries enforced at startup. Module violations cause application shutdown.

**Module Structure**:
```
web/ → service::api → infrastructure/messaging::api
```

**Key Modules**:
- **`web/`**: HTTP routing and handlers (REST endpoints)
- **`domain/`**: Domain logic and business rules (renamed from service)
- **`infrastructure/messaging/`**: Event publishing and SSE infrastructure
- **`persistence/`**: Data access layer with MongoDB integration
- **`security/`**: Authentication and authorization with Keycloak integration
- **`shared/`**: Common models, exceptions, and configuration properties

### Technology Stack
- **Backend**: Kotlin + Spring Boot 3.3+ with Virtual Threads (JDK 21)
- **Frontend**: Vanilla HTML/CSS/JavaScript with Server-Sent Events (SSE)
- **Audio**: Web Audio API with glass sound notifications
- **Notifications**: Browser Notification API for OS integration
- **Architecture**: SpringModulith with strict module boundaries
- **Authentication**: Keycloak OAuth2/OpenID Connect with JWT tokens
- **Database**: MongoDB for user data and application state
- **Infrastructure**: Docker Compose with PostgreSQL for Keycloak

### Event Flow Architecture
```
Claude Hook (curl) → HTTP Router → Handler → Domain Service → Event Publisher → SSE Stream → UI Dashboard
```

**Multi-Tenant SSE Architecture**: All Claude interactions (permission requests, task completions, errors) flow through a unified stream management API with user-specific event delivery.

**SSE Connection Management**:
- **Caffeine Caching**: High-performance caching with metrics and size-based limits
- **Connection Lifecycle**: Manual disconnect and service shutdown cleanup
- **User Isolation**: Events delivered only to target user's active connections
- **Scalable Design**: Supports thousands of concurrent connections with configurable limits
- **Heartbeat System**: Configurable periodic heartbeat events to detect dead connections
- **Cache Management**: Multi-layered caching (users, connections) with automatic eviction policies

## Authentication & Security

### OAuth2/OpenID Connect Integration
The application uses **Keycloak** as the identity provider with industry-standard OAuth2/OIDC flows:

- **External Authentication**: Users authenticate via Keycloak (not directly with the app)
- **Social Login Support**: GitHub, Google, Facebook, etc. handled by Keycloak
- **JWT Token Validation**: Spring Security automatically validates JWT tokens
- **CORS Configuration**: Strict cross-origin request handling

### Development Environment Setup
```bash
# Start full stack with Keycloak + MongoDB
./gradlew dockerUp

# Keycloak Admin Console: https://localhost:8443
# Application API: http://localhost:8085
```

**Docker Stack Options**:
- **HTTP Development**: `docker/docker-compose-http.yml` - Basic HTTP setup for local development
- **HTTPS Production**: `docker/docker-compose-https.yml` - Full stack with TLS certificates for Keycloak
- **Minimal App Only**: `docker-compose.yml` - Application container only (useful for external infrastructure)

**Docker Management Scripts**:
- **Stack Management**: `docker/scripts/manage_docker_stack.sh` - Start/stop/restart full infrastructure
- **Keycloak Setup**: `docker/scripts/setup_keycloak_http.sh` - Initial Keycloak realm configuration

### Authentication Flow
1. **Login**: Redirect to Keycloak (`https://localhost:8443/realms/claude-code-hooks/protocol/openid-connect/auth`)
2. **Token Exchange**: Exchange authorization code for JWT token
3. **API Access**: Include `Authorization: Bearer <jwt_token>` header for protected endpoints
4. **User Lookup**: Extract user info from JWT claims or call `/api/v1/user/lookup`

### Endpoint Security
- **Public**: Claude hook endpoints, health checks
- **Authenticated**: SSE stream, user management endpoints
- **Token Claims**: Access to `sub`, `preferred_username`, `email`, `name`, `identity_provider`

### API Key Authentication
The application supports **dual authentication modes**: JWT tokens for web dashboard access and API keys for automated Claude hook calls:

```bash
# API Key header authentication (for scripts/automation)
curl -H "X-API-Key: ck_dev_abc123..." http://localhost:8085/api/v1/claude-code/hooks/notification/event

# JWT Bearer token authentication (for web dashboard)  
curl -H "Authorization: Bearer <jwt_token>" http://localhost:8085/api/v1/claude-code/hooks/events/stream/connect
```

**API Key Features**:
- **Generated per user**: Each user can generate multiple API keys via `/api/v1/developer/apikey/generate`
- **Permission-based**: Keys include specific permissions (e.g., `HOOKS_WRITE`)
- **Prefix identification**: Keys start with `ck_dev_` for easy identification
- **Expiration support**: Optional expiration dates with automatic validation
- **Usage tracking**: Automatic `lastUsedAt` timestamp updates

## Key Integration Points

### Claude Code Integration
Configure in `~/.claude/settings.json`:
```json
{
  "hooks": {
    "Notification": [{
      "matcher": "*",
      "hooks": [{
        "type": "command",
        "command": "curl -s -X POST http://localhost:8080/api/v1/claude-code/hooks/notification/event -H 'Content-Type: application/json' -d '{\"message\":\"Claude needs your permission\", \"type\":\"APPROVAL\", \"title\":\"🤖 Claude Code\"}'"
      }]
    }],
    "Stop": [{
      "matcher": "*",
      "hooks": [{
        "type": "command",
        "command": "curl -s -X POST http://localhost:8080/api/v1/claude-code/hooks/stop/event -H 'Content-Type: application/json' -d '{\"message\":\"Claude task completed\", \"title\":\"🤖 Claude Code\"}'"
      }]
    }]
  }
}
```

### API Endpoints
- **POST /api/v1/claude-code/hooks/notification/event**: Trigger notification hooks (public)
- **POST /api/v1/claude-code/hooks/stop/event**: Trigger stop hooks (public)
- **GET /api/v1/claude-code/hooks/events/stream/connect**: Connect to SSE stream for real-time events (authenticated)
- **DELETE /api/v1/claude-code/hooks/events/stream/disconnect/{connectionId}**: Disconnect specific SSE connection (authenticated)
- **POST /api/v1/user/initialize**: Initialize new user (authenticated)
- **POST /api/v1/developer/apikey/generate**: Generate API key for Claude hook authentication (authenticated)
- **GET /actuator/health**: Service health check (public)

## Role-Based Access Control

### Multi-Role System
The application supports **multiple roles per user** to align with Keycloak's flexible role model:

```kotlin
// User model with role utilities
data class User(
    val id: String,
    val externalId: String,
    val roles: Set<Role>
) {
    fun hasRole(role: Role): Boolean = roles.contains(role)
    fun hasAnyRole(vararg targetRoles: Role): Boolean = targetRoles.any { roles.contains(it) }
    fun isAdmin(): Boolean = hasRole(Role.ADMIN)
    fun isUser(): Boolean = hasRole(Role.USER)
}
```

### JWT Role Extraction
JWT authentication extracts **all roles** from Keycloak instead of collapsing to single role:
- **Keycloak claim**: `realm_access.roles` contains array of roles
- **Spring Security**: Maps each role to `ROLE_<ROLE_NAME>` authority
- **User creation**: Supports users with multiple roles like `[USER, ADMIN]`

### Role Examples
```kotlin
// Single role user
val user = User(roles = setOf(USER))
user.isUser() // true
user.isAdmin() // false

// Multi-role user  
val admin = User(roles = setOf(USER, ADMIN))
admin.hasAnyRole(ADMIN, USER) // true
admin.hasRole(ADMIN) // true
```

## Development Patterns

### Functional Router Pattern
All HTTP routing uses Spring WebFlux functional routing:
```kotlin
@Bean
fun notificationEventEndpoint(): RouterFunction<ServerResponse> =
    RouterFunctions.route(POST(properties.claudeCode.hooks.notification.event), handler)
```

### Configuration-Driven Design
Endpoints are configured via `EndpointProperties` and YAML:
```yaml
service:
  web:
    endpoints:
      claude-code:
        hooks:
          notification:
            event: "/api/v1/claude-code/hooks/notification/event"
          stop:
            event: "/api/v1/claude-code/hooks/stop/event"
          events:
            stream:
              connect: "/api/v1/claude-code/hooks/events/stream/connect"
              disconnect: "/api/v1/claude-code/hooks/events/stream/disconnect"
```

### Event Publishing Pattern
Uses interface-driven event publishing with user-specific delivery:
```kotlin
interface EventPublisher {
    fun publishToUser(userExternalId: String, event: ClaudeHookEvent)
}
```

**Multi-Tenant Event Delivery**: Events are delivered only to the target user's active SSE connections, ensuring privacy and scalability.

### Virtual Threads
All async operations use Virtual Threads for high-concurrency SSE connections.

### Database & Persistence
The application uses **MongoDB** for data persistence with auditing support:

```kotlin
// Conditional MongoDB services
@ConditionalOnMongoService
class UserRepository : MongoRepository<UserEntity, String>

// Automatic auditing with Spring Data MongoDB (top-level fields)
@Document("users")
data class UserEntity(
    @Id val id: String,
    val externalId: String,
    val roles: Set<Role>, // Support multiple roles per user
    @CreatedDate var createdAt: Instant? = null,
    @LastModifiedDate var modifiedAt: Instant? = null,
    @CreatedBy var createdBy: String? = null,
    @LastModifiedBy var modifiedBy: String? = null,
    @Version var version: Long? = null
)
```

**Key Patterns**:
- **Repository Pattern**: MongoDB repositories with Spring Data
- **Entity Mapping**: Separation between domain models and persistence entities
- **Conditional Services**: Services only active when MongoDB is enabled
- **Audit Trail**: Automatic `createdAt`/`updatedAt` tracking on entities
- **Multi-Role Support**: Users can have multiple roles (e.g., `[USER, ADMIN]`) with utility methods for role checking

## File Organization

### Important Files
- **Main Application**: `src/main/kotlin/com/cordona/claudecodehooks/Application.kt`
- **HTTP Routers**: 
  - `src/main/kotlin/com/cordona/claudecodehooks/web/internal/rest/hooks/notification/event/NotificationEventRouter.kt`
  - `src/main/kotlin/com/cordona/claudecodehooks/web/internal/rest/hooks/stop/event/StopEventRouter.kt`
  - `src/main/kotlin/com/cordona/claudecodehooks/web/internal/rest/user/initialize/InitializeUserRouter.kt`
  - `src/main/kotlin/com/cordona/claudecodehooks/web/internal/rest/developer/apikey/GenerateApiKeyRouter.kt`
- **Request Handlers**: Located in corresponding router directories
- **Domain Services**: `src/main/kotlin/com/cordona/claudecodehooks/domain/internal/events/` and `domain/internal/developer/apikey/`
- **Event Publishers**: `src/main/kotlin/com/cordona/claudecodehooks/infrastructure/external/api/EventPublisher.kt`
- **SSE Infrastructure**: 
  - `src/main/kotlin/com/cordona/claudecodehooks/infrastructure/internal/messaging/sse/SseEventPublisher.kt`
  - `src/main/kotlin/com/cordona/claudecodehooks/infrastructure/internal/messaging/sse/SseConnectionManager.kt`
  - `src/main/kotlin/com/cordona/claudecodehooks/infrastructure/internal/messaging/sse/SseHeartbeatService.kt`
  - `src/main/kotlin/com/cordona/claudecodehooks/infrastructure/internal/messaging/sse/caching/SseConnectionCacheManager.kt`
- **Security Config**: 
  - `src/main/kotlin/com/cordona/claudecodehooks/security/internal/config/SecurityConfig.kt`
  - `src/main/kotlin/com/cordona/claudecodehooks/security/internal/apikey/ApiKeyAuthenticationFilter.kt`
- **Database Layer**: `src/main/kotlin/com/cordona/claudecodehooks/persistence/internal/mongo/` with entities, repositories, and services
- **Shared Models**: `src/main/kotlin/com/cordona/claudecodehooks/shared/models/` including `User.kt`, `ApiKey.kt`, `ClaudeHookEvent.kt`

### Module Dependencies
- **Web layer** depends on `domain::api` (business logic interfaces)  
- **Domain layer** depends on `infrastructure/messaging::api` and `persistence::api`
- **Infrastructure layer** has no domain dependencies (pure infrastructure)
- **Security layer** provides authentication/authorization across all layers
- **Shared layer** provides common models and utilities to all modules

## Testing and Validation

### Manual Testing
- **Dashboard Testing**: Use dashboard at `http://localhost:8085` with built-in test buttons (Docker)
- **SSE Authentication**: Access `/api/v1/claude-code/hooks/events/stream/connect` with valid JWT token
- **Public Hook Endpoints**: 
  - `curl -X POST http://localhost:8085/api/v1/claude-code/hooks/notification/event -H "Content-Type: application/json" -d '{"message":"Test notification"}'`
  - `curl -X POST http://localhost:8085/api/v1/claude-code/hooks/stop/event -H "Content-Type: application/json" -d '{"message":"Test completed"}'`
- **API Key Authentication**:
  - Generate API key: `curl -X POST http://localhost:8085/api/v1/developer/apikey/generate -H "Authorization: Bearer <jwt_token>" -H "Content-Type: application/json" -d '{"name":"test-key"}'`
  - Test API key: `curl -H "X-API-Key: ck_dev_..." http://localhost:8085/api/v1/claude-code/hooks/notification/event -H "Content-Type: application/json" -d '{"message":"API key test"}'`
- **User Management**: 
  - Initialize user: `curl -X POST http://localhost:8085/api/v1/user/initialize -H "Authorization: Bearer <jwt_token>"`
- **SSE Connection Management**:
  - Monitor connections: Browser dev tools network tab for EventSource at `/api/v1/claude-code/hooks/events/stream/connect`
  - Disconnect: `curl -X DELETE http://localhost:8085/api/v1/claude-code/hooks/events/stream/disconnect/{connectionId} -H "Authorization: Bearer <jwt_token>"`

### Audio System Testing
- Click anywhere on dashboard to initialize audio context
- Use dashboard sound toggle to test audio notifications
- Check browser console for audio context errors

## Extension Guidelines

### Adding New Hook Types
1. Create new router with endpoint configuration
2. Implement request handler following existing patterns
3. Add business logic in domain service
4. Use existing `EventPublisher` interface for events
5. Add endpoint configuration in `EndpointProperties`

### Adding New Publishers
Implement `EventPublisher` interface with conditional configuration:
```kotlin
@Service
@ConditionalOnProperty("messaging.slack.enabled", havingValue = "true")
class SlackEventPublisher : EventPublisher
```

## Code Quality and Validation

### Modulith Validation
The application enforces **strict SpringModulith boundaries** at startup. Any module boundary violations will cause the application to fail to start with detailed error messages.

- **Validation trigger**: Enabled via `service.modulith.strict.enabled=true`
- **Enforcement**: `@StrictModulith` annotation on main application class
- **Validation logic**: `ModulithValidator` component runs on `ContextRefreshedEvent`

### Code Style
- **Language**: Kotlin with null safety and immutability patterns
- **Architecture**: Clean architecture with SpringModulith boundaries
- **Async Processing**: Virtual Threads (JDK 21) for all async operations
- **Configuration**: YAML-based configuration with environment variable support

## Success Metrics
- **Audio Reliability**: 100% playbook success rate (resolved from "4/5 failures")
- **Cross-platform**: Works on macOS, Windows, Linux
- **Real-time**: < 100ms notification latency
- **Scalability**: Supports 1000+ concurrent SSE connections with Caffeine caching
- **Multi-Tenant Architecture**: User-isolated event delivery with manual connection cleanup
- **Configuration Management**: Fully externalized SSE configuration without hardcoded defaults
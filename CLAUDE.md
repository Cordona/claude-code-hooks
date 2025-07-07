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

### Development Environment
- **JDK**: 21+ required
- **Gradle**: 8+ (use wrapper `./gradlew`)
- **Main class**: `com.cordona.claudecodehooks.Application`
- **Default port**: 8080
- **Dashboard URL**: `http://localhost:8080`

### Key Configuration Files
- **Application config**: `src/main/resources/application.yml`
- **Service config**: `src/main/resources/config/service-config.yml`
- **Dependencies**: `gradle/libs.versions.toml`
- **Build config**: `build.gradle.kts`

## Architecture Overview

### SpringModulith Architecture
The application uses **SpringModulith** with strict module boundaries enforced at startup. Module violations cause application shutdown.

**Module Structure**:
```
web/ → service::api → events::api
```

**Key Modules**:
- **`web/`**: HTTP routing and handlers (REST endpoints)
- **`service/`**: Domain logic and business rules
- **`events/`**: Event publishing and SSE infrastructure
- **`shared/`**: Common models and configuration properties

### Technology Stack
- **Backend**: Kotlin + Spring Boot 3.3+ with Virtual Threads (JDK 21)
- **Frontend**: Vanilla HTML/CSS/JavaScript with Server-Sent Events (SSE)
- **Audio**: Web Audio API with glass sound notifications
- **Notifications**: Browser Notification API for OS integration
- **Architecture**: SpringModulith with strict module boundaries

### Event Flow Architecture
```
Claude Hook (curl) → HTTP Router → Handler → Domain Service → Event Publisher → SSE Stream → UI Dashboard
```

**Single SSE Stream Design**: All Claude interactions (permission requests, task completions, errors) flow through one unified event stream at `/notifications` endpoint.

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
        "command": "curl -s -X POST http://localhost:8080/notify -H 'Content-Type: application/json' -d '{\"message\":\"Claude needs your permission\", \"type\":\"APPROVAL\", \"title\":\"🤖 Claude Code\"}'"
      }]
    }]
  }
}
```

### API Endpoints
- **POST /notify**: Trigger notifications
- **GET /notifications**: SSE stream for real-time events
- **GET /status**: Service health check

## Development Patterns

### Functional Router Pattern
All HTTP routing uses Spring WebFlux functional routing:
```kotlin
@Bean
fun notificationMessageEndpoint(): RouterFunction<ServerResponse> =
    RouterFunctions.route(POST(properties.claudeCode.hooks.notification.message), handler)
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
            message: "/api/v1/claude-code/hooks/notification/message"
```

### Event Publishing Pattern
Uses interface-driven event publishing with pluggable implementations:
```kotlin
interface EventPublisher {
    fun publish(event: ClaudeHookEvent)
}
```

### Virtual Threads
All async operations use Virtual Threads for high-concurrency SSE connections.

## File Organization

### Important Files
- **Main Application**: `src/main/kotlin/com/cordona/claudecodehooks/Application.kt`
- **HTTP Router**: `src/main/kotlin/com/cordona/claudecodehooks/web/internal/rest/hooks/notification/message/NotificationMessageRouter.kt`
- **Request Handler**: `NotificationMessageHandler.kt`
- **Event Publisher**: `src/main/kotlin/com/cordona/claudecodehooks/events/external/publisher/EventPublisher.kt`
- **SSE Implementation**: `src/main/kotlin/com/cordona/claudecodehooks/events/internal/sse/SseEventPublisher.kt`
- **Properties**: `src/main/kotlin/com/cordona/claudecodehooks/web/internal/properties/EndpointProperties.kt`

### Module Dependencies
- **Web layer** depends on `service::api` (business logic interfaces)
- **Service layer** depends on `events::api` (event publishing interfaces)
- **Events layer** has no domain dependencies (pure infrastructure)

## Testing and Validation

### Manual Testing
- Use dashboard at `http://localhost:8080` with built-in test buttons
- Test curl commands: `curl -X POST http://localhost:8080/notify -H "Content-Type: application/json" -d '{"message":"Test notification"}'`
- Check SSE stream: Open browser dev tools and monitor network tab for EventSource connections

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

## Success Metrics
- **Audio Reliability**: 100% playbook success rate (resolved from "4/5 failures")
- **Cross-platform**: Works on macOS, Windows, Linux
- **Real-time**: < 100ms notification latency
- **Scalability**: Supports 1000+ concurrent SSE connections
# Claude Code Hooks - Notification Service

[![Kotlin](https://img.shields.io/badge/kotlin-1.9.24-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![JDK](https://img.shields.io/badge/JDK-21+-orange.svg)](https://openjdk.org)
[![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?logo=docker&logoColor=white)](https://www.docker.com/)
[![Keycloak](https://img.shields.io/badge/Keycloak-OAuth2-red.svg)](https://www.keycloak.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-4EA94B?logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A modern, enterprise-grade notification service for Claude Code hooks, built with **Kotlin** and **Spring Boot**. Features OAuth2 authentication, multi-tenant architecture, real-time SSE notifications, and comprehensive API key management for seamless Claude Code integration.

## 🎯 Features

### Core Functionality
- ⚡ **Real-time notifications** via Server-Sent Events (SSE)
- 🔊 **Audio alerts** with glass sound notifications
- 📱 **OS-native browser notifications** 
- 📊 **Interactive web dashboard** with notification history
- 🐳 **Docker containerization** for easy deployment

### Authentication & Security
- 🔐 **OAuth2/OpenID Connect** integration with Keycloak
- 🗝️ **API Key authentication** for automated Claude hook calls
- 👥 **Multi-tenant architecture** with user isolation
- 🛡️ **Role-based access control** with multiple roles per user
- 🔒 **JWT token validation** and secure session management

### Technical Highlights
- 🚀 **SpringModulith architecture** with strict module boundaries
- ⚡ **Virtual Threads (JDK 21)** for high-concurrency SSE connections
- 🗄️ **MongoDB persistence** with auditing and caching
- 🔄 **Automatic reconnection** with connection health monitoring
- 💾 **Caffeine caching** for high-performance SSE connection management
- 🛡️ **Comprehensive error handling** and graceful degradation
- 🧪 **Full test coverage** with integration testing

### Enterprise Features
- 📈 **Scalable design** supporting 1000+ concurrent connections
- 🏗️ **Multi-layered caching** with configurable limits
- 💓 **Heartbeat system** for connection health monitoring
- 📊 **Usage tracking** and API key analytics
- 🌐 **Social login support** via Keycloak (GitHub, Google, etc.)

### Cross-Platform Support
- 🍎 macOS
- 🪟 Windows  
- 🐧 Linux

## 🏗️ Architecture

### High-Level Overview
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Claude Code   │───▶│  Notification    │───▶│   Web Dashboard │
│     Hooks       │    │    Service       │    │ + Audio + OS    │
│   (API Keys)    │    │  (OAuth2 + JWT)  │    │  (Authenticated) │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SpringModulith Architecture                  │
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │
│ │ Web Layer   │ │ Domain      │ │Infrastructure│ │ Persistence │ │
│ │ (Routers +  │ │ (Business   │ │ (SSE + Cache │ │ (MongoDB +  │ │
│ │ Handlers)   │ │ Logic)      │ │ + Events)    │ │ Auditing)   │ │
│ └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ │
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                 │
│ │ Security    │ │ Shared      │ │ Validation  │                 │
│ │ (Auth +     │ │ (Models +   │ │ (Modulith   │                 │
│ │ API Keys)   │ │ Utils)      │ │ Boundaries) │                 │
│ └─────────────┘ └─────────────┘ └─────────────┘                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    External Infrastructure                       │
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                 │
│ │ Keycloak    │ │ MongoDB     │ │ Docker      │                 │
│ │ (OAuth2 +   │ │ (Users +    │ │ (Container  │                 │
│ │ Social      │ │ API Keys +  │ │ + Network   │                 │
│ │ Login)      │ │ Auditing)   │ │ + Health)   │                 │
│ └─────────────┘ └─────────────┘ └─────────────┘                 │
└─────────────────────────────────────────────────────────────────┘
```

### Event Flow Architecture
```
Claude Hook (API Key) → Security Filter → HTTP Router → Handler → 
Domain Service → Event Publisher → SSE Stream → Dashboard (JWT)
```

**Tech Stack:**
- **Backend**: Kotlin + Spring Boot 3.3+ with Virtual Threads
- **Frontend**: Vanilla HTML/CSS/JavaScript with SSE
- **Authentication**: Keycloak OAuth2/OpenID Connect + JWT
- **Database**: MongoDB with Spring Data + Auditing
- **Architecture**: SpringModulith with strict module boundaries
- **Caching**: Caffeine for high-performance SSE connections
- **Audio**: Web Audio API with intelligent context management
- **Containerization**: Docker Compose with PostgreSQL + MongoDB

## 🚀 Quick Start

### Prerequisites

- **Java**: JDK 21+ (required for Virtual Threads)
- **Build Tool**: Gradle 8+ (wrapper included)
- **Database**: MongoDB 4.4+ (included in Docker stack)
- **Authentication**: Keycloak 22+ (included in Docker stack)
- **Optional**: Docker for full-stack deployment

### Option 1: Full Stack Development (Recommended)

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Cordona/claude-code-hooks.git
   cd claude-code-hooks
   ```

2. **Start the full stack**:
   ```bash
   # Start Keycloak + MongoDB + Application
   ./gradlew dockerUp
   ```

3. **Access the services**:
   - **Application Dashboard**: [http://localhost:8085](http://localhost:8085)
   - **Keycloak Admin**: [https://localhost:8443](https://localhost:8443) (admin/admin)
   - **MongoDB**: localhost:27017

4. **Initialize your user**:
   - Login through the dashboard using Keycloak
   - Your user will be automatically created in MongoDB
   - Generate API keys for Claude Code integration

### Option 2: Local Development Only

1. **Run application only** (requires external MongoDB):
   ```bash
   ./gradlew bootRun
   ```

2. **Test the service** (public endpoints):
   ```bash
   curl -X POST http://localhost:8080/api/v1/claude-code/hooks/notification/event \
     -H "Content-Type: application/json" \
     -d '{
       "session_id": "test-session",
       "transcript_path": "/tmp/test.md",
       "hook_event_name": "Notification", 
       "message": "Test notification from Claude Code"
     }'
   ```

## 🐳 Docker Deployment

### Option 1: Full Stack Deployment (Recommended)

1. **Deploy the complete infrastructure**:
   ```bash
   # Deploy with Keycloak + MongoDB + Application
   ./gradlew dockerUp
   ```

   This will start:
   - **PostgreSQL**: Database for Keycloak
   - **Keycloak**: OAuth2 provider with realm setup
   - **MongoDB**: Application database
   - **Claude Code Hooks**: Main application

2. **Access the services**:
   - **Application**: [http://localhost:8085](http://localhost:8085)
   - **Keycloak**: [https://localhost:8443](https://localhost:8443)
   - **Health Check**: [http://localhost:8085/actuator/health](http://localhost:8085/actuator/health)

3. **Stack management**:
   ```bash
   # Stop all services
   ./gradlew dockerDown
   
   # View logs
   docker compose logs -f claude-code-hooks
   
   # Restart specific service
   docker compose restart claude-code-hooks
   ```

### Option 2: Standalone Application

1. **Application only** (requires external infrastructure):
   ```bash
   # Build and start app container only
   ./gradlew dockerBuild
   docker run -d \
     --name claude-code-hooks \
     -p 8085:8085 \
     -e SPRING_DATA_MONGODB_URI=mongodb://host.docker.internal:27017/claude-hooks \
     --restart unless-stopped \
     claude-code-hooks:latest
   ```

### Option 2: Manual Docker Commands

1. **Build the application**:
   ```bash
   ./gradlew build
   ```

2. **Build Docker image**:
   ```bash
   docker build -t claude-code-hooks:latest .
   ```

3. **Run the container**:
   ```bash
   docker run -d \
     --name claude-code-hooks \
     -p 8085:8085 \
     --restart unless-stopped \
     claude-code-hooks:latest
   ```

4. **View logs**:
   ```bash
   docker logs -f claude-code-hooks
   ```

5. **Stop and remove**:
   ```bash
   docker stop claude-code-hooks
   docker rm claude-code-hooks
   ```

### Option 3: Production Deployment

For production environments, use the provided docker-compose configuration:

```bash
# Set environment variables
export SERVER_PORT=8085

# Deploy with health checks and networking
docker-compose up -d

# Monitor health
docker-compose ps
docker-compose logs -f claude-code-hooks
```

The service includes:
- Health checks with automatic restarts
- Isolated bridge networking
- Configurable port mapping
- Resource limits and optimizations

## ⚙️ Claude Code Integration

### 1. Generate API Key

1. **Access the dashboard** and login via Keycloak:
   ```
   http://localhost:8085
   ```

2. **Generate an API key** for Claude Code:
   ```bash
   curl -X POST http://localhost:8085/api/v1/developer/apikey/generate \
     -H "Authorization: Bearer <your-jwt-token>" \
     -H "Content-Type: application/json" \
     -d '{
       "name": "claude-code-integration",
       "permissions": ["HOOKS_WRITE"],
       "expiresAt": "2024-12-31T23:59:59Z"
     }'
   ```

3. **Save the API key** (starts with `ck_dev_`) for Claude Code configuration.

### 2. Configure Claude Code Hooks

Edit `~/.claude/settings.json`:

```json
{
  "hooks": {
    "Notification": [{
      "matcher": "*",
      "hooks": [{
        "type": "command",
        "command": "curl -s -X POST http://localhost:8085/api/v1/claude-code/hooks/notification/event -H 'Content-Type: application/json' -H 'X-API-Key: ck_dev_your_api_key_here' -d @-"
      }]
    }],
    "Stop": [{
      "matcher": "*", 
      "hooks": [{
        "type": "command",
        "command": "curl -s -X POST http://localhost:8085/api/v1/claude-code/hooks/stop/event -H 'Content-Type: application/json' -H 'X-API-Key: ck_dev_your_api_key_here' -d @-"
      }]
    }]
  }
}
```

### 3. Alternative: Deploy Scripts (Automated Setup)

Use the provided deployment scripts for automated configuration:

```bash
# Deploy Claude hooks with automatic API key setup
./src/main/resources/scripts/claude-hooks/deploy-claude-hooks.sh

# Update existing configuration
./src/main/resources/scripts/claude-hooks/update-claude-hooks-config.sh
```

### 4. Enable Dashboard Notifications

1. **Login to dashboard**: [http://localhost:8085](http://localhost:8085)
2. **Authenticate via Keycloak**: Use GitHub, Google, or email/password
3. **Enable browser notifications**: Click "📱 Enable OS Notifications"
4. **Initialize audio**: Click anywhere to enable sound notifications

### 5. Authentication Methods

**For Dashboard Access** (Interactive):
- **JWT Tokens**: OAuth2 login via Keycloak
- **Social Login**: GitHub, Google, Facebook, etc.
- **Session Management**: Automatic token refresh

**For Claude Code Hooks** (Automated):
- **API Keys**: Generated per-user with specific permissions
- **Header Authentication**: `X-API-Key: ck_dev_...`
- **Usage Tracking**: Automatic last-used timestamp updates

### 6. Test Integration

```bash
# Test notification with API key
curl -X POST http://localhost:8085/api/v1/claude-code/hooks/notification/event \
  -H "Content-Type: application/json" \
  -H "X-API-Key: ck_dev_your_api_key_here" \
  -d '{
    "session_id": "test-session",
    "transcript_path": "/tmp/test.md",
    "hook_event_name": "Notification",
    "message": "Test from Claude Code with API key"
  }'

# Test stop hook
curl -X POST http://localhost:8085/api/v1/claude-code/hooks/stop/event \
  -H "Content-Type: application/json" \
  -H "X-API-Key: ck_dev_your_api_key_here" \
  -d '{
    "session_id": "test-session",
    "transcript_path": "/tmp/test.md",
    "hook_event_name": "Stop",
    "message": "Task completed"
  }'
```

## 🔧 Configuration

### Application Configuration

Key settings in `src/main/resources/application.yml`:

```yaml
server:
  port: 8085
  
spring:
  application:
    name: claude-code-hooks
  data:
    mongodb:
      uri: mongodb://localhost:27017/claude-hooks
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://localhost:8443/realms/claude-code-hooks
    
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8085` | HTTP server port |
| `SPRING_DATA_MONGODB_URI` | `mongodb://localhost:27017/claude-hooks` | MongoDB connection |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | `https://localhost:8443/realms/claude-code-hooks` | Keycloak issuer |
| `SERVER_SENT_EVENTS_TIMEOUT_MILLIS` | `0` | SSE timeout (0 = no timeout) |
| `SERVER_SENT_EVENTS_MAX_CONNECTIONS` | `1000` | Max SSE connections |
| `SSE_CACHE_MAX_USERS` | `1000` | Max users in cache |
| `SSE_HEARTBEAT_ENABLED` | `true` | Enable heartbeat system |
| `SSE_HEARTBEAT_INTERVAL_MILLIS` | `30000` | Heartbeat interval |
| `JAVA_OPTS` | `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` | JVM options |

### Dashboard Settings

- **Audio notifications**: Toggle sound on/off
- **Retention policy**: Configure notification history (10/25/50/100)
- **Testing tools**: Built-in notification testing
- **Debug logging**: Real-time event monitoring

## 🧪 Testing

### Run Tests

```bash
# Run all tests
./gradlew test

# Run specific test categories
./gradlew test --tests "*IntegrationTest"
./gradlew test --tests "*ModularityTest"
```

### Test Coverage

The project includes comprehensive testing:
- **Unit tests**: Service layer and utilities
- **Integration tests**: Full HTTP request/response cycles with SSE
- **Modularity tests**: SpringModulith boundary validation
- **Container tests**: Docker deployment verification

### Manual Testing

1. **Dashboard testing**: Use built-in test buttons
2. **API testing**: Use provided curl commands
3. **SSE testing**: Monitor real-time event streams
4. **Audio testing**: Verify sound notifications work

## 📊 Monitoring

### Health Checks

- **Endpoint**: `GET /actuator/health`
- **Docker**: Built-in health check with retry logic
- **Monitoring**: View active SSE connections and system status

### Logging

- **Application logs**: Structured JSON logging with correlation IDs
- **SSE connection tracking**: Real-time connection management
- **Error handling**: Comprehensive error logging and recovery

## 🚦 API Reference

### Authentication Endpoints

#### Generate API Key
```http
POST /api/v1/developer/apikey/generate
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "name": "string",
  "permissions": ["HOOKS_WRITE"],
  "expiresAt": "2024-12-31T23:59:59Z"  // optional
}
```

#### Initialize User
```http
POST /api/v1/user/initialize
Authorization: Bearer <jwt-token>
```

### Hook Endpoints (API Key Authentication)

#### Notification Hook
```http
POST /api/v1/claude-code/hooks/notification/event
Content-Type: application/json
X-API-Key: ck_dev_your_api_key

{
  "session_id": "string",
  "transcript_path": "string", 
  "hook_event_name": "Notification",
  "message": "string"
}
```

#### Stop Hook
```http
POST /api/v1/claude-code/hooks/stop/event
Content-Type: application/json
X-API-Key: ck_dev_your_api_key

{
  "session_id": "string",
  "transcript_path": "string",
  "hook_event_name": "Stop", 
  "stop_hook_active": boolean
}
```

### Real-time Events (JWT Authentication)

#### SSE Stream Connection
```http
GET /api/v1/claude-code/hooks/events/stream/connect
Authorization: Bearer <jwt-token>
Accept: text/event-stream
```

#### SSE Disconnect
```http
DELETE /api/v1/claude-code/hooks/events/stream/disconnect/{connectionId}
Authorization: Bearer <jwt-token>
```

### Health & Monitoring

#### Health Check
```http
GET /actuator/health
```

Returns application health status and dependencies.

### Authentication Modes

| Endpoint Type | Authentication | Use Case |
|---------------|----------------|----------|
| **Hook Events** | API Key (`X-API-Key`) | Claude Code automation |
| **Dashboard/SSE** | JWT Token (`Authorization: Bearer`) | Web interface |
| **Health** | Public | Monitoring |

## 🛠️ Development

### Project Structure

```
src/main/kotlin/com/cordona/claudecodehooks/
├── web/                    # HTTP routing and handlers
│   └── internal/rest/      # REST endpoints (routers + handlers)
├── domain/                 # Business logic and domain services
│   ├── external/api/       # External API interfaces
│   └── internal/           # Internal domain services
├── infrastructure/         # Infrastructure and messaging
│   ├── external/api/       # External event publishing
│   └── internal/messaging/sse/  # SSE connection management
├── persistence/            # Data access layer
│   ├── external/api/       # Persistence interfaces
│   └── internal/mongo/     # MongoDB implementation
├── security/               # Authentication and authorization
│   ├── external/api/       # Security interfaces
│   └── internal/           # JWT + API Key implementation
├── shared/                 # Common models, enums, utilities
│   ├── models/             # Domain models (User, ApiKey, etc.)
│   ├── commands/           # Command objects
│   ├── enums/              # Permissions, Roles, etc.
│   └── exceptions/         # Custom exceptions
└── validation/             # SpringModulith boundary validation
```

### Module Dependencies

```
web → domain::api → {infrastructure::api, persistence::api}
security → {domain::api, persistence::api}
shared → (used by all modules)
```

### Building

```bash
# Clean build
./gradlew clean build

# Build with tests
./gradlew build

# Build Docker image
./gradlew dockerBuild

# Build and deploy
./gradlew dockerUp
```

### Code Quality

The project follows Kotlin best practices:
- **Null safety**: Leveraging Kotlin's type system
- **Immutability**: Data classes and immutable collections
- **Functional style**: Higher-order functions and functional programming
- **SpringModulith**: Strict architectural boundaries

## 🐛 Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| **Authentication failed** | Check Keycloak is running and JWT token is valid |
| **API key rejected** | Verify API key format (`ck_dev_...`) and permissions |
| **No audio notifications** | Click anywhere on dashboard to initialize audio context |
| **SSE connection drops** | Service auto-reconnects every 3 seconds, check JWT expiry |
| **User not found** | Call `/api/v1/user/initialize` first or login via dashboard |
| **MongoDB connection failed** | Ensure MongoDB is running and URI is correct |
| **Keycloak login redirect fails** | Check issuer URI and realm configuration |
| **Port conflicts** | Change `SERVER_PORT` environment variable |
| **Docker build fails** | Ensure JDK 21+ is available |
| **Permission denied errors** | Check Claude settings.json syntax and API key |

### Debug Mode

Enable debug logging by setting the log level:

```yaml
logging:
  level:
    com.cordona.claudecodehooks: DEBUG
```

### Performance Tuning

For high-volume deployments:

```bash
# Increase JVM heap and enable Virtual Threads optimization
export JAVA_OPTS="-Xmx2g -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Scale with multiple containers
docker-compose up --scale claude-code-hooks=3
```

## 📈 Success Metrics

✅ **Cross-platform compatibility** - Works on macOS, Windows, Linux  
✅ **Real-time performance** - <100ms notification latency  
✅ **High availability** - Automatic reconnection and health monitoring  
✅ **Production ready** - Containerized with comprehensive testing  
✅ **Maintainable** - SpringModulith architecture with clear boundaries  

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feat/amazing-feature`
5. Open a Pull Request

### Development Guidelines

- Follow Kotlin coding conventions
- Maintain SpringModulith boundaries
- Add tests for new features
- Update documentation
- Ensure Docker compatibility

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Claude Code team** for the extensible hook system
- **Spring Team** for SpringModulith architecture patterns
- **Kotlin community** for excellent tooling and ecosystem
- **Docker** for containerization best practices

---

**⭐ Star this repository if it helps you improve your Claude Code workflow!**
# Claude Code Hooks - Notification Service

[![Kotlin](https://img.shields.io/badge/kotlin-1.9.24-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![JDK](https://img.shields.io/badge/JDK-21+-orange.svg)](https://openjdk.org)
[![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?logo=docker&logoColor=white)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A modern, cross-platform notification service for Claude Code hooks, built with **Kotlin** and **Spring Boot**. Replaces macOS-specific AppleScript solutions with containerized web-based notifications featuring real-time SSE, audio alerts, and OS integration.

## 🎯 Features

### Core Functionality
- ⚡ **Real-time notifications** via Server-Sent Events (SSE)
- 🔊 **Audio alerts** with glass sound notifications
- 📱 **OS-native browser notifications** 
- 📊 **Interactive web dashboard** with notification history
- 🐳 **Docker containerization** for easy deployment

### Technical Highlights
- 🚀 **SpringModulith architecture** with strict module boundaries
- ⚡ **Virtual Threads (JDK 21)** for high-concurrency SSE connections
- 🔄 **Automatic reconnection** with connection health monitoring
- 🛡️ **Comprehensive error handling** and graceful degradation
- 🧪 **Full test coverage** with integration testing

### Cross-Platform Support
- 🍎 macOS
- 🪟 Windows  
- 🐧 Linux

## 🏗️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Claude Code   │───▶│  Notification    │───▶│   Web Dashboard │
│     Hooks       │    │    Service       │    │   + Audio + OS  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌──────────────────┐
                       │ SpringModulith   │
                       │ ┌──────────────┐ │
                       │ │ Web Layer    │ │
                       │ └──────────────┘ │
                       │ ┌──────────────┐ │
                       │ │ Service      │ │
                       │ └──────────────┘ │
                       │ ┌──────────────┐ │
                       │ │Infrastructure│ │
                       │ └──────────────┘ │
                       └──────────────────┘
```

**Tech Stack:**
- **Backend**: Kotlin + Spring Boot 3.3+ with Virtual Threads
- **Frontend**: Vanilla HTML/CSS/JavaScript with SSE
- **Architecture**: SpringModulith with modular boundaries
- **Audio**: Web Audio API with intelligent context management
- **Containerization**: Docker with multi-platform support

## 🚀 Quick Start

### Prerequisites

- **Java**: JDK 21+ (required for Virtual Threads)
- **Build Tool**: Gradle 8+ (wrapper included)
- **Optional**: Docker for containerized deployment

### Local Development

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Cordona/claude-code-hooks.git
   cd claude-code-hooks
   ```

2. **Run the application**:
   ```bash
   ./gradlew bootRun
   ```

3. **Access the dashboard**:
   Open [http://localhost:8085](http://localhost:8085)

4. **Test the service**:
   ```bash
   curl -X POST http://localhost:8085/api/v1/claude-code/hooks/notification/event \
     -H "Content-Type: application/json" \
     -d '{
       "session_id": "test-session",
       "transcript_path": "/tmp/test.md",
       "hook_event_name": "Notification", 
       "message": "Test notification from Claude Code"
     }'
   ```

## 🐳 Docker Deployment

### Option 1: Using Docker Compose (Recommended)

1. **Build and start the service**:
   ```bash
   ./gradlew dockerUp
   ```

   This command will:
   - Build the application JAR
   - Create the Docker image
   - Start the container with proper networking

2. **Access the service**:
   - Dashboard: [http://localhost:8085](http://localhost:8085)
   - Container name: `claude-code-hooks`

3. **Stop the service**:
   ```bash
   ./gradlew dockerDown
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

Configure Claude Code to send hooks to the notification service:

### 1. Update Claude Settings

Edit `~/.claude/settings.json`:

```json
{
  "hooks": {
    "Notification": [{
      "matcher": "*",
      "hooks": [{
        "type": "command",
        "command": "curl -s -X POST http://localhost:8085/api/v1/claude-code/hooks/notification/event -H 'Content-Type: application/json' -d @-"
      }]
    }],
    "Stop": [{
      "matcher": "*", 
      "hooks": [{
        "type": "command",
        "command": "curl -s -X POST http://localhost:8085/api/v1/claude-code/hooks/stop/event -H 'Content-Type: application/json' -d @-"
      }]
    }]
  }
}
```

### 2. Enable Browser Notifications

1. Open the dashboard at [http://localhost:8085](http://localhost:8085)
2. Click "📱 Enable OS Notifications" 
3. Grant permission in your browser
4. Click anywhere on the dashboard to initialize audio context

### 3. Test Integration

Trigger a test notification through Claude Code or use the dashboard's built-in test tools.

## 🔧 Configuration

### Application Configuration

Key settings in `src/main/resources/application.yml`:

```yaml
server:
  port: 8085
  
spring:
  application:
    name: claude-code-hooks
    
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

### Notification Endpoint

```http
POST /api/v1/claude-code/hooks/notification/event
Content-Type: application/json

{
  "session_id": "string",
  "transcript_path": "string", 
  "hook_event_name": "Notification",
  "message": "string"
}
```

### Stop Hook Endpoint

```http
POST /api/v1/claude-code/hooks/stop/event
Content-Type: application/json

{
  "session_id": "string",
  "transcript_path": "string",
  "hook_event_name": "Stop", 
  "stop_hook_active": boolean
}
```

### SSE Stream

```http
GET /api/v1/claude-code/hooks/events/stream
Accept: text/event-stream
```

Returns real-time events in SSE format with automatic reconnection.

## 🛠️ Development

### Project Structure

```
src/main/kotlin/com/cordona/claudecodehooks/
├── web/                    # HTTP routing and handlers
├── service/                # Business logic and domain services  
├── infrastructure/         # SSE messaging and external integrations
├── shared/                 # Common models and enums
├── validation/            # SpringModulith validation
└── config/                # Application configuration
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
| **No audio notifications** | Click anywhere on dashboard to initialize audio context |
| **SSE connection drops** | Service auto-reconnects every 3 seconds |
| **Port conflicts** | Change `SERVER_PORT` environment variable |
| **Docker build fails** | Ensure JDK 21+ is available |
| **Permission denied errors** | Check Claude settings.json syntax |

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
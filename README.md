# 🎵 Harmonia

> A real-time music discovery and sharing platform with integrated chat, audio streaming, and collaborative playlists.

**Harmonia** is a full-stack web application that combines social messaging with music sharing. Users can chat in real-time, upload and stream audio files, create playlists, rate tracks, and integrate metadata from external sources like iTunes. Built with Spring Boot and designed for scalability, Harmonia provides a modern, responsive UI and robust backend architecture.

---

## ✨ Features

- **Real-Time Chat**: WebSocket-powered instant messaging with persistent message history
- **Audio Upload & Streaming**: Secure file upload with UUID-based storage and on-demand streaming
- **Playlist Management**: Create, edit, and share playlists with track ordering and search functionality
- **Multi-User Support**: Full authentication and authorization with user-specific content ownership
- **Ratings & Favorites**: Rate tracks and mark favorites for personalized recommendations
- **External Metadata Integration**: Fetch track information from iTunes API and other sources
- **Interactive Music Cards**: Play shared tracks and expand playlists directly in the chat interface
- **Responsive Design**: Modern, Spotify-inspired UI with glassmorphism effects and mobile support

---

## 🏗️ Architecture & Stack

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Build Tool**: Maven
- **Database**: H2 (development) with PostgreSQL compatibility
- **ORM**: Spring Data JPA / Hibernate
- **Security**: Spring Security with authentication & authorization
- **Real-Time**: WebSocket (STOMP protocol)
- **REST API**: RESTful endpoints for all operations

### Frontend
- **Template Engine**: Thymeleaf
- **Styling**: CSS3 with modern glassmorphism effects
- **JavaScript**: Vanilla JS with WebSocket integration
- **Alternative**: Console runner for API testing

### Testing
- **Unit Tests**: JUnit 5 with Mockito
- **Integration Tests**: Spring Boot Test with MockMvc
- **E2E Tests**: Comprehensive workflow validation
- **Concurrency Tests**: Multi-user simulation tests
- **Database Tests**: Testcontainers for PostgreSQL validation
- **Security Tests**: Authentication and authorization coverage

---

## 🚀 Installation & Usage

### Prerequisites
- **Java 17** or higher
- **Maven 3.8+**
- **Git**

### Quick Start

```bash
# Clone the repository
git clone https://github.com/yourusername/harmonia.git
cd harmonia

# Run the application (H2 database)
mvn spring-boot:run

# Access the application
# Home: http://localhost:8080
# Chat: http://localhost:8080/chat
# Audio: http://localhost:8080/audio
# H2 Console: http://localhost:8080/h2-console
```

### Running with PostgreSQL

```bash
# Run with PostgreSQL profile
mvn spring-boot:run -Dspring-boot.run.profiles=postgres

# Or build and run the JAR
mvn clean package
java -jar target/musicchat-0.0.1-SNAPSHOT.jar --spring.profiles.active=postgres
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test suites
mvn test -Dtest="edu.cqu.coit13235.musicchat.e2e.*"           # E2E tests
mvn test -Dtest="edu.cqu.coit13235.musicchat.security.*"      # Security tests
mvn test -Dtest="ConcurrentUserSimulationTest"                 # Concurrency tests
mvn test -Dtest="PostgreSQLCompatibilityTest"                  # Database tests

# Generate test report
mvn surefire-report:report
```

---

## 📡 API Endpoints

### Chat
- `GET /api/chat/messages` - Retrieve all messages
- `POST /api/chat/send` - Send a new message
- `WebSocket /ws-chat` - Real-time chat connection

### Audio
- `POST /api/audio/upload` - Upload audio file
- `GET /api/audio/download/{id}` - Stream audio file
- `GET /api/audio/search` - Search tracks by title/artist
- `DELETE /api/audio/{id}` - Delete audio file (owner only)

### Playlists
- `GET /api/playlists` - List all playlists
- `POST /api/playlists` - Create new playlist
- `GET /api/playlists/{id}` - Get playlist details
- `PUT /api/playlists/{id}` - Update playlist
- `DELETE /api/playlists/{id}` - Delete playlist
- `POST /api/playlists/{id}/tracks` - Add track to playlist
- `DELETE /api/playlists/{playlistId}/tracks/{trackId}` - Remove track

### Ratings & Favorites
- `POST /api/ratings` - Rate a track
- `GET /api/ratings/track/{trackId}` - Get track ratings
- `POST /api/favorites` - Add to favorites
- `GET /api/favorites/user/{userId}` - Get user favorites

---

## 🧪 Testing

Harmonia includes comprehensive test coverage:

- **100+ total tests** across unit, integration, and E2E layers
- **Service layer**: 100% coverage with unit tests
- **Controller layer**: Full integration testing with MockMvc
- **E2E workflows**: Complete user journey validation
- **Security**: Authentication and authorization tests
- **Concurrency**: Multi-user simulation (10+ simultaneous users)
- **Database**: PostgreSQL compatibility via Testcontainers
- **Performance**: Sub-200ms response times for API endpoints

---

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork** the repository
2. **Create a feature branch**: `git checkout -b feature/your-feature-name`
3. **Commit your changes**: `git commit -m 'Add some feature'`
4. **Push to the branch**: `git push origin feature/your-feature-name`
5. **Open a Pull Request**

### Code Standards
- Follow existing code style and conventions
- Write tests for new features
- Update documentation as needed
- Ensure all tests pass before submitting PR

---

## 📋 Project Structure

```
harmonia/
├── src/
│   ├── main/
│   │   ├── java/edu/cqu/coit13235/musicchat/
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── service/         # Business logic
│   │   │   ├── domain/          # Entity models
│   │   │   ├── repository/      # Data access
│   │   │   ├── security/        # Authentication
│   │   │   └── websocket/       # WebSocket config
│   │   └── resources/
│   │       ├── application.properties
│   │       └── templates/       # Thymeleaf views
│   └── test/
│       └── java/                # Test suites
├── design/                      # Architecture diagrams
├── pom.xml                      # Maven configuration
└── README.md
```

---

## 🔮 Roadmap

### Current Features ✅
- Real-time WebSocket chat
- Audio upload and streaming
- Playlist creation and management
- User authentication and authorization
- PostgreSQL compatibility
- Comprehensive test coverage

### Planned Enhancements 🚧
- Message reactions and threading
- Push notifications
- Collaborative playlists (multi-user editing)
- Audio metadata extraction and waveform generation
- Cloud storage integration (AWS S3)
- Full-text search with Elasticsearch
- Usage analytics and recommendation engine
- Redis caching for performance
- API versioning and rate limiting
- CI/CD pipeline

---

## 📄 License

This project is licensed under the **MIT License**.

```
MIT License

Copyright (c) 2025 Harmonia

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 📞 Contact

For questions, suggestions, or collaboration opportunities, please open an issue or reach out via GitHub.

---

**Built with ❤️ using Spring Boot, WebSockets, and modern web technologies.**

#   H a r m o n i a  
 
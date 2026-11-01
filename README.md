
# FrameHost

### One Host. Every Screen. Total Control.

FrameHost is an open-source, multi-device video synchronization and remote playback control platform.

It allows users to synchronize video playback across multiple devices and control playback from any authorized connected device.

## Project Vision

Build a platform where one device hosts a video session, multiple devices join as viewers, and authorized devices can remotely control playback.

Our initial focus is YouTube synchronization, followed by local video support and offline local-network playback.

## Key Features

### Planned Features

- Multi-device video synchronization
- Host, Controller, and Follower roles
- Real-time playback synchronization
- Remote play and pause
- Remote seeking and playback-speed control
- Room creation and joining
- Device management and permissions
- Automatic reconnection and state recovery
- YouTube video integration
- Future local video and offline support

> Features are planned and will be implemented incrementally. They are not yet available in the initial repository.

## Technology Stack

### Frontend
- React
- TypeScript
- Vite
- CSS
- YouTube IFrame Player API

### Backend
- Java 21
- Spring Boot
- Spring Web
- Spring WebSocket
- Spring Validation
- Spring Boot Actuator

### Development Tools
- Visual Studio Code
- Maven
- Git and GitHub
- Postman
- JUnit 5
- Vitest
- React Testing Library

## Architecture

FrameHost follows a frontend-backend architecture.

```text
           FrameHost
               |
       +-------+-------+
       |               |
    Frontend         Backend
    React            Spring Boot
       |               |
       +------ WebSocket
               |
        Authoritative
         Room State
               |
       +-------+-------+
       |       |       |
      Host  Controller Follower
```

## Project Structure

```text
FrameHost/
├── frontend/
├── backend/
├── docs/
│   ├── ARCHITECTURE.md
│   ├── PROJECT_STATUS.md
│   └── DECISIONS.md
├── .github/
├── .gitignore
├── LICENSE
└── README.md
```

## Development Roadmap

- [ ] Phase 0: Project initialization
- [ ] Phase 1: YouTube player
- [ ] Phase 2: Backend and room management
- [ ] Phase 3: Host and follower synchronization
- [ ] Phase 4: Multi-device remote control
- [ ] Phase 5: Multi-device reliability
- [ ] Phase 6: Local video support
- [ ] Phase 7: Offline mode
- [ ] Phase 8: Production hardening

## Development Philosophy

- Clean and maintainable code
- Security-first architecture
- Real-time communication
- Minimal dependencies
- Zero mandatory paid infrastructure
- Incremental development and testing

## Getting Started

Development setup instructions will be added as the frontend and backend implementations are initialized.

## License

This project is licensed under the Apache License 2.0.

See the [LICENSE](LICENSE) file for details.

---

**FrameHost — One Host. Every Screen. Total Control.**

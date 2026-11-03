# FrameHost

### One Host. Every Screen. Total Control.

FrameHost is an open-source, multi-device video synchronization and remote playback control platform.

It allows users to create a shared video room, connect multiple devices, and synchronize YouTube playback across authorized devices in real time.

---

## Project Overview

FrameHost enables one device to act as the **Host**, while other devices join the same room as **Controllers** or **Followers**.

The Host manages the room and controls which connected devices are allowed to perform playback actions.

Our initial focus is synchronized YouTube playback. Future versions aim to support local video playback and offline synchronization over a local network.

## Current Features

The following functionality has been implemented in the current development branch:

- [x] Room creation and joining
- [x] Host, Controller, and Follower roles
- [x] Connected device management
- [x] Host-controlled room lifecycle
- [x] Device authentication using access tokens
- [x] Real-time playback communication using WebSocket and STOMP
- [x] YouTube IFrame Player API integration
- [x] Remote Play and Pause
- [x] Remote Seek
- [x] Change YouTube video
- [x] Individual playback permissions for connected devices
- [x] Host-only permission management
- [x] Frontend and backend integration

## Planned Features

These capabilities are part of the roadmap and are not yet considered complete:

- [ ] Playback-speed controls
- [ ] Automatic reconnection and playback state recovery
- [ ] Initial playback-state synchronization for newly joined devices
- [ ] Improved synchronization and drift correction
- [ ] More comprehensive integration and end-to-end testing
- [ ] Local video playback
- [ ] Offline synchronization over a local network
- [ ] Production deployment and monitoring

## Technology Stack

### Frontend

- React 19
- TypeScript
- Vite
- Tailwind CSS 4
- STOMP.js
- YouTube IFrame Player API

### Backend

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring WebSocket
- Spring Validation
- Spring Boot Actuator
- Maven

### Testing and Development Tools

- JUnit 5
- Maven
- ESLint
- Vitest and React Testing Library (planned for frontend test coverage)
- Postman
- Visual Studio Code
- Git and GitHub

## Architecture

FrameHost follows a frontend-backend architecture.

```text
                 FrameHost
                     |
           +---------+---------+
           |                   |
        Frontend             Backend
        React                Spring Boot
           |                   |
           +---- WebSocket ----+
                     |
              Room Playback
                   State
                     |
          +----------+----------+
          |          |          |
         Host    Controller  Follower
```

The backend maintains authoritative room and playback state. Connected devices communicate with the backend through authenticated WebSocket connections.

The Host controls room-level operations and manages playback permissions for other devices.

## Project Structure

```text
FrameHost/
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── services/
│   │   └── types/
│   ├── package.json
│   └── .env.example
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   ├── pom.xml
│   └── mvnw
│
├── docs/
├── .github/
├── .gitignore
├── LICENSE
└── README.md
```

## Getting Started

### Prerequisites

Install the following tools:

- Java 21
- Node.js and npm
- Git
- A modern web browser

### 1. Clone the repository

```bash
git clone https://github.com/vivekkushwahaofficial/FrameHost.git

cd FrameHost
```

### 2. Start the backend

Open a terminal:

```bash
cd backend
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

On Linux or macOS:

```bash
./mvnw spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

### 3. Start the frontend

Open another terminal from the project root:

```bash
cd frontend
npm install
```

Create a `.env` file using `.env.example` as a reference.

The local WebSocket configuration is:

```env
VITE_WS_URL=ws://localhost:8080/ws
```

Start the development server:

```bash
npm run dev
```

Open:

```text
http://localhost:5173
```

### 4. Validate the frontend

```bash
npm run build
npm run lint
```

### 5. Validate the backend

From the backend directory:

```bash
mvnw.cmd clean verify
```

## Development Roadmap

| Phase | Description                                 | Status                          |
| ----- | ------------------------------------------- | ------------------------------- |
| 0     | Project initialization                      | Complete                        |
| 1     | YouTube player integration                  | Complete                        |
| 2     | Backend and room management                 | Complete                        |
| 3     | Host and Follower synchronization           | Initial implementation complete |
| 4     | Multi-device remote control and permissions | Initial implementation complete |
| 5     | Multi-device reliability and recovery       | In progress                     |
| 6     | Local video support                         | Planned                         |
| 7     | Offline local-network mode                  | Planned                         |
| 8     | Production hardening                        | Planned                         |

## Development Philosophy

- Clean, maintainable, and readable code
- Security-first architecture
- Reliable real-time communication
- Minimal dependencies
- Zero mandatory paid infrastructure
- Incremental development and testing
- No unnecessary rewrites of working functionality

## Contribution

Contributions, suggestions, and bug reports are welcome.

Please open an issue before starting significant changes. Feature branches should be created from `develop`, and changes should be submitted through pull requests.

## License

FrameHost is licensed under the Apache License 2.0.

See the [LICENSE](LICENSE) file for details.

---

**FrameHost — One Host. Every Screen. Total Control.**

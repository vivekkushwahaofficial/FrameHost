# FrameHost – System Architecture

## 1. Overview

FrameHost is an open-source, multi-device video synchronization and remote playback control platform.

It uses a React and TypeScript frontend with a Java and Spring Boot backend. REST APIs handle room and device management, while WebSocket with STOMP handles real-time playback communication.

The backend maintains the authoritative playback state for each active room.

The initial video provider is YouTube, integrated through the official YouTube IFrame Player API.

## 2. High-Level Architecture

```text
                   FrameHost
                       |
             +---------+---------+
             |                   |
          Frontend             Backend
          React                Spring Boot
          TypeScript           Java 21
             |                   |
             |---- REST API -----|
             |                   |
             |---- WebSocket ----|
                    STOMP
                       |
              Room Playback State
                       |
             +---------+---------+
             |         |         |
            Host   Controller  Follower
```

All connected devices communicate with the backend.

The backend validates incoming playback commands, updates room state, and broadcasts accepted playback updates to devices subscribed to that room.

## 3. Frontend Architecture

### Technology

- React
- TypeScript
- Vite
- Tailwind CSS
- STOMP.js
- YouTube IFrame Player API

### Responsibilities

- Room creation and joining.
- Device session management.
- Host, Controller, and Follower interfaces.
- YouTube player initialization and control.
- Sending local playback events.
- Receiving remote playback updates.
- Managing authenticated WebSocket connections.
- Tracking connection status.
- Applying remote playback state to the player.

### Current Source Structure

```text
frontend/src/
├── components/
│   ├── landing/
│   ├── layout/
│   ├── player/
│   └── room/
├── hooks/
├── services/
│   ├── api/
│   ├── websocket/
│   └── youtube/
├── types/
├── App.tsx
└── main.tsx
```

The frontend uses React hooks to manage player behavior and WebSocket synchronization.

The STOMP client reconnects after temporary connection failures. Complete playback-state recovery after reconnection remains future work.

## 4. Backend Architecture

### Technology

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring WebSocket
- Spring Validation
- Spring Boot Actuator
- Maven

### Responsibilities

- Room creation and lifecycle management.
- Device registration and authentication.
- Device membership management.
- Host authorization.
- Playback permission management.
- WebSocket connection authentication.
- Playback command validation.
- Authoritative playback state management.
- State version generation.
- Broadcasting accepted playback updates.

### Current Source Structure

```text
backend/src/main/java/com/framehost/
├── common/
├── config/
├── device/
├── playback/
├── room/
└── websocket/
```

The backend currently uses in-memory storage. Persistent database integration is not implemented.

## 5. Device Roles and Permissions

FrameHost defines three device roles.

### Host

- Creates and ends rooms.
- Has full playback control.
- Views connected devices.
- Removes connected devices.
- Grants and revokes playback permissions.
- Retains authority over room management.

### Controller

- A defined device role in the backend.
- Can perform playback actions when the required permission is granted.
- Cannot perform Host-only room management operations.

Automatic Controller assignment or role promotion is not currently implemented.

### Follower

- Joins an existing room.
- Receives room playback updates.
- Can perform playback actions when the Host grants the corresponding permissions.

### Playback Permissions

The backend currently supports:

| Permission   | Allowed actions          |
| ------------ | ------------------------ |
| PLAY_PAUSE   | Play and pause           |
| SEEK         | Change playback position |
| CHANGE_VIDEO | Select another video     |

The Host always has full playback control. Non-Host devices must have the permission required for each command.

## 6. REST API Communication

REST APIs handle room and device management.

Current endpoints:

| Method | Endpoint                                                           | Purpose                     |
| ------ | ------------------------------------------------------------------ | --------------------------- |
| POST   | `/api/v1/rooms`                                                    | Create a room               |
| POST   | `/api/v1/rooms/{roomCode}/join`                                    | Join a room                 |
| GET    | `/api/v1/rooms/{roomCode}/devices`                                 | Get connected devices       |
| POST   | `/api/v1/rooms/{roomCode}/leave`                                   | Leave a room                |
| DELETE | `/api/v1/rooms/{roomCode}/devices/{deviceId}`                      | Remove a device             |
| DELETE | `/api/v1/rooms/{roomCode}`                                         | End a room                  |
| PUT    | `/api/v1/rooms/{roomCode}/devices/{deviceId}/playback-permissions` | Update playback permissions |

Room and device operations use device authentication tokens where required.

## 7. Real-Time Communication

FrameHost uses WebSocket with STOMP for real-time playback communication.

### WebSocket Configuration

- WebSocket endpoint: `/ws`
- Application destination prefix: `/app`
- Message broker prefix: `/topic`

### Current Destinations

Client sends playback commands to:

```text
/app/rooms/{roomCode}/playback
```

Clients subscribe to playback updates at:

```text
/topic/rooms/{roomCode}/playback
```

### Connection Authentication

During the STOMP CONNECT frame, the frontend provides:

- `roomCode`
- `Authorization: Bearer <deviceToken>`

The backend authenticates the device and associates its identity with the WebSocket session.

The backend also restricts subscriptions and messages to the authenticated device's room.

### Implemented Playback Commands

- PLAY
- PAUSE
- SEEK
- CHANGE_VIDEO

Playback-speed information can be included in supported command payloads, but a separate `CHANGE_PLAYBACK_RATE` command is not implemented.

Room lifecycle and device management currently use REST APIs rather than the playback STOMP channel.

## 8. Authoritative Playback State

The backend stores an immutable playback state snapshot for each room.

Current state fields:

| Field          | Description                    |
| -------------- | ------------------------------ |
| videoProvider  | Video provider                 |
| videoId        | Selected video identifier      |
| playbackStatus | PLAYING or PAUSED              |
| currentTime    | Playback position in seconds   |
| playbackRate   | Playback speed                 |
| stateVersion   | Server-generated state version |
| lastUpdatedAt  | Server update timestamp        |

The state version starts at 1 and increments whenever the backend accepts a playback-state update.

The backend broadcasts the updated state to the room's playback topic.

The current state model does not contain `roomId` or `hostDeviceId`; room association is handled separately.

## 9. Playback Synchronization Flow

The current playback command flow:

1. A connected device generates a playback command.
2. The frontend sends it through STOMP.
3. The backend authenticates the WebSocket session.
4. The backend verifies room membership.
5. The backend checks the device's current role.
6. The backend validates the command payload.
7. The backend checks the required playback permission.
8. The backend updates the authoritative room state.
9. The backend increments the state version.
10. The backend broadcasts the updated state.
11. Connected frontend players receive the update.
12. Players apply the remote playback state.

The frontend ignores playback updates with invalid or outdated state versions.

## 10. Conflict Resolution

Multiple authorized devices may send commands close together.

The backend serializes command validation and state updates using synchronization on the room object.

State versions are generated by the backend, not supplied by clients.

This provides server-side ordering for accepted state updates.

The current implementation does not yet provide a complete stale-command rejection protocol or distributed ordering across multiple backend instances.

## 11. Storage Strategy

### Current Implementation

- In-memory room repository.
- In-memory device registration.
- In-memory authentication token hashes.
- In-memory playback permissions.
- In-memory playback state.

Room capacity is currently limited to 10 devices, including the Host.

Device authentication tokens are stored as hashes rather than raw tokens.

When a room ends, its device token hashes, playback permissions, and playback state are cleared.

### Future Improvements

- Persistent database integration.
- Distributed room state.
- Multi-instance WebSocket messaging.
- Room expiration and cleanup.
- Persistent user accounts, if required.

## 12. YouTube Integration

YouTube is the first supported video provider.

FrameHost uses the official YouTube IFrame Player API.

The frontend controls the embedded YouTube player.

The backend coordinates playback commands and authoritative playback state.

FrameHost does not proxy, download, or redistribute YouTube video streams.

YouTube playback requires an internet connection and is subject to YouTube's platform requirements.

## 13. Reliability and Recovery

### Currently Implemented

- STOMP automatic reconnect configuration.
- WebSocket heartbeat configuration.
- Connection status tracking.
- Playback state version validation.
- Cleanup of WebSocket connections when the room session changes.

### Planned Improvements

- Initial playback-state delivery to newly connected devices.
- Playback-state recovery after reconnection.
- Better handling of temporary network failures.
- Periodic synchronization and drift correction.
- Integration tests for multiple simultaneous devices.

Automatic reconnection is configured, but reconnecting alone does not guarantee that a device receives the latest room state.

## 14. Security Considerations

Current security controls include:

- Device authentication tokens.
- Hashed token storage.
- Authenticated WebSocket connections.
- Room-specific WebSocket subscriptions.
- Room-specific playback message destinations.
- Host-only room management operations.
- Host-controlled playback permissions.
- Server-side command validation.
- Device membership and role verification.

Further production hardening is required, including deployment-specific origin configuration, secure transport, rate limiting, and broader security testing.

## 15. Future Architecture

Future development may introduce:

- Local video playback.
- Authorized local media access.
- Progressive Web App support.
- Local-network synchronization.
- Offline playback for locally available videos.
- Persistent storage.
- Scalable real-time messaging.

These capabilities are future milestones and are not part of the current implementation.

---

**FrameHost — One Host. Every Screen. Total Control.**

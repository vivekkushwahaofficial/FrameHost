# FrameHost – System Architecture

## 1. Overview

FrameHost is a multi-device video synchronization and remote-control platform.

The system uses a frontend-backend architecture with real-time communication through WebSocket.

The backend maintains the authoritative playback state for every room.

## 2. High-Level Architecture

```text
+-------------------+
|    Host Device    |
|   React + TS      |
+---------+---------+
          |
          |
+---------v---------+
|                   |
|   Spring Boot     |
|     Backend       |
|                   |
|  Room Management  |
|  Device Management|
|  Authorization   |
|  Playback State   |
|                   |
+---------+---------+
          |
      WebSocket
          |
   +------+------+
   |      |      |
+--v--+ +-v----+ +--v-----+
|Host | |Phone | | Tablet |
+-----+ +------+ +--------+
```

## 3. Frontend Architecture

Technology:

- React
- TypeScript
- Vite
- CSS
- YouTube IFrame Player API

Responsibilities:

- Room creation and joining.
- Device identity.
- Host, Controller and Follower interfaces.
- Video player initialization.
- Playback controls.
- WebSocket connection management.
- Synchronization and drift correction.
- Reconnection handling.

Suggested structure:

```text
frontend/src/
├── components/
├── features/
│   ├── player/
│   ├── rooms/
│   ├── devices/
│   └── permissions/
├── hooks/
├── services/
├── types/
├── utils/
└── App.tsx
```

## 4. Backend Architecture

Technology:

- Java 21
- Spring Boot
- Spring Web
- Spring WebSocket
- Spring Validation
- Spring Boot Actuator

Responsibilities:

- Room management.
- Device registration.
- Host identification.
- Controller authorization.
- Follower management.
- WebSocket communication.
- Playback command validation.
- Authoritative playback state.
- State versioning.
- Reconnection handling.
- Room cleanup.

Suggested structure:

```text
backend/src/main/java/com/framehost/
├── config/
├── room/
├── device/
├── permission/
├── playback/
├── websocket/
├── security/
└── common/
```

## 5. Device Roles

### Host

- Creates and closes rooms.
- Selects videos.
- Controls playback.
- Manages connected devices.
- Grants and revokes permissions.
- Retains ultimate room authority.

### Controller

- Controls playback when authorized.
- Can request permitted video changes.
- Cannot override host-only settings.

### Follower

- Joins an existing room.
- Watches synchronized playback.
- Follows authoritative playback updates.
- Can request controller access.

## 6. Real-Time Communication

WebSocket provides bidirectional communication between connected devices and the backend.

The backend validates every command before updating the room state.

Main message categories:

### Room Events

- CREATE_ROOM
- JOIN_ROOM
- LEAVE_ROOM
- ROOM_STATE
- ROOM_CLOSED

### Playback Commands

- PLAY
- PAUSE
- SEEK
- CHANGE_VIDEO
- CHANGE_PLAYBACK_RATE

### Device and Permission Events

- DEVICE_CONNECTED
- DEVICE_DISCONNECTED
- ROLE_UPDATED
- PERMISSION_GRANTED
- PERMISSION_REVOKED
- CONTROL_REQUESTED

### Synchronization Events

- SYNC_STATE
- HEARTBEAT
- STATE_CORRECTION
- RECONNECT

## 7. Authoritative Playback State

The backend maintains the latest playback state for each room.

State fields:

- roomId
- videoProvider
- videoId
- playbackStatus
- currentTime
- playbackRate
- hostDeviceId
- lastUpdatedAt
- stateVersion

Every accepted command updates the state version.

Connected devices receive the latest accepted state.

## 8. Synchronization Strategy

1. A device sends a playback command.
2. Backend identifies the sender.
3. Backend validates room membership.
4. Backend checks role and permissions.
5. Backend validates the command payload.
6. Backend rejects invalid or outdated commands.
7. Backend updates authoritative state.
8. Backend increments stateVersion.
9. Backend broadcasts the accepted state.
10. Connected players synchronize.

Small timing differences should be corrected without unnecessary seeking.

## 9. Conflict Resolution

Multiple controllers may send commands close together.

FrameHost uses server-side command ordering and state versioning.

Client timestamps are not trusted as the source of command order.

The host retains authority over room settings and device permissions.

## 10. Storage Strategy

Initial MVP:

- In-memory room storage.
- In-memory device and permission management.
- No external database dependency.

A database can be introduced when persistent rooms, users or playback history become necessary.

## 11. Video Integration

YouTube is the first supported video provider.

Integration uses the official YouTube IFrame Player API.

The backend coordinates playback state and commands.

It does not proxy, download or redistribute YouTube video streams.

YouTube playback requires an internet connection.

## 12. Future Architecture

After the YouTube MVP:

- Local video playback.
- Authorized local media access.
- Progressive Web App support.
- Local-network synchronization.
- Offline playback for locally available videos.

These capabilities are future milestones, not part of the initial implementation.

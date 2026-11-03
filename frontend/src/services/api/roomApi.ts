import type {
  CreateRoomResponse,
  JoinRoomRequest,
  JoinRoomResponse,
  DeviceResponse,
} from "../../types/room";

import type {
  PlaybackPermission,
  PlaybackPermissionResponse,
} from "../../types/playbackPermissions";
const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const responseText = await response.text();

    let message = responseText || `Request failed (${response.status})`;

    try {
      const errorBody = JSON.parse(responseText) as {
        message?: string;
        detail?: string;
      };

      message = errorBody.message || errorBody.detail || message;
    } catch {
      // Keep the original response text if it is not JSON.
    }

    throw new Error(message);
  }

  return response.json() as Promise<T>;
}

export async function createRoom(): Promise<CreateRoomResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/rooms`, {
    method: "POST",
  });

  return handleResponse<CreateRoomResponse>(response);
}

export async function joinRoom(
  roomCode: string,
  request: JoinRoomRequest,
): Promise<JoinRoomResponse> {
  const normalizedRoomCode = roomCode.trim().toUpperCase();

  const response = await fetch(
    `${API_BASE_URL}/api/v1/rooms/${encodeURIComponent(normalizedRoomCode)}/join`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
    },
  );

  return handleResponse<JoinRoomResponse>(response);
}
export async function leaveRoom(
  roomCode: string,
  deviceToken: string,
): Promise<void> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/rooms/${encodeURIComponent(roomCode)}/leave`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${deviceToken}`,
      },
    },
  );

  await handleResponse<unknown>(response);
}

export async function endRoom(
  roomCode: string,
  deviceToken: string,
): Promise<void> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/rooms/${encodeURIComponent(roomCode)}`,
    {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${deviceToken}`,
      },
    },
  );

  await handleResponse<unknown>(response);
}

export async function getRoomDevices(
  roomCode: string,
  deviceToken: string,
): Promise<DeviceResponse[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/rooms/${encodeURIComponent(roomCode)}/devices`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${deviceToken}`,
      },
    },
  );

  return handleResponse<DeviceResponse[]>(response);
}

// Update playback permissions for a specific Follower.
export async function updatePlaybackPermissions(
  roomCode: string,
  deviceId: string,
  deviceToken: string,
  permissions: PlaybackPermission[],
): Promise<PlaybackPermissionResponse> {
  const normalizedRoomCode = roomCode.trim().toUpperCase();

  const response = await fetch(
    `${API_BASE_URL}/api/v1/rooms/${encodeURIComponent(normalizedRoomCode)}/devices/${encodeURIComponent(deviceId)}/playback-permissions`,
    {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${deviceToken}`,
      },
      body: JSON.stringify({ permissions }),
    },
  );

  return handleResponse<PlaybackPermissionResponse>(response);
}

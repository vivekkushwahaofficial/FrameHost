// Available playback permissions supported by the backend.
export type PlaybackPermission = "PLAY_PAUSE" | "SEEK" | "CHANGE_VIDEO";

// Request body for updating a Follower's permissions.
export interface PlaybackPermissionRequest {
  permissions: PlaybackPermission[];
}

// Response returned by the backend.
export interface PlaybackPermissionResponse {
  roomCode: string;
  deviceId: string;
  permissions: PlaybackPermission[];
  message: string;
}

import type { PlaybackPermission } from "./playbackPermissions";
export type DeviceRole = "HOST" | "CONTROLLER" | "FOLLOWER";

export interface CreateRoomResponse {
  roomCode: string;
  hostDeviceId: string;
  deviceToken: string;
  role: DeviceRole;
  createdAt: string;
  deviceCount: number;
  maxDevices: number;
}

export interface JoinRoomRequest {
  deviceName: string;
}

export interface JoinRoomResponse {
  roomCode: string;
  deviceId: string;
  deviceName: string;
  deviceToken: string;
  role: DeviceRole;
  joinedAt: string;
  deviceCount: number;
  maxDevices: number;
}

export interface DeviceResponse {
  deviceId: string;
  deviceName: string;
  role: DeviceRole;
  joinedAt: string;
  permissions: PlaybackPermission[];
}

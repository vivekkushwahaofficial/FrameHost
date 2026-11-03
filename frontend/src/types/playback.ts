export type PlaybackCommandType = "PLAY" | "PAUSE" | "SEEK" | "CHANGE_VIDEO";

export type PlaybackStatus = "PLAYING" | "PAUSED";

export interface LocalPlaybackEvent {
  command: PlaybackCommandType;
  videoId?: string;
  currentTime?: number;
  playbackRate?: number;
}

export interface PlaybackState {
  videoProvider: "YOUTUBE";
  videoId: string;
  playbackStatus: PlaybackStatus;
  currentTime: number;
  playbackRate: number;
  stateVersion: number;
  lastUpdatedAt: string;
}

export type PlaybackEventHandler = (event: LocalPlaybackEvent) => void;

import { Client, type IMessage } from "@stomp/stompjs";

import type { LocalPlaybackEvent, PlaybackState } from "../../types/playback";

interface PlaybackConnectionConfig {
  roomCode: string;
  deviceToken: string;
  onPlaybackState: (state: PlaybackState) => void;
  onConnected?: () => void;
  onDisconnected?: () => void;
  onError?: (message: string) => void;
}

interface PlaybackCommandMessage {
  command: LocalPlaybackEvent["command"];
  videoProvider?: "YOUTUBE";
  videoId?: string;
  currentTime?: number;
  playbackRate?: number;
}

export interface PlaybackStompClient {
  disconnect: () => Promise<void>;
  sendCommand: (event: LocalPlaybackEvent) => boolean;
  isConnected: () => boolean;
}

export function createPlaybackStompClient(
  config: PlaybackConnectionConfig,
): PlaybackStompClient {
  const roomCode = config.roomCode.trim().toUpperCase();

  const client = new Client({
    brokerURL: import.meta.env.VITE_WS_URL,

    connectHeaders: {
      roomCode,
      Authorization: `Bearer ${config.deviceToken}`,
    },

    // Retry connection after temporary network failures.
    reconnectDelay: 5000,

    // Enable connection health checks.
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,

    onConnect: () => {
      // Subscribe only after successful authentication.
      client.subscribe(
        `/topic/rooms/${roomCode}/playback`,
        (message: IMessage) => {
          try {
            const state = JSON.parse(message.body) as PlaybackState;

            config.onPlaybackState(state);
          } catch {
            config.onError?.("Received an invalid playback state.");
          }
        },
      );

      config.onConnected?.();
    },

    onDisconnect: () => {
      config.onDisconnected?.();
    },

    onWebSocketClose: () => {
      config.onDisconnected?.();
    },

    onStompError: (frame) => {
      config.onError?.(frame.headers.message || "STOMP connection error.");
    },

    onWebSocketError: () => {
      config.onError?.("Unable to connect to the FrameHost server.");
    },
  });

  // Start the WebSocket connection.
  client.activate();

  // Send a playback command to Spring Boot.
  function sendCommand(event: LocalPlaybackEvent): boolean {
    if (!client.connected) {
      config.onError?.("Cannot send playback command: not connected.");

      return false;
    }

    const command: PlaybackCommandMessage = {
      ...event,
    };

    // The backend requires a provider when changing videos.
    if (event.command === "CHANGE_VIDEO") {
      command.videoProvider = "YOUTUBE";
    }

    client.publish({
      destination: `/app/rooms/${roomCode}/playback`,
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify(command),
    });

    return true;
  }

  return {
    disconnect: async () => {
      await client.deactivate();
    },

    sendCommand,

    isConnected: () => client.connected,
  };
}

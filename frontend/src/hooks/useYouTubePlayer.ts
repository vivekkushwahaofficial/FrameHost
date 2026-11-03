import { useCallback, useEffect, useRef, useState } from "react";

import type { RefObject } from "react";

import { loadYouTubeApi } from "../services/youtube/youtubeApi";

import type {
  LocalPlaybackEvent,
  PlaybackEventHandler,
  PlaybackState,
} from "../types/playback";

const PLAYER_STATE = {
  UNSTARTED: -1,
  ENDED: 0,
  PLAYING: 1,
  PAUSED: 2,
  BUFFERING: 3,
  CUED: 5,
} as const;

const REMOTE_EVENT_TIMEOUT = 2000;
const PLAYBACK_UPDATE_INTERVAL = 500;
const SEEK_DRIFT_THRESHOLD = 1.5;

interface UseYouTubePlayerReturn {
  containerRef: RefObject<HTMLDivElement | null>;
  isReady: boolean;
  playerState: number;
  currentTime: number;
  duration: number;
  error: string | null;
  play: () => void;
  pause: () => void;
  loadVideo: (videoId: string) => void;
  seekTo: (seconds: number) => void;
  applyRemoteState: (state: PlaybackState) => void;
}

interface ExpectedRemoteEvent {
  state: number;
  timeoutId: number;
}

// Convert YouTube error codes into understandable messages.
function getYouTubeErrorMessage(errorCode: number): string {
  switch (errorCode) {
    case 2:
      return "Invalid YouTube video ID or video parameter.";

    case 5:
      return "The YouTube HTML5 player encountered an error.";

    case 100:
      return "This video is unavailable or has been removed.";

    case 101:
    case 150:
      return "The video owner does not allow playback on embedded players.";

    case 153:
      return "YouTube could not verify the player request. Check the browser and embed configuration.";

    default:
      return `YouTube player error: ${errorCode}`;
  }
}

function isValidPlaybackState(state: PlaybackState): boolean {
  return (
    state.videoProvider === "YOUTUBE" &&
    /^[a-zA-Z0-9_-]{11}$/.test(state.videoId) &&
    (state.playbackStatus === "PLAYING" || state.playbackStatus === "PAUSED") &&
    Number.isFinite(state.currentTime) &&
    state.currentTime >= 0 &&
    Number.isFinite(state.playbackRate) &&
    state.playbackRate > 0 &&
    Number.isInteger(state.stateVersion) &&
    state.stateVersion > 0
  );
}

export function useYouTubePlayer(
  onLocalPlaybackEvent?: PlaybackEventHandler,
): UseYouTubePlayerReturn {
  const containerRef = useRef<HTMLDivElement>(null);

  const playerRef = useRef<YouTubePlayer | null>(null);

  // Keep the latest callback without recreating the player.
  const callbackRef = useRef(onLocalPlaybackEvent);

  // Track the expected state change caused by a remote command.
  const remoteExpectedStateRef = useRef<ExpectedRemoteEvent | null>(null);

  // Retain remote state until the YouTube player is ready.
  const pendingRemoteStateRef = useRef<PlaybackState | null>(null);

  // Track the latest server state version.
  const lastRemoteVersionRef = useRef(0);

  // Track the currently loaded video.
  const currentVideoIdRef = useRef<string | null>(null);

  const [isReady, setIsReady] = useState(false);

  const [playerState, setPlayerState] = useState<number>(
    PLAYER_STATE.UNSTARTED,
  );

  const [currentTime, setCurrentTime] = useState(0);

  const [duration, setDuration] = useState(0);

  const [error, setError] = useState<string | null>(null);

  // Keep the latest event callback.
  useEffect(() => {
    callbackRef.current = onLocalPlaybackEvent;
  }, [onLocalPlaybackEvent]);

  // Send local playback events to the synchronization layer.
  const emitLocalEvent = useCallback((event: LocalPlaybackEvent) => {
    callbackRef.current?.(event);
  }, []);

  // Clear any pending remote event suppression.
  const clearExpectedRemoteEvent = useCallback(() => {
    const expectedEvent = remoteExpectedStateRef.current;

    if (expectedEvent) {
      window.clearTimeout(expectedEvent.timeoutId);
      remoteExpectedStateRef.current = null;
    }
  }, []);

  // Mark a player state change as originating from a remote command.
  const expectRemoteEvent = useCallback(
    (expectedState: number) => {
      clearExpectedRemoteEvent();

      const timeoutId = window.setTimeout(() => {
        remoteExpectedStateRef.current = null;
      }, REMOTE_EVENT_TIMEOUT);

      remoteExpectedStateRef.current = {
        state: expectedState,
        timeoutId,
      };
    },
    [clearExpectedRemoteEvent],
  );

  // Apply a validated authoritative state to the YouTube player.
  const applyRemoteState = useCallback(
    (state: PlaybackState) => {
      if (!isValidPlaybackState(state)) {
        return;
      }

      // Keep only the newest pending state while the player initializes.
      const player = playerRef.current;

      if (!player) {
        const pendingState = pendingRemoteStateRef.current;

        if (!pendingState || state.stateVersion > pendingState.stateVersion) {
          pendingRemoteStateRef.current = state;
        }

        return;
      }

      // Reject stale or duplicate server updates.
      if (state.stateVersion <= lastRemoteVersionRef.current) {
        return;
      }

      lastRemoteVersionRef.current = state.stateVersion;
      pendingRemoteStateRef.current = null;

      const isNewVideo = currentVideoIdRef.current !== state.videoId;

      currentVideoIdRef.current = state.videoId;

      setError(null);

      const expectedState =
        state.playbackStatus === "PLAYING"
          ? PLAYER_STATE.PLAYING
          : PLAYER_STATE.PAUSED;

      expectRemoteEvent(expectedState);

      player.setPlaybackRate(state.playbackRate);

      if (isNewVideo) {
        const videoOptions = {
          videoId: state.videoId,
          startSeconds: state.currentTime,
        };

        if (state.playbackStatus === "PLAYING") {
          player.loadVideoById(videoOptions);
        } else {
          player.cueVideoById(videoOptions);
        }

        setCurrentTime(state.currentTime);

        return;
      }

      const currentPosition = player.getCurrentTime();

      // Correct playback drift when the difference is noticeable.
      if (
        Number.isFinite(currentPosition) &&
        Math.abs(currentPosition - state.currentTime) > SEEK_DRIFT_THRESHOLD
      ) {
        player.seekTo(state.currentTime, true);
        setCurrentTime(state.currentTime);
      }

      if (state.playbackStatus === "PLAYING") {
        player.playVideo();
      } else {
        player.pauseVideo();
      }
    },
    [expectRemoteEvent],
  );

  // Initialize the official YouTube IFrame player.
  useEffect(() => {
    let isMounted = true;

    async function initializePlayer() {
      try {
        const youtube = await loadYouTubeApi();

        if (!isMounted || !containerRef.current) {
          return;
        }

        const player = new youtube.Player(containerRef.current, {
          width: "100%",
          height: "100%",

          playerVars: {
            autoplay: 0,
            controls: 1,
            enablejsapi: 1,
            playsinline: 1,
            rel: 0,
            origin: window.location.origin,
          },

          events: {
            onReady: (event) => {
              if (!isMounted) {
                event.target.destroy();
                return;
              }

              playerRef.current = event.target;

              setIsReady(true);
              setError(null);
              setDuration(event.target.getDuration());

              // Apply the latest remote state received during initialization.
              const pendingState = pendingRemoteStateRef.current;

              if (pendingState) {
                applyRemoteState(pendingState);
              }
            },

            onStateChange: (event) => {
              if (!isMounted) {
                return;
              }

              setPlayerState(event.data);

              if (
                event.data === PLAYER_STATE.PLAYING ||
                event.data === PLAYER_STATE.PAUSED
              ) {
                setCurrentTime(event.target.getCurrentTime());
              }

              if (event.data === PLAYER_STATE.ENDED) {
                setCurrentTime(event.target.getDuration());
              }

              // Ignore state changes caused by remote commands.
              const expectedEvent = remoteExpectedStateRef.current;

              if (expectedEvent?.state === event.data) {
                clearExpectedRemoteEvent();
                return;
              }

              // Only report meaningful local playback changes.
              if (event.data === PLAYER_STATE.PLAYING) {
                emitLocalEvent({
                  command: "PLAY",
                  currentTime: event.target.getCurrentTime(),
                  playbackRate: event.target.getPlaybackRate(),
                });
              }

              if (event.data === PLAYER_STATE.PAUSED) {
                emitLocalEvent({
                  command: "PAUSE",
                  currentTime: event.target.getCurrentTime(),
                  playbackRate: event.target.getPlaybackRate(),
                });
              }
            },

            onError: (event) => {
              if (!isMounted) {
                return;
              }

              setError(getYouTubeErrorMessage(event.data));
            },
          },
        });

        playerRef.current = player;
      } catch (initializationError) {
        if (isMounted) {
          setError(
            initializationError instanceof Error
              ? initializationError.message
              : "Unable to initialize YouTube player.",
          );
        }
      }
    }

    void initializePlayer();

    return () => {
      isMounted = false;

      clearExpectedRemoteEvent();

      playerRef.current?.destroy();
      playerRef.current = null;
    };
  }, [applyRemoteState, clearExpectedRemoteEvent, emitLocalEvent]);

  // Keep playback progress updated while the video is playing.
  useEffect(() => {
    if (!isReady || playerState !== PLAYER_STATE.PLAYING) {
      return;
    }

    const intervalId = window.setInterval(() => {
      const player = playerRef.current;

      if (!player) {
        return;
      }

      setCurrentTime(player.getCurrentTime());
      setDuration(player.getDuration());
    }, PLAYBACK_UPDATE_INTERVAL);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [isReady, playerState]);

  // Local Play action.
  const play = useCallback(() => {
    playerRef.current?.playVideo();
  }, []);

  // Local Pause action.
  const pause = useCallback(() => {
    playerRef.current?.pauseVideo();
  }, []);

  // Load a new video locally.
  const loadVideo = useCallback(
    (videoId: string) => {
      const player = playerRef.current;

      if (!player || !/^[a-zA-Z0-9_-]{11}$/.test(videoId)) {
        return;
      }

      clearExpectedRemoteEvent();

      setError(null);
      setCurrentTime(0);
      currentVideoIdRef.current = videoId;

      player.cueVideoById(videoId);

      emitLocalEvent({
        command: "CHANGE_VIDEO",
        videoId,
        currentTime: 0,
      });
    },
    [clearExpectedRemoteEvent, emitLocalEvent],
  );

  // Local Seek action.
  const seekTo = useCallback(
    (seconds: number) => {
      const player = playerRef.current;

      if (!player || !Number.isFinite(seconds)) {
        return;
      }

      const duration = player.getDuration();
      const safePosition =
        duration > 0
          ? Math.max(0, Math.min(seconds, duration))
          : Math.max(0, seconds);

      player.seekTo(safePosition, true);

      setCurrentTime(safePosition);

      emitLocalEvent({
        command: "SEEK",
        currentTime: safePosition,
        playbackRate: player.getPlaybackRate(),
      });
    },
    [emitLocalEvent],
  );

  return {
    containerRef,
    isReady,
    playerState,
    currentTime,
    duration,
    error,
    play,
    pause,
    loadVideo,
    seekTo,
    applyRemoteState,
  };
}

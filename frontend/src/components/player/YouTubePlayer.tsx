import { useEffect, useState, type SyntheticEvent } from "react";

import { useYouTubePlayer } from "../../hooks/useYouTubePlayer";

import type { LocalPlaybackEvent, PlaybackState } from "../../types/playback";

interface YouTubePlayerProps {
  onLocalPlaybackEvent?: (event: LocalPlaybackEvent) => void;
  remoteState?: PlaybackState | null;
  connectionStatus?:
    | "DISCONNECTED"
    | "CONNECTING"
    | "CONNECTED"
    | "RECONNECTING";
  syncError?: string | null;
}

// Extract and validate a YouTube video ID.
function extractVideoId(input: string): string | null {
  const value = input.trim();

  // Accept a direct 11-character video ID.
  if (/^[a-zA-Z0-9_-]{11}$/.test(value)) {
    return value;
  }

  try {
    // Support URLs without an explicitly supplied protocol.
    const normalizedValue = /^https?:\/\//i.test(value)
      ? value
      : `https://${value}`;

    const url = new URL(normalizedValue);
    const hostname = url.hostname.toLowerCase();

    let videoId: string | null = null;

    // Short YouTube URLs: youtu.be/VIDEO_ID
    if (hostname === "youtu.be") {
      videoId = url.pathname.split("/").filter(Boolean)[0] ?? null;
    }

    // Standard YouTube URLs.
    else if (
      [
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
      ].includes(hostname)
    ) {
      // youtube.com/watch?v=VIDEO_ID
      if (url.pathname === "/watch") {
        videoId = url.searchParams.get("v");
      }

      // youtube.com/embed/VIDEO_ID
      // youtube.com/shorts/VIDEO_ID
      // youtube.com/live/VIDEO_ID
      else {
        const pathSegments = url.pathname.split("/").filter(Boolean);

        if (["embed", "shorts", "live"].includes(pathSegments[0] ?? "")) {
          videoId = pathSegments[1] ?? null;
        }
      }
    }

    // Validate the extracted ID before returning it.
    return videoId && /^[a-zA-Z0-9_-]{11}$/.test(videoId) ? videoId : null;
  } catch {
    return null;
  }
}

// Convert seconds into MM:SS format.
function formatTime(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 0) {
    return "00:00";
  }

  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = Math.floor(seconds % 60);

  return `${String(minutes).padStart(2, "0")}:${String(
    remainingSeconds,
  ).padStart(2, "0")}`;
}

function YouTubePlayer({
  onLocalPlaybackEvent,
  remoteState = null,
  connectionStatus = "DISCONNECTED",
  syncError = null,
}: YouTubePlayerProps) {
  const {
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
  } = useYouTubePlayer(onLocalPlaybackEvent);

  const [videoInput, setVideoInput] = useState("");
  const [inputError, setInputError] = useState<string | null>(null);

  // Apply authoritative playback states received from the backend.
  useEffect(() => {
    if (remoteState) {
      applyRemoteState(remoteState);
    }
  }, [remoteState, applyRemoteState]);

  // Handle video loading.
  function handleLoadVideo(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();

    const videoId = extractVideoId(videoInput);

    if (!videoId) {
      setInputError("Enter a valid YouTube video URL or ID.");
      return;
    }

    if (!isReady) {
      setInputError("YouTube player is still initializing.");
      return;
    }

    setInputError(null);
    loadVideo(videoId);
  }

  return (
    <section className="mx-auto w-full max-w-5xl px-4 py-12 sm:px-6">
      <div className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm sm:p-6">
        <div className="mb-5">
          <h2 className="text-xl font-bold text-gray-900">FrameHost Player</h2>

          <p className="mt-1 text-sm text-gray-500">
            Load a YouTube video and control playback.
          </p>
        </div>

        {/* YouTube video input */}
        <form
          onSubmit={handleLoadVideo}
          className="mb-5 flex flex-col gap-3 sm:flex-row"
        >
          <input
            type="text"
            value={videoInput}
            onChange={(event) => {
              setVideoInput(event.target.value);
              setInputError(null);
            }}
            placeholder="Paste YouTube URL or video ID"
            className="min-w-0 flex-1 rounded-lg border border-gray-300 px-4 py-3 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          />

          <button
            type="submit"
            disabled={!isReady}
            className="rounded-lg bg-blue-600 px-6 py-3 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            Load Video
          </button>
        </form>

        {inputError && (
          <p className="mb-4 text-sm text-red-600" role="alert">
            {inputError}
          </p>
        )}

        {/* YouTube player container */}
        <div className="relative aspect-video overflow-hidden rounded-xl bg-black">
          <div ref={containerRef} className="h-full w-full" />

          {!isReady && (
            <div className="absolute inset-0 flex items-center justify-center bg-gray-950 text-sm text-white">
              Initializing YouTube player...
            </div>
          )}
        </div>

        {error && (
          <p className="mt-3 text-sm text-red-600" role="alert">
            {error}
          </p>
        )}

        {/* Playback controls */}
        <div className="mt-5 flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={play}
            disabled={!isReady}
            className="rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
          >
            Play
          </button>

          <button
            type="button"
            onClick={pause}
            disabled={!isReady}
            className="rounded-lg border border-gray-300 px-5 py-2.5 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-50"
          >
            Pause
          </button>

          <span className="text-sm text-gray-500">
            {playerState === 1
              ? "Playing"
              : playerState === 2
                ? "Paused"
                : playerState === 3
                  ? "Buffering"
                  : "Not playing"}
          </span>
        </div>

        {/* WebSocket connection status */}
        <div className="mb-4 mt-3 flex items-center gap-2 text-sm">
          <span
            className={`h-2.5 w-2.5 rounded-full ${
              connectionStatus === "CONNECTED"
                ? "bg-green-500"
                : connectionStatus === "CONNECTING" ||
                    connectionStatus === "RECONNECTING"
                  ? "bg-yellow-500"
                  : "bg-gray-400"
            }`}
          />

          <span className="text-gray-600">
            {connectionStatus === "CONNECTED"
              ? "Connected to room"
              : connectionStatus === "CONNECTING"
                ? "Connecting..."
                : connectionStatus === "RECONNECTING"
                  ? "Reconnecting..."
                  : "Not connected"}
          </span>
        </div>

        {syncError && (
          <p className="mb-4 text-sm text-red-600" role="alert">
            {syncError}
          </p>
        )}

        {/* Playback timeline */}
        <div className="mt-5">
          <input
            type="range"
            min={0}
            max={duration > 0 ? duration : 100}
            step={1}
            value={Math.min(currentTime, duration || 100)}
            disabled={!isReady || duration <= 0}
            onChange={(event) => {
              seekTo(Number(event.target.value));
            }}
            className="w-full cursor-pointer accent-blue-600 disabled:cursor-not-allowed"
            aria-label="Video playback position"
          />

          <div className="flex justify-between text-xs text-gray-500">
            <span>{formatTime(currentTime)}</span>
            <span>{formatTime(duration)}</span>
          </div>
        </div>
      </div>
    </section>
  );
}

export default YouTubePlayer;

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

  // Playback status presentation.
  const playbackStatus =
    playerState === 1
      ? "Playing"
      : playerState === 2
        ? "Paused"
        : playerState === 3
          ? "Buffering"
          : "Not playing";

  const isConnected = connectionStatus === "CONNECTED";

  const isConnecting =
    connectionStatus === "CONNECTING" || connectionStatus === "RECONNECTING";

  return (
    <section className="mx-auto w-full max-w-5xl px-4 py-10 transition-colors duration-300 sm:px-6 sm:py-12">
      <div className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-[#111C30] dark:shadow-xl dark:shadow-black/10 sm:p-6 md:p-8">
        {/* Player heading */}
        <div className="mb-6">
          <span className="inline-flex items-center rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-[0.16em] text-blue-700 dark:border-blue-400/20 dark:bg-blue-500/10 dark:text-blue-400">
            Shared playback
          </span>

          <h2 className="mt-4 text-xl font-bold tracking-tight text-gray-900 dark:text-slate-50 sm:text-2xl">
            FrameHost Player
          </h2>

          <p className="mt-2 text-sm leading-6 text-gray-500 dark:text-slate-400">
            Load a YouTube video and control playback across connected devices.
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
            aria-label="YouTube video URL or ID"
            className="min-w-0 flex-1 rounded-xl border border-gray-300 bg-white px-4 py-3 text-sm text-gray-900 outline-none transition-all placeholder:text-gray-400 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 dark:border-slate-700 dark:bg-slate-900/70 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:border-blue-400 dark:focus:ring-blue-500/10"
          />

          <button
            type="submit"
            disabled={!isReady}
            className="rounded-xl bg-blue-600 px-6 py-3 text-sm font-semibold text-white shadow-sm shadow-blue-600/20 transition-colors hover:bg-blue-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-blue-500 dark:hover:bg-blue-400 dark:focus-visible:outline-blue-400"
          >
            Load Video
          </button>
        </form>

        {/* Input validation error */}
        {inputError && (
          <p
            className="mb-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300"
            role="alert"
          >
            {inputError}
          </p>
        )}

        {/* YouTube player container */}
        <div className="relative aspect-video overflow-hidden rounded-xl border border-gray-200 bg-black dark:border-slate-700">
          <div ref={containerRef} className="h-full w-full" />

          {!isReady && (
            <div className="absolute inset-0 flex items-center justify-center bg-gray-950 text-sm text-slate-300">
              Initializing YouTube player...
            </div>
          )}
        </div>

        {/* Player error */}
        {error && (
          <p
            className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300"
            role="alert"
          >
            {error}
          </p>
        )}

        {/* Playback controls */}
        <div className="mt-6 flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={play}
            disabled={!isReady}
            className="rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-blue-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-blue-500 dark:hover:bg-blue-400 dark:focus-visible:outline-blue-400"
          >
            Play
          </button>

          <button
            type="button"
            onClick={pause}
            disabled={!isReady}
            className="rounded-xl border border-gray-300 bg-white px-5 py-2.5 text-sm font-semibold text-gray-700 transition-colors hover:bg-gray-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:focus-visible:outline-blue-400"
          >
            Pause
          </button>

          <span
            className="ml-1 text-sm font-medium text-gray-600 dark:text-slate-300"
            aria-live="polite"
          >
            {playbackStatus}
          </span>
        </div>

        {/* WebSocket connection status */}
        <div className="mb-4 mt-5 flex flex-wrap items-center gap-2">
          <span
            className={`h-2.5 w-2.5 shrink-0 rounded-full ${
              isConnected
                ? "bg-green-500 dark:bg-emerald-400"
                : isConnecting
                  ? "animate-pulse bg-yellow-500 dark:bg-amber-400"
                  : "bg-gray-400 dark:bg-slate-500"
            }`}
          />

          <span
            className="text-sm text-gray-600 dark:text-slate-300"
            aria-live="polite"
          >
            {isConnected
              ? "Connected to room"
              : connectionStatus === "CONNECTING"
                ? "Connecting..."
                : connectionStatus === "RECONNECTING"
                  ? "Reconnecting..."
                  : "Not connected"}
          </span>
        </div>

        {/* Synchronization error */}
        {syncError && (
          <p
            className="mb-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300"
            role="alert"
          >
            {syncError}
          </p>
        )}

        {/* Playback timeline */}
        <div className="mt-6 border-t border-gray-100 pt-5 dark:border-slate-800">
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
            className="w-full cursor-pointer accent-blue-600 disabled:cursor-not-allowed dark:accent-blue-400"
            aria-label="Video playback position"
          />

          <div className="mt-2 flex justify-between text-xs font-medium tabular-nums text-gray-500 dark:text-slate-400">
            <span>{formatTime(currentTime)}</span>
            <span>{formatTime(duration)}</span>
          </div>
        </div>
      </div>
    </section>
  );
}

export default YouTubePlayer;

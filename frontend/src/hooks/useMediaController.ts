import { useCallback, useEffect, useRef, useState } from "react";

import type {
  MediaPlaybackSnapshot,
  MediaPlaybackStatus,
} from "../types/media";

const SUPPORTED_VIDEO_EXTENSIONS = /\.(mp4|webm|ogg|mov|m4v)$/i;

export function useMediaController() {
  const videoRef = useRef<HTMLVideoElement>(null);
  const objectUrlRef = useRef<string | null>(null);

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [videoUrl, setVideoUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [snapshot, setSnapshot] = useState<MediaPlaybackSnapshot>({
    provider: "LOCAL",
    status: "IDLE",
    currentTime: 0,
    duration: 0,
    playbackRate: 1,
  });

  // Release the object URL when the hook's component unmounts.
  useEffect(() => {
    return () => {
      if (objectUrlRef.current) {
        URL.revokeObjectURL(objectUrlRef.current);
        objectUrlRef.current = null;
      }
    };
  }, []);

  // Update the playback snapshot.
  const updateSnapshot = useCallback(
    (updates: Partial<MediaPlaybackSnapshot>) => {
      setSnapshot((current) => ({
        ...current,
        ...updates,
      }));
    },
    [],
  );

  // Select a local video file.
  const selectFile = useCallback(
    (file: File) => {
      const isVideo =
        file.type.startsWith("video/") ||
        SUPPORTED_VIDEO_EXTENSIONS.test(file.name);

      if (!isVideo) {
        setError("Please select a supported video file.");
        return false;
      }

      // Release the previous object URL.
      if (objectUrlRef.current) {
        URL.revokeObjectURL(objectUrlRef.current);
      }

      const newUrl = URL.createObjectURL(file);

      objectUrlRef.current = newUrl;

      setSelectedFile(file);
      setVideoUrl(newUrl);
      setError(null);

      updateSnapshot({
        provider: "LOCAL",
        status: "IDLE",
        currentTime: 0,
        duration: 0,
        playbackRate: 1,
      });

      return true;
    },
    [updateSnapshot],
  );

  // Remove the selected video.
  const clearFile = useCallback(() => {
    videoRef.current?.pause();

    if (objectUrlRef.current) {
      URL.revokeObjectURL(objectUrlRef.current);
      objectUrlRef.current = null;
    }

    setSelectedFile(null);
    setVideoUrl(null);
    setError(null);

    updateSnapshot({
      status: "IDLE",
      currentTime: 0,
      duration: 0,
      playbackRate: 1,
    });
  }, [updateSnapshot]);

  // Set playback speed.
  const setPlaybackRate = useCallback(
    (rate: number) => {
      if (!Number.isFinite(rate) || rate <= 0) {
        return;
      }

      if (videoRef.current) {
        videoRef.current.playbackRate = rate;
      }

      updateSnapshot({ playbackRate: rate });
    },
    [updateSnapshot],
  );

  // Synchronize the snapshot with native video events.
  const updateStatus = useCallback(
    (status: MediaPlaybackStatus) => {
      updateSnapshot({ status });
    },
    [updateSnapshot],
  );

  const handleLoadedMetadata = useCallback(() => {
    const video = videoRef.current;

    if (!video) return;

    updateSnapshot({
      duration: Number.isFinite(video.duration) ? video.duration : 0,
      currentTime: video.currentTime,
      playbackRate: video.playbackRate,
    });
  }, [updateSnapshot]);

  const handleTimeUpdate = useCallback(() => {
    const video = videoRef.current;

    if (!video) return;

    updateSnapshot({
      currentTime: video.currentTime,
      duration: Number.isFinite(video.duration) ? video.duration : 0,
    });
  }, [updateSnapshot]);

  const handleRateChange = useCallback(() => {
    const video = videoRef.current;

    if (!video) return;

    updateSnapshot({ playbackRate: video.playbackRate });
  }, [updateSnapshot]);

  return {
    videoRef,
    selectedFile,
    videoUrl,
    error,
    snapshot,
    selectFile,
    clearFile,
    setPlaybackRate,
    updateStatus,
    handleLoadedMetadata,
    handleTimeUpdate,
    handleRateChange,
    setError,
  };
}

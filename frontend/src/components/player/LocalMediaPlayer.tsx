import { useMediaController } from "../../hooks/useMediaController";

function LocalMediaPlayer() {
  const {
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
  } = useMediaController();

  function handleFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];

    if (file) {
      selectFile(file);
    }

    // Allow selecting the same file again.
    event.target.value = "";
  }

  return (
    <section className="mx-auto w-full max-w-5xl px-4 py-8">
      <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-[#111C30]">
        <h2 className="mb-2 text-xl font-bold text-gray-900 dark:text-white">
          Local Video Player
        </h2>

        <p className="mb-5 text-sm text-gray-500 dark:text-slate-400">
          Play a video stored on this device.
        </p>

        <div className="mb-5 flex flex-wrap gap-3">
          <label className="cursor-pointer rounded-xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white hover:bg-blue-700">
            Select Video
            <input
              type="file"
              accept="video/*,.mp4,.webm,.ogg,.mov,.m4v"
              onChange={handleFileChange}
              className="sr-only"
            />
          </label>

          {selectedFile && (
            <button
              type="button"
              onClick={clearFile}
              className="rounded-xl border border-gray-300 px-5 py-3 text-sm font-semibold text-gray-700 hover:bg-gray-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
            >
              Remove Video
            </button>
          )}
        </div>

        {error && (
          <p className="mb-4 text-sm text-red-600" role="alert">
            {error}
          </p>
        )}

        {videoUrl ? (
          <>
            <video
              ref={videoRef}
              src={videoUrl}
              controls
              playsInline
              preload="metadata"
              onLoadedMetadata={handleLoadedMetadata}
              onTimeUpdate={handleTimeUpdate}
              onPlay={() => updateStatus("PLAYING")}
              onPause={() => updateStatus("PAUSED")}
              onWaiting={() => updateStatus("BUFFERING")}
              onEnded={() => updateStatus("ENDED")}
              onRateChange={handleRateChange}
              onError={() =>
                setError(
                  "Unable to play this video. Its format or codec may not be supported.",
                )
              }
              className="w-full rounded-xl bg-black"
            />

            <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
              <div className="min-w-0">
                <p className="truncate text-sm text-gray-600 dark:text-slate-300">
                  {selectedFile?.name}
                </p>

                <p className="mt-1 text-xs text-gray-500">
                  {snapshot.status} · {Math.round(snapshot.currentTime)}s
                </p>
              </div>

              <label className="flex items-center gap-2 text-sm text-gray-700 dark:text-slate-300">
                Playback speed
                <select
                  value={snapshot.playbackRate}
                  onChange={(event) =>
                    setPlaybackRate(Number(event.target.value))
                  }
                  className="rounded-lg border border-gray-300 bg-white px-3 py-2 dark:border-slate-700 dark:bg-slate-900"
                >
                  {[0.5, 0.75, 1, 1.25, 1.5, 1.75, 2].map((rate) => (
                    <option key={rate} value={rate}>
                      {rate}x
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </>
        ) : (
          <div className="flex aspect-video items-center justify-center rounded-xl bg-gray-100 text-sm text-gray-500 dark:bg-slate-900 dark:text-slate-400">
            Select a video to start playback.
          </div>
        )}
      </div>
    </section>
  );
}

export default LocalMediaPlayer;

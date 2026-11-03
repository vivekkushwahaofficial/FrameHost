interface HeroProps {
  onCreateRoom: () => void;
  onJoinRoom: () => void;
}

function Hero({ onCreateRoom, onJoinRoom }: HeroProps) {
  return (
    <section
      id="get-started"
      className="bg-white px-5 py-16 transition-colors duration-200 dark:bg-gray-950 sm:px-6 sm:py-20 lg:py-24"
    >
      <div className="mx-auto max-w-4xl text-center">
        {/* Feature badge */}
        <span className="inline-flex items-center gap-2 rounded-full border border-blue-100 bg-blue-50 px-3 py-1.5 text-xs font-semibold text-blue-700 dark:border-blue-900 dark:bg-blue-950 dark:text-blue-300">
          <span className="h-1.5 w-1.5 rounded-full bg-blue-600" />
          Multi-device video synchronization
        </span>

        {/* Main heading */}
        <h1 className="mt-6 text-4xl font-bold leading-tight tracking-tight text-gray-950 dark:text-gray-100 sm:text-5xl lg:text-6xl">
          One video.
          <span className="block text-blue-600">Every device. In sync.</span>
        </h1>

        {/* Description */}
        <p className="mx-auto mt-5 max-w-2xl text-base leading-7 text-gray-600 dark:text-gray-400 sm:text-lg">
          Watch YouTube together across multiple devices. Create a room, connect
          your devices, and enjoy synchronized playback with shared control.
        </p>

        {/* Action buttons */}
        <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
          <button
            type="button"
            onClick={onCreateRoom}
            className="inline-flex min-h-11 items-center justify-center rounded-lg bg-blue-600 px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-blue-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
          >
            Create a room
            <span className="ml-2" aria-hidden="true">
              →
            </span>
          </button>

          <button
            type="button"
            onClick={onJoinRoom}
            className="inline-flex min-h-11 items-center justify-center rounded-lg border border-gray-300 bg-white px-6 py-3 text-sm font-semibold text-gray-700 transition-colors hover:border-gray-400 hover:bg-gray-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 dark:border-gray-600 dark:bg-gray-900 dark:text-gray-200 dark:hover:border-gray-500 dark:hover:bg-gray-800"
          >
            Join a room
          </button>
        </div>

        {/* Additional information */}
        <p className="mt-5 text-xs text-gray-500 dark:text-gray-400">
          Free to use <span className="mx-2">·</span>
          No app installation required
        </p>
      </div>
    </section>
  );
}

export default Hero;

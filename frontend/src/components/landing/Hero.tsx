function Hero() {
  return (
    <section id="get-started" className="bg-white px-6 py-20 sm:py-28">
      <div className="mx-auto max-w-4xl text-center">
        {/* Introduction badge */}
        <span className="inline-flex items-center rounded-full bg-blue-50 px-4 py-2 text-sm font-medium text-blue-700">
          Watch together, from anywhere
        </span>

        {/* Main heading */}
        <h1 className="mt-8 text-4xl font-bold tracking-tight text-gray-900 sm:text-5xl lg:text-6xl">
          One video.
          <span className="block text-blue-600">Every screen. In sync.</span>
        </h1>

        {/* Description */}
        <p className="mx-auto mt-6 max-w-2xl text-base leading-7 text-gray-600 sm:text-lg">
          Enjoy synchronized video playback across multiple devices. One host
          controls the experience while everyone watches together.
        </p>

        {/* Primary actions */}
        <div className="mt-10 flex flex-col justify-center gap-4 sm:flex-row">
          <button
            type="button"
            className="rounded-lg bg-blue-600 px-7 py-3 text-sm font-semibold text-white transition-colors hover:bg-blue-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
          >
            Create Room
          </button>

          <button
            type="button"
            className="rounded-lg border border-gray-300 bg-white px-7 py-3 text-sm font-semibold text-gray-700 transition-colors hover:bg-gray-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
          >
            Join Room
          </button>
        </div>

        {/* Supporting information */}
        <p className="mt-6 text-sm text-gray-500">
          Free to use · No downloads required
        </p>
      </div>
    </section>
  );
}

export default Hero;

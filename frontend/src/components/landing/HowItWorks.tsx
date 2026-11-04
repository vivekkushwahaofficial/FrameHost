const steps = [
  {
    number: "01",
    title: "Create a room",
    description:
      "Create a room and become its host. A unique room code is generated for other devices to join.",
  },
  {
    number: "02",
    title: "Connect devices",
    description:
      "Open FrameHost on another device and join using the room code as a Controller or Follower.",
  },
  {
    number: "03",
    title: "Watch in sync",
    description:
      "The host manages playback while connected devices follow the shared video session.",
  },
];

function HowItWorks() {
  return (
    <section
      id="how-it-works"
      className="scroll-mt-16 border-y border-gray-200 bg-gray-50 px-5 py-16 transition-colors duration-300 dark:border-slate-800 dark:bg-[#090F1D] sm:px-6 lg:py-20"
    >
      <div className="mx-auto max-w-7xl">
        {/* Section heading */}
        <div className="mx-auto max-w-xl text-center">
          <span className="inline-flex items-center rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-[0.18em] text-blue-700 dark:border-blue-400/20 dark:bg-blue-500/10 dark:text-blue-400">
            Getting started
          </span>

          <h2 className="mt-4 text-3xl font-bold tracking-tight text-gray-950 dark:text-slate-50 sm:text-4xl">
            How FrameHost works
          </h2>

          <p className="mt-4 text-sm leading-7 text-gray-600 dark:text-slate-300 sm:text-base">
            Start a shared viewing session in three simple steps.
          </p>
        </div>

        {/* Steps */}
        <div className="mt-12 grid gap-5 md:grid-cols-3">
          {steps.map((step) => (
            <article
              key={step.number}
              className="group relative overflow-hidden rounded-2xl border border-gray-200 bg-white p-6 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-blue-300 hover:shadow-lg hover:shadow-blue-500/5 dark:border-slate-800 dark:bg-[#111C30] dark:shadow-none dark:hover:border-blue-500/40 dark:hover:shadow-lg dark:hover:shadow-blue-500/5 sm:p-7"
            >
              {/* Subtle decorative accent */}
              <div
                aria-hidden="true"
                className="pointer-events-none absolute -right-10 -top-10 h-28 w-28 rounded-full bg-blue-500/5 blur-2xl transition-opacity duration-300 group-hover:bg-blue-500/10 dark:bg-blue-400/5 dark:group-hover:bg-blue-400/10"
              />

              {/* Step number */}
              <div className="relative inline-flex h-11 w-11 items-center justify-center rounded-xl border border-blue-200 bg-blue-50 text-sm font-bold tracking-wide text-blue-700 transition-colors duration-300 group-hover:border-blue-300 group-hover:bg-blue-100 dark:border-blue-400/20 dark:bg-blue-500/10 dark:text-blue-400 dark:group-hover:border-blue-400/40 dark:group-hover:bg-blue-500/15">
                {step.number}
              </div>

              {/* Step content */}
              <h3 className="relative mt-6 text-lg font-semibold tracking-tight text-gray-900 dark:text-slate-100 sm:text-xl">
                {step.title}
              </h3>

              <p className="relative mt-3 text-sm leading-7 text-gray-600 dark:text-slate-300">
                {step.description}
              </p>

              {/* Bottom accent */}
              <div
                aria-hidden="true"
                className="mt-7 h-px w-10 bg-blue-500/40 transition-all duration-300 group-hover:w-16 group-hover:bg-blue-500 dark:bg-blue-400/40"
              />
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

export default HowItWorks;

const steps = [
  {
    number: "01",
    title: "Create a room",
    description:
      "One person creates a room and becomes the host. Choose a video to start watching together.",
  },
  {
    number: "02",
    title: "Invite your devices",
    description:
      "Join the room from other devices using the room code. Choose Controller or Follower mode.",
  },
  {
    number: "03",
    title: "Watch in sync",
    description:
      "The host controls playback, and connected devices follow the same video experience.",
  },
];

function HowItWorks() {
  return (
    <section id="how-it-works" className="bg-gray-50 px-6 py-20 sm:py-24">
      <div className="mx-auto max-w-7xl">
        {/* Section heading */}
        <div className="mx-auto max-w-2xl text-center">
          <span className="text-sm font-semibold uppercase tracking-wider text-blue-600">
            Simple experience
          </span>

          <h2 className="mt-3 text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">
            How FrameHost works
          </h2>

          <p className="mt-4 text-base leading-7 text-gray-600">
            Get everyone on the same video in three simple steps.
          </p>
        </div>

        {/* Steps */}
        <div className="mt-14 grid gap-8 md:grid-cols-3">
          {steps.map((step) => (
            <article
              key={step.number}
              className="rounded-xl border border-gray-200 bg-white p-7"
            >
              <span className="text-sm font-bold text-blue-600">
                STEP {step.number}
              </span>

              <h3 className="mt-4 text-xl font-semibold text-gray-900">
                {step.title}
              </h3>

              <p className="mt-3 text-sm leading-6 text-gray-600">
                {step.description}
              </p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

export default HowItWorks;

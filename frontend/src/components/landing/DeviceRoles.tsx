const roles = [
  {
    title: "Host",
    label: "Main device",
    description: "Creates the room and manages the shared viewing session.",
    features: [
      "Creates and manages rooms",
      "Controls playback",
      "Manages participant permissions",
    ],
  },
  {
    title: "Controller",
    label: "Remote control",
    description:
      "Connects from another device and sends commands allowed by the host.",
    features: [
      "Sends playback commands",
      "Uses granted permissions",
      "Controls remotely",
    ],
  },
  {
    title: "Follower",
    label: "Watch-only",
    description:
      "Joins the room to watch the shared video and follow synchronized playback.",
    features: [
      "Watches the shared video",
      "Follows playback changes",
      "Does not control playback",
    ],
  },
];

function DeviceRoles() {
  return (
    <section
      id="device-roles"
      className="scroll-mt-16 bg-white px-5 py-16 transition-colors duration-300 dark:bg-[#090F1D] sm:px-6 lg:py-20"
    >
      <div className="mx-auto max-w-7xl">
        {/* Section heading */}
        <div className="mx-auto max-w-xl text-center">
          <span className="inline-flex items-center rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-[0.18em] text-blue-700 dark:border-blue-400/20 dark:bg-blue-500/10 dark:text-blue-400">
            Device permissions
          </span>

          <h2 className="mt-4 text-3xl font-bold tracking-tight text-gray-950 dark:text-slate-50 sm:text-4xl">
            One room. Three roles.
          </h2>

          <p className="mt-4 text-sm leading-7 text-gray-600 dark:text-slate-300 sm:text-base">
            Each connected device has a clear responsibility in the session.
          </p>
        </div>

        {/* Role cards */}
        <div className="mt-12 grid gap-5 md:grid-cols-3">
          {roles.map((role, index) => (
            <article
              key={role.title}
              className="group relative overflow-hidden rounded-2xl border border-gray-200 bg-white p-6 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-blue-300 hover:shadow-lg hover:shadow-blue-500/5 dark:border-slate-800 dark:bg-[#111C30] dark:shadow-none dark:hover:border-blue-500/40 dark:hover:shadow-lg dark:hover:shadow-blue-500/5 sm:p-7"
            >
              {/* Decorative accent */}
              <div
                aria-hidden="true"
                className="pointer-events-none absolute -right-10 -top-10 h-28 w-28 rounded-full bg-blue-500/5 blur-2xl transition-colors duration-300 group-hover:bg-blue-500/10 dark:bg-blue-400/5 dark:group-hover:bg-blue-400/10"
              />

              {/* Role metadata */}
              <div className="relative flex items-center justify-between gap-3">
                <span className="text-xs font-bold tracking-[0.16em] text-blue-600 dark:text-blue-400">
                  ROLE 0{index + 1}
                </span>

                <span className="rounded-full border border-gray-200 bg-gray-50 px-3 py-1 text-xs font-medium text-gray-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300">
                  {role.label}
                </span>
              </div>

              {/* Role information */}
              <h3 className="relative mt-6 text-xl font-bold tracking-tight text-gray-900 dark:text-slate-100">
                {role.title}
              </h3>

              <p className="relative mt-3 min-h-12 text-sm leading-7 text-gray-600 dark:text-slate-300">
                {role.description}
              </p>

              {/* Divider */}
              <div className="my-6 border-t border-gray-100 dark:border-slate-800" />

              {/* Responsibilities */}
              <h4 className="relative text-xs font-semibold uppercase tracking-[0.14em] text-gray-500 dark:text-slate-400">
                Responsibilities
              </h4>

              <ul className="relative mt-5 space-y-4">
                {role.features.map((feature) => (
                  <li
                    key={feature}
                    className="flex items-start gap-3 text-sm leading-6 text-gray-700 dark:text-slate-300"
                  >
                    <svg
                      className="mt-1 h-4 w-4 shrink-0 text-blue-600 dark:text-blue-400"
                      viewBox="0 0 20 20"
                      fill="currentColor"
                      aria-hidden="true"
                    >
                      <path
                        fillRule="evenodd"
                        d="M16.704 5.29a1 1 0 0 1 .006 1.414l-7.2 7.26a1 1 0 0 1-1.42 0l-3.8-3.83a1 1 0 1 1 1.42-1.41l3.09 3.11 6.49-6.54a1 1 0 0 1 1.414-.004Z"
                        clipRule="evenodd"
                      />
                    </svg>

                    <span>{feature}</span>
                  </li>
                ))}
              </ul>

              {/* Bottom accent */}
              <div
                aria-hidden="true"
                className="relative mt-8 h-px w-10 bg-blue-500/40 transition-all duration-300 group-hover:w-16 group-hover:bg-blue-500 dark:bg-blue-400/40"
              />
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

export default DeviceRoles;

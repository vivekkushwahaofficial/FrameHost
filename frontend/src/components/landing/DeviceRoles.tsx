const roles = [
  {
    title: "Host",
    description:
      "Creates the room, selects the video and has ultimate authority over playback.",
    features: [
      "Creates and manages rooms",
      "Controls video playback",
      "Manages participants",
    ],
  },
  {
    title: "Controller",
    description:
      "Connects from another device and sends playback commands according to host permissions.",
    features: [
      "Remote playback controls",
      "Sends playback commands",
      "Connects independently",
    ],
  },
  {
    title: "Follower",
    description:
      "Joins the room to enjoy synchronized playback without controlling the session.",
    features: [
      "Watches synchronized video",
      "Follows host playback",
      "No playback authority",
    ],
  },
];

function DeviceRoles() {
  return (
    <section id="device-roles" className="bg-white px-6 py-20 sm:py-24">
      <div className="mx-auto max-w-7xl">
        <div className="mx-auto max-w-2xl text-center">
          <span className="text-sm font-semibold uppercase tracking-wider text-blue-600">
            Multi-device experience
          </span>

          <h2 className="mt-3 text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">
            One room. Three device roles.
          </h2>

          <p className="mt-4 text-base leading-7 text-gray-600">
            Every device has a clear responsibility in your synchronized viewing
            experience.
          </p>
        </div>

        <div className="mt-14 grid gap-8 md:grid-cols-3">
          {roles.map((role) => (
            <article
              key={role.title}
              className="rounded-xl border border-gray-200 p-7"
            >
              <h3 className="text-xl font-semibold text-gray-900">
                {role.title}
              </h3>

              <p className="mt-3 text-sm leading-6 text-gray-600">
                {role.description}
              </p>

              <ul className="mt-6 space-y-3">
                {role.features.map((feature) => (
                  <li
                    key={feature}
                    className="flex items-start gap-3 text-sm text-gray-700"
                  >
                    <span
                      className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-blue-600"
                      aria-hidden="true"
                    />
                    {feature}
                  </li>
                ))}
              </ul>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

export default DeviceRoles;

const GITHUB_REPO = "https://github.com/vivekkushwahaofficial/FrameHost";

const footerLinkClass =
  "text-sm text-gray-600 transition-colors hover:text-blue-600 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-blue-600 dark:text-slate-200 dark:hover:text-blue-400 dark:focus-visible:outline-blue-400";

function Footer() {
  return (
    <footer className="border-t border-gray-200 bg-gray-50 transition-colors duration-300 dark:border-slate-800 dark:bg-[#090F1D]">
      <div className="mx-auto max-w-7xl px-5 pb-5 pt-10 sm:px-6 sm:pb-6 sm:pt-12">
        {/* Main footer content */}
        <div className="grid gap-9 sm:grid-cols-2 lg:grid-cols-12">
          {/* Brand and description */}
          <div className="lg:col-span-6">
            <a
              href="/"
              className="inline-flex items-center gap-2 rounded-sm focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-blue-600"
              aria-label="FrameHost home"
            >
              {/* FrameHost icon */}
              <svg
                viewBox="0 0 40 40"
                className="h-9 w-9 shrink-0"
                fill="none"
                aria-hidden="true"
              >
                <rect
                  x="4"
                  y="5"
                  width="25"
                  height="25"
                  rx="6"
                  stroke="#0EA5E9"
                  strokeWidth="3"
                />
                <rect
                  x="11"
                  y="10"
                  width="25"
                  height="25"
                  rx="6"
                  fill="#2563EB"
                />
                <path d="M20 16L29 22.5L20 29V16Z" fill="white" />
              </svg>

              {/* Brand name */}
              <span className="text-xl font-bold tracking-tight text-gray-950 dark:text-slate-100">
                FrameHost
                <span className="text-blue-600 dark:text-blue-400">.</span>
              </span>
            </a>

            <p className="mt-4 max-w-sm text-sm leading-7 text-gray-600 dark:text-slate-300">
              Watch YouTube together across multiple devices. Create a room,
              connect your devices, and stay synchronized throughout the viewing
              session.
            </p>

            {/* GitHub link */}
            <a
              href={GITHUB_REPO}
              target="_blank"
              rel="noopener noreferrer"
              className="mt-5 inline-flex items-center gap-2 text-sm font-semibold text-gray-700 transition-colors hover:text-blue-600 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-blue-600 dark:text-slate-200 dark:hover:text-blue-400 dark:focus-visible:outline-blue-400"
            >
              <svg
                className="h-5 w-5"
                viewBox="0 0 24 24"
                fill="currentColor"
                aria-hidden="true"
              >
                <path
                  fillRule="evenodd"
                  clipRule="evenodd"
                  d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.009-.868-.014-1.703-2.782.605-3.369-1.342-3.369-1.342-.454-1.157-1.11-1.465-1.11-1.465-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.646.35-1.087.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0 1 12 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.026 2.747-1.026.546 1.378.202 2.397.1 2.65.64.7 1.027 1.595 1.027 2.688 0 3.848-2.339 4.695-4.566 4.943.359.31.678.921.678 1.856 0 1.34-.012 2.42-.012 2.75 0 .268.18.58.688.482A10.019 10.019 0 0 0 22 12.017C22 6.484 17.523 2 12 2Z"
                />
              </svg>
              Explore on GitHub
              <span aria-hidden="true">↗</span>
              <span className="sr-only">(opens in a new tab)</span>
            </a>
          </div>

          {/* Navigation links */}
          <div className="lg:col-span-3">
            <h3 className="text-sm font-bold tracking-wide text-gray-900 dark:text-slate-100">
              Navigation
            </h3>

            <ul className="mt-4 space-y-3">
              <li>
                <a href="/#get-started" className={footerLinkClass}>
                  Get started
                </a>
              </li>

              <li>
                <a href="/#how-it-works" className={footerLinkClass}>
                  How it works
                </a>
              </li>

              <li>
                <a href="/#device-roles" className={footerLinkClass}>
                  Device roles
                </a>
              </li>
            </ul>
          </div>

          {/* Project links */}
          <div className="lg:col-span-3">
            <h3 className="text-sm font-bold tracking-wide text-gray-900 dark:text-slate-100">
              Project
            </h3>

            <ul className="mt-4 space-y-3">
              <li>
                <a
                  href={GITHUB_REPO}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={footerLinkClass}
                >
                  GitHub repository ↗
                </a>
              </li>

              <li>
                <a
                  href={`${GITHUB_REPO}/issues`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={footerLinkClass}
                >
                  Report an issue ↗
                </a>
              </li>

              {/* Platform statistics link */}
              <li>
                <a href="/stats" className={footerLinkClass}>
                  View Statistics →
                </a>
              </li>
            </ul>
          </div>
        </div>

        {/* Footer bottom */}
        <div className="mt-10 flex flex-col gap-3 border-t border-gray-200 pt-5 sm:flex-row sm:items-center sm:justify-between dark:border-slate-800">
          <p className="text-xs leading-5 text-gray-500 dark:text-slate-400">
            © {new Date().getFullYear()} FrameHost. All rights reserved.
          </p>

          <p className="text-xs leading-5 text-gray-500 dark:text-slate-400">
            Built for watching together.
          </p>
        </div>
      </div>
    </footer>
  );
}

export default Footer;

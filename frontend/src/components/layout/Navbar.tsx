import { useState } from "react";
import { useTheme } from "../../context/useTheme";

interface NavbarProps {
  isRoomActive?: boolean;
}

function Navbar({ isRoomActive = false }: NavbarProps) {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const { isDark, toggleTheme } = useTheme();

  const navLinkClass =
    "text-sm font-medium text-gray-600 transition-colors hover:text-blue-600 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-blue-600 dark:text-gray-300 dark:hover:text-blue-400";

  const closeMenu = () => setIsMenuOpen(false);

  return (
    <header className="sticky top-0 z-50 border-b border-gray-200 bg-white/95 backdrop-blur dark:border-gray-800 dark:bg-gray-950/95">
      <nav
        className="mx-auto flex h-16 max-w-7xl items-center px-4 sm:px-6"
        aria-label="Main navigation"
      >
        {/* Brand */}
        {isRoomActive ? (
          <span
            className="inline-flex shrink-0 items-center gap-2"
            aria-label="FrameHost"
          >
            <BrandIcon />
            <BrandName />
          </span>
        ) : (
          <a
            href="/"
            onClick={closeMenu}
            className="inline-flex shrink-0 items-center gap-2 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-blue-600"
            aria-label="FrameHost home"
          >
            <BrandIcon />
            <BrandName />
          </a>
        )}

        {/* Desktop navigation */}
        {!isRoomActive && (
          <div className="ml-auto hidden flex-1 items-center justify-between md:flex">
            {/* Center navigation links */}
            <div className="mx-auto flex items-center gap-6 lg:gap-8">
              <a href="#how-it-works" className={navLinkClass}>
                How it works
              </a>

              <a href="#device-roles" className={navLinkClass}>
                Device roles
              </a>
            </div>

            {/* Right-side actions */}
            <div className="flex shrink-0 items-center gap-3">
              <ThemeToggle isDark={isDark} toggleTheme={toggleTheme} />

              <a
                href="#get-started"
                className="inline-flex whitespace-nowrap rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-blue-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
              >
                Get started
              </a>
            </div>
          </div>
        )}

        {/* Mobile and active-room actions */}
        <div
          className={`ml-auto flex shrink-0 items-center gap-2 ${!isRoomActive ? "md:hidden" : ""}`}
        >
          <ThemeToggle isDark={isDark} toggleTheme={toggleTheme} />

          {!isRoomActive && (
            <button
              type="button"
              onClick={() => setIsMenuOpen((open) => !open)}
              className="inline-flex h-10 w-10 items-center justify-center rounded-lg text-gray-700 transition-colors hover:bg-gray-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 dark:text-gray-200 dark:hover:bg-gray-800 md:hidden"
              aria-label={isMenuOpen ? "Close menu" : "Open menu"}
              aria-expanded={isMenuOpen}
              aria-controls="mobile-navigation"
            >
              {isMenuOpen ? (
                <svg
                  className="h-5 w-5"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  aria-hidden="true"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="m6 6 12 12M18 6 6 18"
                  />
                </svg>
              ) : (
                <svg
                  className="h-5 w-5"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  aria-hidden="true"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M4 6h16M4 12h16M4 18h16"
                  />
                </svg>
              )}
            </button>
          )}
        </div>
      </nav>

      {/* Mobile navigation dropdown */}
      {!isRoomActive && isMenuOpen && (
        <div
          id="mobile-navigation"
          className="border-t border-gray-100 bg-white px-5 py-4 dark:border-gray-800 dark:bg-gray-950 md:hidden"
        >
          <div className="mx-auto flex max-w-7xl flex-col gap-4">
            <a
              href="#how-it-works"
              onClick={closeMenu}
              className={navLinkClass}
            >
              How it works
            </a>

            <a
              href="#device-roles"
              onClick={closeMenu}
              className={navLinkClass}
            >
              Device roles
            </a>

            <a
              href="#get-started"
              onClick={closeMenu}
              className="inline-flex w-fit rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-blue-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
            >
              Get started
            </a>
          </div>
        </div>
      )}
    </header>
  );
}

/* Theme toggle button */
function ThemeToggle({
  isDark,
  toggleTheme,
}: {
  isDark: boolean;
  toggleTheme: () => void;
}) {
  return (
    <button
      type="button"
      onClick={toggleTheme}
      className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-lg text-gray-700 transition-colors hover:bg-gray-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 dark:text-gray-200 dark:hover:bg-gray-800"
      aria-label={isDark ? "Switch to light mode" : "Switch to dark mode"}
      title={isDark ? "Switch to light mode" : "Switch to dark mode"}
    >
      {isDark ? (
        /* Sun icon */
        <svg
          className="h-5 w-5"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2m0 16v2M4.93 4.93l1.42 1.42m11.3 11.3 1.42 1.42M2 12h2m16 0h2M4.93 19.07l1.42-1.42m11.3-11.3 1.42-1.42" />
        </svg>
      ) : (
        /* Moon icon */
        <svg
          className="h-5 w-5"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <path d="M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.463.402.807a6.5 6.5 0 0 0 8.268 8.268c.344-.215.829-.003.803.397Z" />
        </svg>
      )}
    </button>
  );
}

/* FrameHost logo icon */
function BrandIcon() {
  return (
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
      <rect x="11" y="10" width="25" height="25" rx="6" fill="#2563EB" />
      <path d="M20 16L29 22.5L20 29V16Z" fill="white" />
    </svg>
  );
}

/* FrameHost brand name */
function BrandName() {
  return (
    <span className="text-lg font-bold tracking-tight text-gray-900 dark:text-gray-100 sm:text-xl">
      FrameHost<span className="text-blue-600">.</span>
    </span>
  );
}

export default Navbar;

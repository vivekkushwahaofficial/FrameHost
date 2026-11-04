import { useEffect, useState, type ReactNode } from "react";

import {
  getAnalyticsStats,
  type AnalyticsStats,
} from "../services/api/analyticsApi";

import { useTheme } from "../context/useTheme";

const REFRESH_INTERVAL = 60_000;

type MetricIconName = "users" | "activity" | "calendar" | "video" | "monitor";

// Icon component for the five existing statistics.
function MetricIcon({ name }: { name: MetricIconName }) {
  const icons: Record<MetricIconName, ReactNode> = {
    users: (
      <>
        <path d="M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        <circle cx="10" cy="7" r="4" />
        <path d="M20 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
      </>
    ),
    activity: <path d="M22 12h-4l-3 9L9 3l-3 9H2" />,
    calendar: (
      <>
        <rect x="3" y="5" width="18" height="16" rx="2" />
        <path d="M16 3v4M8 3v4M3 11h18" />
      </>
    ),
    video: (
      <>
        <rect x="3" y="5" width="18" height="14" rx="2" />
        <path d="m10 9 5 3-5 3z" />
      </>
    ),
    monitor: (
      <>
        <rect x="2" y="3" width="20" height="14" rx="2" />
        <path d="M8 21h8M12 17v4" />
      </>
    ),
  };

  return (
    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl border border-blue-500/15 bg-blue-500/10 text-blue-600 dark:border-cyan-400/15 dark:bg-cyan-400/10 dark:text-cyan-400">
      <svg
        xmlns="http://www.w3.org/2000/svg"
        className="h-6 w-6"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        {icons[name]}
      </svg>
    </div>
  );
}

// Reuses FrameHost's existing global theme provider.
function ThemeToggle() {
  const { isDark, toggleTheme } = useTheme();

  return (
    <button
      type="button"
      onClick={toggleTheme}
      className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-gray-200 bg-white text-gray-700 transition-colors hover:bg-gray-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-200 dark:hover:bg-gray-800"
      aria-label={isDark ? "Switch to light mode" : "Switch to dark mode"}
      title={isDark ? "Switch to light mode" : "Switch to dark mode"}
    >
      {isDark ? (
        // Sun icon
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
        // Moon icon
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

function StatsPage() {
  const [stats, setStats] = useState<AnalyticsStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [retryCount, setRetryCount] = useState(0);

  // Load statistics and refresh them every 60 seconds.
  useEffect(() => {
    let isActive = true;

    const loadStats = async () => {
      try {
        const data = await getAnalyticsStats();

        if (!isActive) return;

        setStats(data);
        setError(null);
        setLastUpdated(new Date());
      } catch {
        if (isActive) {
          setError("Unable to load statistics. Please try again.");
        }
      } finally {
        if (isActive) {
          setLoading(false);
        }
      }
    };

    void loadStats();

    const intervalId = window.setInterval(() => {
      void loadStats();
    }, REFRESH_INTERVAL);

    return () => {
      isActive = false;
      window.clearInterval(intervalId);
    };
  }, [retryCount]);

  // Existing metrics with their corresponding icons.
  const metrics: {
    label: string;
    value: number | undefined;
    icon: MetricIconName;
  }[] = [
    {
      label: "Total Visitors",
      value: stats?.totalVisitors,
      icon: "users",
    },
    {
      label: "Active Visitors",
      value: stats?.activeVisitors,
      icon: "activity",
    },
    {
      label: "Today's Visitors",
      value: stats?.dailyUniqueVisitors,
      icon: "calendar",
    },
    {
      label: "Rooms Created",
      value: stats?.totalRoomsCreated,
      icon: "video",
    },
    {
      label: "Active Rooms",
      value: stats?.activeRooms,
      icon: "monitor",
    },
  ];

  return (
    <main className="flex min-h-screen flex-col bg-white px-4 py-5 text-gray-900 transition-colors duration-200 dark:bg-gray-950 dark:text-gray-100 sm:px-6 sm:py-6">
      <div className="mx-auto flex w-full max-w-6xl flex-1 flex-col">
        {/* Header */}
        <header className="mb-6">
          <a
            href="/"
            className="text-sm text-gray-700 transition-colors hover:text-blue-700 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-blue-600 dark:text-gray-300 dark:hover:text-cyan-300"
          >
            ← Back to FrameHost
          </a>

          <div className="mt-4 flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="mb-1 text-xs font-semibold tracking-widest text-blue-600 dark:text-cyan-400">
                FRAMEHOST TRANSPARENCY
              </p>

              <h1 className="text-2xl font-bold tracking-tight text-gray-950 dark:text-gray-100 sm:text-3xl">
                Platform Statistics
              </h1>

              <p className="mt-1 text-sm text-gray-700 dark:text-gray-300">
                A live overview of FrameHost activity.
              </p>
            </div>

            {/* Public status and global theme control */}
            <div className="flex items-center gap-3">
              <span className="rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1.5 text-xs font-medium text-emerald-600 dark:text-emerald-400">
                <span className="mr-1.5">●</span>
                Public statistics
              </span>

              <ThemeToggle />
            </div>
          </div>
        </header>

        {/* Dashboard content */}
        <section className="flex flex-1 flex-col justify-center">
          {/* Error Message */}
          {error && (
            <div
              className="mb-4 rounded-xl border border-red-300 bg-red-50 p-3 text-sm font-medium text-red-800 dark:border-red-500/40 dark:bg-red-500/10 dark:text-red-200"
              role="alert"
            >
              <p>{error}</p>

              <button
                type="button"
                onClick={() => {
                  setLoading(true);
                  setRetryCount((count) => count + 1);
                }}
                className="mt-2 font-semibold underline underline-offset-2 hover:no-underline"
              >
                Try again
              </button>
            </div>
          )}

          {/* Statistics Cards */}
          {loading && !stats ? (
            <div className="rounded-xl border border-gray-300 bg-gray-50 p-8 text-center text-sm font-medium text-gray-700 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-300">
              Loading FrameHost statistics...
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-6">
              {metrics.map((metric, index) => (
                <article
                  key={metric.label}
                  className={`flex min-h-[116px] items-center justify-between gap-4 rounded-xl border border-gray-200 bg-gray-50 p-5 transition-colors hover:border-blue-300 dark:border-gray-800 dark:bg-gray-900 dark:hover:border-cyan-500/40 lg:col-span-2 ${
                    index === 3 ? "lg:col-start-2" : ""
                  } ${index === 4 ? "lg:col-start-4" : ""}`}
                >
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-gray-700 dark:text-gray-200">
                      {metric.label}
                    </p>

                    <p className="mt-3 text-3xl font-bold tracking-tight text-gray-950 dark:text-white">
                      {metric.value?.toLocaleString("en-IN") ?? "—"}
                    </p>
                  </div>

                  <MetricIcon name={metric.icon} />
                </article>
              ))}
            </div>
          )}

          {/* Refresh Information */}
          <div className="mt-5 flex flex-wrap justify-between gap-2 text-xs font-medium text-gray-600 dark:text-gray-300">
            <p>Automatically refreshes every 60 seconds.</p>

            <p>
              Last updated:{" "}
              {lastUpdated ? lastUpdated.toLocaleTimeString() : "Not available"}
            </p>
          </div>
        </section>

        {/* Privacy Information */}
        <footer className="mt-6 border-t border-gray-300 pt-4 text-xs font-medium leading-5 text-gray-700 dark:border-gray-700 dark:text-gray-300">
          FrameHost displays aggregate platform statistics only. Individual
          visitor identities are not publicly displayed.
        </footer>
      </div>
    </main>
  );
}

export default StatsPage;

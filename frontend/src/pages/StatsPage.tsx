import { useEffect, useState } from "react";
import {
  getAnalyticsStats,
  type AnalyticsStats,
} from "../services/api/analyticsApi";

const REFRESH_INTERVAL = 60_000;

function StatsPage() {
  const [stats, setStats] = useState<AnalyticsStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [retryCount, setRetryCount] = useState(0);

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

    // Load statistics when the page mounts.
    void loadStats();

    // Refresh statistics every 60 seconds.
    const intervalId = window.setInterval(() => {
      void loadStats();
    }, REFRESH_INTERVAL);

    return () => {
      isActive = false;
      window.clearInterval(intervalId);
    };
  }, [retryCount]);

  const metrics = [
    {
      label: "Total Visitors",
      value: stats?.totalVisitors,
    },
    {
      label: "Active Visitors",
      value: stats?.activeVisitors,
    },
    {
      label: "Today's Visitors",
      value: stats?.dailyUniqueVisitors,
    },
    {
      label: "Rooms Created",
      value: stats?.totalRoomsCreated,
    },
    {
      label: "Active Rooms",
      value: stats?.activeRooms,
    },
  ];

  return (
    <main className="min-h-screen bg-slate-950 px-5 py-16 text-white">
      <div className="mx-auto max-w-6xl">
        <div className="mb-12">
          <a href="/" className="text-sm text-slate-400 hover:text-white">
            ← Back to FrameHost
          </a>

          <div className="mt-8 flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="mb-2 text-sm font-medium text-cyan-400">
                FRAMEHOST TRANSPARENCY
              </p>

              <h1 className="text-4xl font-bold tracking-tight">
                Platform Statistics
              </h1>

              <p className="mt-3 text-slate-400">
                A live overview of FrameHost activity.
              </p>
            </div>

            <span className="rounded-full border border-emerald-500/30 bg-emerald-500/10 px-4 py-2 text-sm text-emerald-400">
              ● Public statistics
            </span>
          </div>
        </div>

        {error && (
          <div
            className="mb-6 rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300"
            role="alert"
          >
            <p>{error}</p>

            <button
              onClick={() => {
                setLoading(true);
                setRetryCount((count) => count + 1);
              }}
              className="mt-3 text-sm font-semibold underline"
            >
              Try again
            </button>
          </div>
        )}

        {loading && !stats ? (
          <div className="rounded-2xl border border-slate-800 bg-slate-900 p-10 text-center text-slate-400">
            Loading FrameHost statistics...
          </div>
        ) : (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {metrics.map((metric) => (
              <article
                key={metric.label}
                className="rounded-2xl border border-slate-800 bg-slate-900 p-7 transition hover:border-cyan-500/40"
              >
                <p className="text-sm text-slate-400">{metric.label}</p>

                <p className="mt-4 text-4xl font-bold tracking-tight">
                  {metric.value?.toLocaleString("en-IN") ?? "—"}
                </p>
              </article>
            ))}
          </div>
        )}

        <div className="mt-8 flex flex-wrap justify-between gap-3 text-sm text-slate-500">
          <p>Automatically refreshes every 60 seconds.</p>

          <p>
            Last updated:{" "}
            {lastUpdated ? lastUpdated.toLocaleTimeString() : "Not available"}
          </p>
        </div>

        <p className="mt-12 border-t border-slate-800 pt-6 text-sm text-slate-500">
          FrameHost displays aggregate platform statistics only. Individual
          visitor identities are not publicly displayed.
        </p>
      </div>
    </main>
  );
}

export default StatsPage;

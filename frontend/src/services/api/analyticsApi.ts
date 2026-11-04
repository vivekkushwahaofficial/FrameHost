export interface AnalyticsStats {
  totalVisitors: number;
  activeVisitors: number;
  dailyUniqueVisitors: number;
  totalRoomsCreated: number;
  activeRooms: number;
}

const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"
).replace(/\/$/, "");

export async function getAnalyticsStats(): Promise<AnalyticsStats> {
  const response = await fetch(`${API_BASE_URL}/api/analytics/stats`, {
    method: "GET",
    headers: {
      Accept: "application/json",
    },
  });

  if (!response.ok) {
    throw new Error("Unable to load FrameHost statistics.");
  }

  return response.json() as Promise<AnalyticsStats>;
}

export async function sendAnalyticsHeartbeat(visitorId: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/analytics/heartbeat`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify({ visitorId }),
  });

  if (!response.ok) {
    throw new Error("Unable to record visitor heartbeat.");
  }
}

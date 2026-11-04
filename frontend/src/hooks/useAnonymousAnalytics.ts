import { useEffect } from "react";
import { sendAnalyticsHeartbeat } from "../services/api/analyticsApi";

const VISITOR_ID_KEY = "framehost_visitor_id";
const HEARTBEAT_INTERVAL = 60_000;

function getOrCreateVisitorId(): string | null {
  try {
    const existingId = localStorage.getItem(VISITOR_ID_KEY);

    if (existingId) {
      return existingId;
    }

    const visitorId = crypto.randomUUID();

    localStorage.setItem(VISITOR_ID_KEY, visitorId);

    return visitorId;
  } catch {
    // Analytics must never interrupt the application.
    return null;
  }
}

export function useAnonymousAnalytics(): void {
  useEffect(() => {
    const visitorId = getOrCreateVisitorId();

    if (!visitorId) {
      return;
    }

    let isActive = true;

    const sendHeartbeat = async () => {
      try {
        await sendAnalyticsHeartbeat(visitorId);
      } catch {
        if (isActive) {
          console.warn("Anonymous analytics heartbeat failed.");
        }
      }
    };

    // Record the initial visit.
    void sendHeartbeat();

    // Send a heartbeat every 60 seconds.
    const intervalId = window.setInterval(() => {
      void sendHeartbeat();
    }, HEARTBEAT_INTERVAL);

    return () => {
      isActive = false;
      window.clearInterval(intervalId);
    };
  }, []);
}

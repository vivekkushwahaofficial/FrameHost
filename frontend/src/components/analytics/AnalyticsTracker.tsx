import { useAnonymousAnalytics } from "../../hooks/useAnonymousAnalytics";

function AnalyticsTracker() {
  useAnonymousAnalytics();

  return null;
}

export default AnalyticsTracker;

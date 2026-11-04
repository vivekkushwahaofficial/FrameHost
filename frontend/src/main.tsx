import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App.tsx";
import AnalyticsTracker from "./components/analytics/AnalyticsTracker";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <AnalyticsTracker />
    <App />
  </StrictMode>,
);

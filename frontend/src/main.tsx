import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";

import App from "./App";
import { ThemeProvider } from "./context/ThemeContext";
import AnalyticsTracker from "./components/analytics/AnalyticsTracker";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <ThemeProvider>
      <AnalyticsTracker />
      <App />
    </ThemeProvider>
  </StrictMode>,
);

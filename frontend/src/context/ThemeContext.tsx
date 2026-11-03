import { useEffect, useState, type ReactNode } from "react";

import { ThemeContext, type Theme } from "./theme-context";

function getInitialTheme(): Theme {
  const savedTheme = localStorage.getItem("framehost-theme");

  if (
    savedTheme === "light" ||
    savedTheme === "dark" ||
    savedTheme === "system"
  ) {
    return savedTheme;
  }

  return "system";
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>(getInitialTheme);

  const [systemIsDark, setSystemIsDark] = useState(
    () => window.matchMedia("(prefers-color-scheme: dark)").matches,
  );

  const isDark = theme === "system" ? systemIsDark : theme === "dark";

  useEffect(() => {
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");

    const handleChange = (event: MediaQueryListEvent) => {
      setSystemIsDark(event.matches);
    };

    mediaQuery.addEventListener("change", handleChange);

    return () => {
      mediaQuery.removeEventListener("change", handleChange);
    };
  }, []);

  useEffect(() => {
    document.documentElement.classList.toggle("dark", isDark);
    document.documentElement.style.colorScheme = isDark ? "dark" : "light";

    localStorage.setItem("framehost-theme", theme);
  }, [theme, isDark]);

  function toggleTheme() {
    setTheme(isDark ? "light" : "dark");
  }

  return (
    <ThemeContext.Provider value={{ theme, isDark, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

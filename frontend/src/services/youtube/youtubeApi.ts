const YOUTUBE_API_URL = "https://www.youtube.com/iframe_api";

const SCRIPT_ID = "youtube-iframe-api";

let apiPromise: Promise<YouTubeNamespace> | null = null;

export function loadYouTubeApi(): Promise<YouTubeNamespace> {
  // Reuse an already-loaded API.
  if (window.YT?.Player) {
    return Promise.resolve(window.YT);
  }

  // Prevent loading the same API multiple times.
  if (apiPromise) {
    return apiPromise;
  }

  apiPromise = new Promise<YouTubeNamespace>((resolve, reject) => {
    // Preserve any existing API-ready callback.
    const previousCallback = window.onYouTubeIframeAPIReady;

    // YouTube calls this function after loading its API.
    window.onYouTubeIframeAPIReady = () => {
      previousCallback?.();

      if (window.YT?.Player) {
        resolve(window.YT);
      } else {
        apiPromise = null;
        reject(new Error("YouTube API failed to initialize."));
      }
    };

    // Check whether the script already exists.
    const existingScript = document.getElementById(SCRIPT_ID);

    if (existingScript) {
      return;
    }

    // Create the official YouTube API script.
    const script = document.createElement("script");

    script.id = SCRIPT_ID;
    script.src = YOUTUBE_API_URL;
    script.async = true;

    // Handle script loading failures.
    script.onerror = () => {
      apiPromise = null;
      script.remove();

      reject(new Error("Unable to load YouTube IFrame API."));
    };

    // Add the script to the document.
    document.head.appendChild(script);
  });

  return apiPromise;
}

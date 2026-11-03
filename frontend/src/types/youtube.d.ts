export {};

interface YouTubeVideoOptions {
  videoId: string;
  startSeconds?: number;
  endSeconds?: number;
  suggestedQuality?: string;
}

declare global {
  interface Window {
    YT?: YouTubeNamespace;
    onYouTubeIframeAPIReady?: () => void;
  }

  interface YouTubeNamespace {
    Player: new (
      element: HTMLElement | string,
      options: YouTubePlayerOptions,
    ) => YouTubePlayer;

    PlayerState: {
      UNSTARTED: -1;
      ENDED: 0;
      PLAYING: 1;
      PAUSED: 2;
      BUFFERING: 3;
      CUED: 5;
    };
  }

  interface YouTubePlayerOptions {
    width?: string | number;
    height?: string | number;
    videoId?: string;

    playerVars?: {
      autoplay?: 0 | 1;
      controls?: 0 | 1;
      enablejsapi?: 0 | 1;
      playsinline?: 0 | 1;
      rel?: 0 | 1;
      origin?: string;
    };

    events?: {
      onReady?: (event: YouTubePlayerEvent) => void;
      onStateChange?: (event: YouTubePlayerEvent<number>) => void;
      onError?: (event: YouTubePlayerEvent<number>) => void;
    };
  }

  interface YouTubePlayerEvent<T = undefined> {
    target: YouTubePlayer;
    data: T;
  }

  interface YouTubePlayer {
    playVideo(): void;
    pauseVideo(): void;
    stopVideo(): void;

    seekTo(seconds: number, allowSeekAhead: boolean): void;

    loadVideoById(videoId: string): void;
    loadVideoById(options: YouTubeVideoOptions): void;

    cueVideoById(videoId: string): void;
    cueVideoById(options: YouTubeVideoOptions): void;

    getVideoData(): {
      video_id: string;
      title: string;
    };

    getCurrentTime(): number;
    getPlayerState(): number;
    getDuration(): number;

    setPlaybackRate(rate: number): void;
    getPlaybackRate(): number;

    mute(): void;
    unMute(): void;
    isMuted(): boolean;

    destroy(): void;
  }
}

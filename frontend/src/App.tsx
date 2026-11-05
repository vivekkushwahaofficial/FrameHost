import { useEffect, useState } from "react";

import Navbar from "./components/layout/Navbar";
import Footer from "./components/layout/Footer";
import StatsPage from "./pages/StatsPage";
import Hero from "./components/landing/Hero";
import HowItWorks from "./components/landing/HowItWorks";
import DeviceRoles from "./components/landing/DeviceRoles";
import LocalMediaPlayer from "./components/player/LocalMediaPlayer";
import type { MediaProvider } from "./types/media";

import RoomAccess from "./components/room/RoomAccess";
import RoomSession from "./components/room/RoomSession";
import YouTubePlayer from "./components/player/YouTubePlayer";

import { usePlaybackSync } from "./hooks/usePlaybackSync";

import {
  createRoom as createRoomRequest,
  joinRoom as joinRoomRequest,
  leaveRoom,
  endRoom,
  getRoomDevices,
  updatePlaybackPermissions,
} from "./services/api/roomApi";

import type { DeviceRole, DeviceResponse } from "./types/room";
import type { PlaybackPermission } from "./types/playbackPermissions";

// Available room access modes.
type AccessMode = "CREATE" | "JOIN";

// Stores the authenticated room session.
interface RoomSessionData {
  roomCode: string;
  deviceId: string;
  deviceName: string;
  deviceToken: string;
  role: DeviceRole;
  deviceCount: number;
  maxDevices: number;
}

function FrameHostApp() {
  const [accessMode, setAccessMode] = useState<AccessMode | null>(null);

  const [roomSession, setRoomSession] = useState<RoomSessionData | null>(null);

  const [devices, setDevices] = useState<DeviceResponse[]>([]);

  const [loading, setLoading] = useState(false);

  const [error, setError] = useState<string | null>(null);

  // Tracks which device's permissions are currently being updated.
  const [updatingDeviceId, setUpdatingDeviceId] = useState<string | null>(null);

  // Select the playback provider.
  const [playerMode, setPlayerMode] = useState<MediaProvider>("YOUTUBE");

  // Automatically scroll to the room access form when opened.
  useEffect(() => {
    if (!accessMode) return;

    document.getElementById("room-access")?.scrollIntoView({
      behavior: "smooth",
      block: "center",
    });
  }, [accessMode]);

  // Extract primitive values for stable effect dependencies.
  const currentRoomCode = roomSession?.roomCode ?? "";
  const currentDeviceToken = roomSession?.deviceToken ?? "";
  const isHost = roomSession?.role === "HOST";

  // Manage the authenticated WebSocket connection.
  const playbackSync = usePlaybackSync({
    roomCode: currentRoomCode,
    deviceToken: currentDeviceToken,
    deviceRole: roomSession?.role ?? "FOLLOWER",
    enabled: roomSession !== null,
  });

  // Fetch and periodically refresh the device list for the Host.
  useEffect(() => {
    if (!isHost || !currentRoomCode || !currentDeviceToken) {
      return;
    }

    let isActive = true;

    const fetchDevices = async () => {
      try {
        const response = await getRoomDevices(
          currentRoomCode,
          currentDeviceToken,
        );

        if (!isActive) return;

        setDevices(response);

        // Synchronize the displayed device count.
        setRoomSession((currentSession) => {
          if (!currentSession || currentSession.roomCode !== currentRoomCode) {
            return currentSession;
          }

          return {
            ...currentSession,
            deviceCount: response.length,
          };
        });
      } catch (err) {
        if (!isActive) return;

        console.error("Unable to refresh room devices:", err);
      }
    };

    // Fetch devices immediately.
    void fetchDevices();

    // Refresh the list every five seconds.
    const intervalId = window.setInterval(() => {
      void fetchDevices();
    }, 5000);

    // Clean up the interval when the room changes or component unmounts.
    return () => {
      isActive = false;
      window.clearInterval(intervalId);
    };
  }, [isHost, currentRoomCode, currentDeviceToken]);

  // Create a new room.
  async function handleCreateRoom() {
    setLoading(true);
    setError(null);

    try {
      const response = await createRoomRequest();

      setDevices([]);

      setRoomSession({
        roomCode: response.roomCode,
        deviceId: response.hostDeviceId,
        deviceName: "Host",
        deviceToken: response.deviceToken,
        role: response.role,
        deviceCount: response.deviceCount,
        maxDevices: response.maxDevices,
      });

      setAccessMode(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to create room.");
    } finally {
      setLoading(false);
    }
  }

  // Join an existing room.
  async function handleJoinRoom(roomCode: string, deviceName: string) {
    setLoading(true);
    setError(null);

    try {
      const response = await joinRoomRequest(roomCode, {
        deviceName,
      });

      setDevices([]);

      setRoomSession({
        roomCode: response.roomCode,
        deviceId: response.deviceId,
        deviceName: response.deviceName,
        deviceToken: response.deviceToken,
        role: response.role,
        deviceCount: response.deviceCount,
        maxDevices: response.maxDevices,
      });

      setAccessMode(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to join room.");
    } finally {
      setLoading(false);
    }
  }

  // Update playback permissions for a specific Follower.
  async function handleUpdatePermissions(
    deviceId: string,
    permissions: PlaybackPermission[],
  ) {
    // Only the Host can manage playback permissions.
    if (!roomSession || roomSession.role !== "HOST") {
      return;
    }

    setUpdatingDeviceId(deviceId);
    setError(null);

    try {
      // Save permissions through the backend API.
      const response = await updatePlaybackPermissions(
        roomSession.roomCode,
        deviceId,
        roomSession.deviceToken,
        permissions,
      );

      // Update the device list using the backend response.
      setDevices((currentDevices) =>
        currentDevices.map((device) =>
          device.deviceId === deviceId
            ? {
                ...device,
                permissions: response.permissions,
              }
            : device,
        ),
      );
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to update playback permissions.",
      );
    } finally {
      setUpdatingDeviceId(null);
    }
  }

  // Leave the room or end it if this device is the Host.
  async function handleLeaveRoom() {
    if (!roomSession) return;

    setLoading(true);
    setError(null);

    try {
      if (roomSession.role === "HOST") {
        await endRoom(roomSession.roomCode, roomSession.deviceToken);
      } else {
        await leaveRoom(roomSession.roomCode, roomSession.deviceToken);
      }

      setDevices([]);
      setRoomSession(null);
      setAccessMode(null);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Unable to leave the room.",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <Navbar isRoomActive={roomSession !== null} />

      <main>
        {roomSession ? (
          <>
            <RoomSession
              roomCode={roomSession.roomCode}
              deviceName={roomSession.deviceName}
              role={roomSession.role}
              deviceCount={roomSession.deviceCount}
              maxDevices={roomSession.maxDevices}
              devices={devices}
              connectionStatus={playbackSync.connectionStatus}
              canManagePermissions={isHost}
              updatingDeviceId={updatingDeviceId}
              onUpdatePermissions={handleUpdatePermissions}
              onLeave={() => void handleLeaveRoom()}
            />

            {error && (
              <p
                className="mx-auto mt-4 max-w-7xl px-6 text-sm text-red-600"
                role="alert"
              >
                {error}
              </p>
            )}

            {/* Player provider selection */}
            <section className="mx-auto mt-8 w-full max-w-5xl px-4 sm:px-6">
              <div className="flex flex-wrap gap-3">
                <button
                  type="button"
                  onClick={() => setPlayerMode("YOUTUBE")}
                  aria-pressed={playerMode === "YOUTUBE"}
                  className={`rounded-xl px-5 py-3 text-sm font-semibold transition-colors ${
                    playerMode === "YOUTUBE"
                      ? "bg-blue-600 text-white"
                      : "border border-gray-300 text-gray-700 hover:bg-gray-100 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
                  }`}
                >
                  YouTube
                </button>

                <button
                  type="button"
                  onClick={() => setPlayerMode("LOCAL")}
                  aria-pressed={playerMode === "LOCAL"}
                  className={`rounded-xl px-5 py-3 text-sm font-semibold transition-colors ${
                    playerMode === "LOCAL"
                      ? "bg-blue-600 text-white"
                      : "border border-gray-300 text-gray-700 hover:bg-gray-100 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
                  }`}
                >
                  Local Video
                </button>
              </div>
            </section>

            {/* Render the selected player */}
            {playerMode === "YOUTUBE" ? (
              <YouTubePlayer
                onLocalPlaybackEvent={playbackSync.onLocalPlaybackEvent}
                remoteState={playbackSync.remoteState}
                connectionStatus={playbackSync.connectionStatus}
                syncError={playbackSync.error}
              />
            ) : (
              <LocalMediaPlayer />
            )}
          </>
        ) : (
          <>
            <Hero
              onCreateRoom={() => {
                setError(null);
                setAccessMode("CREATE");
              }}
              onJoinRoom={() => {
                setError(null);
                setAccessMode("JOIN");
              }}
            />

            {accessMode && (
              <RoomAccess
                mode={accessMode}
                loading={loading}
                error={error}
                onCreateRoom={handleCreateRoom}
                onJoinRoom={handleJoinRoom}
                onCancel={() => {
                  setAccessMode(null);
                  setError(null);
                }}
              />
            )}

            <HowItWorks />
            <DeviceRoles />
          </>
        )}
      </main>

      <Footer />
    </>
  );
}
function App() {
  return window.location.pathname === "/stats" ? (
    <StatsPage />
  ) : (
    <FrameHostApp />
  );
}

export default App;

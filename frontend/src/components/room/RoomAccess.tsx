import { useState, type SyntheticEvent } from "react";
type RoomAccessMode = "CREATE" | "JOIN";

interface RoomAccessProps {
  mode: RoomAccessMode;
  loading: boolean;
  error: string | null;
  onCreateRoom: () => Promise<void>;
  onJoinRoom: (roomCode: string, deviceName: string) => Promise<void>;
  onCancel: () => void;
}

function RoomAccess({
  mode,
  loading,
  error,
  onCreateRoom,
  onJoinRoom,
  onCancel,
}: RoomAccessProps) {
  const [roomCode, setRoomCode] = useState("");
  const [deviceName, setDeviceName] = useState("");

  async function handleSubmit(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();

    if (mode === "CREATE") {
      await onCreateRoom();
      return;
    }

    await onJoinRoom(roomCode.trim().toUpperCase(), deviceName.trim());
  }

  return (
    <section
      id="room-access"
      className="bg-gray-50 px-6 py-16"
      aria-labelledby="room-access-title"
    >
      <div className="mx-auto max-w-lg rounded-2xl border border-gray-200 bg-white p-6 shadow-sm sm:p-8">
        <h2 id="room-access-title" className="text-2xl font-bold text-gray-900">
          {mode === "CREATE" ? "Create a room" : "Join a room"}
        </h2>

        <p className="mt-2 text-sm text-gray-600">
          {mode === "CREATE"
            ? "Start a new session and invite others using your room code."
            : "Enter the room code shared by the Host."}
        </p>

        <form onSubmit={handleSubmit} className="mt-6 space-y-5">
          {mode === "JOIN" && (
            <>
              <div>
                <label
                  htmlFor="room-code"
                  className="mb-2 block text-sm font-medium text-gray-700"
                >
                  Room code
                </label>

                <input
                  id="room-code"
                  type="text"
                  value={roomCode}
                  onChange={(event) =>
                    setRoomCode(event.target.value.toUpperCase())
                  }
                  placeholder="Enter room code"
                  maxLength={20}
                  required
                  autoComplete="off"
                  className="w-full rounded-lg border border-gray-300 px-4 py-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                />
              </div>

              <div>
                <label
                  htmlFor="device-name"
                  className="mb-2 block text-sm font-medium text-gray-700"
                >
                  Device name
                </label>

                <input
                  id="device-name"
                  type="text"
                  value={deviceName}
                  onChange={(event) => setDeviceName(event.target.value)}
                  placeholder="e.g. My Laptop"
                  maxLength={30}
                  required
                  className="w-full rounded-lg border border-gray-300 px-4 py-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                />
              </div>
            </>
          )}

          {error && (
            <p
              className="rounded-lg bg-red-50 p-3 text-sm text-red-700"
              role="alert"
            >
              {error}
            </p>
          )}

          <div className="flex flex-col-reverse gap-3 sm:flex-row">
            <button
              type="button"
              onClick={onCancel}
              disabled={loading}
              className="rounded-lg border border-gray-300 px-5 py-3 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={
                loading ||
                (mode === "JOIN" && (!roomCode.trim() || !deviceName.trim()))
              }
              className="flex-1 rounded-lg bg-blue-600 px-5 py-3 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {loading
                ? "Please wait..."
                : mode === "CREATE"
                  ? "Create Room"
                  : "Join Room"}
            </button>
          </div>
        </form>
      </div>
    </section>
  );
}

export default RoomAccess;

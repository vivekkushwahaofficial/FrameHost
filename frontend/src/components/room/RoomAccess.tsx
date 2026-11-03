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
      className="bg-gray-50 px-5 py-16 transition-colors duration-300 dark:bg-[#090F1D] sm:px-6 lg:py-20"
      aria-labelledby="room-access-title"
    >
      <div className="mx-auto max-w-lg overflow-hidden rounded-2xl border border-gray-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-[#111C30] sm:p-8">
        {/* Heading */}
        <div>
          <span className="inline-flex items-center rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-[0.16em] text-blue-700 dark:border-blue-400/20 dark:bg-blue-500/10 dark:text-blue-400">
            {mode === "CREATE" ? "New session" : "Join session"}
          </span>

          <h2
            id="room-access-title"
            className="mt-4 text-2xl font-bold tracking-tight text-gray-900 dark:text-slate-50 sm:text-3xl"
          >
            {mode === "CREATE" ? "Create a room" : "Join a room"}
          </h2>

          <p className="mt-3 text-sm leading-7 text-gray-600 dark:text-slate-300">
            {mode === "CREATE"
              ? "Start a new session and invite others using your room code."
              : "Enter the room code shared by the Host."}
          </p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="mt-7 space-y-5">
          {mode === "JOIN" && (
            <>
              {/* Room code */}
              <div>
                <label
                  htmlFor="room-code"
                  className="mb-2 block text-sm font-semibold text-gray-700 dark:text-slate-300"
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
                  className="w-full rounded-xl border border-gray-300 bg-white px-4 py-3 text-sm text-gray-900 outline-none transition-all placeholder:text-gray-400 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 dark:border-slate-700 dark:bg-slate-900/70 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:border-blue-400 dark:focus:ring-blue-500/10"
                />
              </div>

              {/* Device name */}
              <div>
                <label
                  htmlFor="device-name"
                  className="mb-2 block text-sm font-semibold text-gray-700 dark:text-slate-300"
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
                  className="w-full rounded-xl border border-gray-300 bg-white px-4 py-3 text-sm text-gray-900 outline-none transition-all placeholder:text-gray-400 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 dark:border-slate-700 dark:bg-slate-900/70 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:border-blue-400 dark:focus:ring-blue-500/10"
                />
              </div>
            </>
          )}

          {/* Error message */}
          {error && (
            <p
              className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm leading-6 text-red-700 dark:border-red-500/20 dark:bg-red-500/10 dark:text-red-300"
              role="alert"
            >
              {error}
            </p>
          )}

          {/* Actions */}
          <div className="flex flex-col-reverse gap-3 pt-2 sm:flex-row">
            <button
              type="button"
              onClick={onCancel}
              disabled={loading}
              className="rounded-xl border border-gray-300 px-5 py-3 text-sm font-semibold text-gray-700 transition-colors hover:bg-gray-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800 dark:focus-visible:outline-blue-400"
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={
                loading ||
                (mode === "JOIN" && (!roomCode.trim() || !deviceName.trim()))
              }
              className="flex-1 rounded-xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white shadow-sm shadow-blue-600/20 transition-all hover:bg-blue-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-blue-500 dark:shadow-blue-500/10 dark:hover:bg-blue-400 dark:focus-visible:outline-blue-400"
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

import type { DeviceRole, DeviceResponse } from "../../types/room";
import type { PlaybackPermission } from "../../types/playbackPermissions";

interface RoomSessionProps {
  roomCode: string;
  deviceName: string;
  role: DeviceRole;
  deviceCount: number;
  maxDevices: number;
  connectionStatus: string;
  devices?: DeviceResponse[];
  canManagePermissions: boolean;
  updatingDeviceId: string | null;
  onUpdatePermissions: (
    deviceId: string,
    permissions: PlaybackPermission[],
  ) => void;
  onLeave: () => void;
}

// Available permissions that the Host can assign to Followers.
const PERMISSION_OPTIONS: {
  value: PlaybackPermission;
  label: string;
  description: string;
}[] = [
  {
    value: "PLAY_PAUSE",
    label: "Play / Pause",
    description: "Allow this device to play and pause videos.",
  },
  {
    value: "SEEK",
    label: "Seek",
    description: "Allow this device to change the playback position.",
  },
  {
    value: "CHANGE_VIDEO",
    label: "Change Video",
    description: "Allow this device to load another video.",
  },
];

function RoomSession({
  roomCode,
  deviceName,
  role,
  deviceCount,
  maxDevices,
  connectionStatus,
  devices = [],
  canManagePermissions,
  updatingDeviceId,
  onUpdatePermissions,
  onLeave,
}: RoomSessionProps) {
  const connected = connectionStatus === "CONNECTED";

  // Toggle one permission while preserving the other assigned permissions.
  function handlePermissionChange(
    deviceId: string,
    currentPermissions: PlaybackPermission[],
    permission: PlaybackPermission,
    checked: boolean,
  ) {
    const updatedPermissions = checked
      ? [...currentPermissions, permission]
      : currentPermissions.filter((item) => item !== permission);

    onUpdatePermissions(deviceId, updatedPermissions);
  }

  return (
    <section className="border-b border-gray-200 bg-white px-5 py-7 transition-colors duration-300 dark:border-slate-800 dark:bg-[#090F1D] sm:px-6 sm:py-8">
      <div className="mx-auto max-w-7xl">
        {/* Room information */}
        <div className="flex flex-col justify-between gap-6 sm:flex-row sm:items-center">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-gray-500 dark:text-slate-400">
              Your room
            </p>

            <div className="mt-2 flex flex-wrap items-center gap-3">
              <h2 className="text-2xl font-bold tracking-[0.12em] text-gray-900 dark:text-slate-50 sm:text-3xl">
                {roomCode}
              </h2>

              <button
                type="button"
                onClick={() => {
                  void navigator.clipboard.writeText(roomCode);
                }}
                className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-xs font-semibold text-gray-700 transition-colors hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:border-blue-500/40 dark:hover:bg-blue-500/10 dark:hover:text-blue-400"
              >
                Copy code
              </button>
            </div>

            <p className="mt-3 text-sm text-gray-600 dark:text-slate-300">
              {deviceName}{" "}
              <span className="mx-1 text-gray-400 dark:text-slate-600">·</span>{" "}
              {role}
            </p>
          </div>

          {/* Device count and connection status */}
          <div className="flex flex-wrap items-center gap-5 sm:gap-6">
            <div>
              <p className="text-xs font-medium text-gray-500 dark:text-slate-400">
                Devices
              </p>
              <p className="mt-1 text-sm font-bold text-gray-900 dark:text-slate-100">
                {deviceCount} / {maxDevices}
              </p>
            </div>

            <div>
              <p className="text-xs font-medium text-gray-500 dark:text-slate-400">
                Connection
              </p>

              <p
                className={`mt-1 flex items-center gap-2 text-sm font-semibold ${
                  connected
                    ? "text-green-600 dark:text-emerald-400"
                    : "text-yellow-600 dark:text-amber-400"
                }`}
              >
                <span
                  className={`h-2 w-2 rounded-full ${
                    connected
                      ? "bg-green-500 dark:bg-emerald-400"
                      : "bg-yellow-500 dark:bg-amber-400"
                  }`}
                />
                {connectionStatus}
              </p>
            </div>

            <button
              type="button"
              onClick={onLeave}
              className="rounded-xl border border-red-200 bg-white px-4 py-2.5 text-sm font-semibold text-red-600 transition-colors hover:bg-red-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-500 dark:border-red-500/20 dark:bg-red-500/5 dark:text-red-400 dark:hover:bg-red-500/10"
            >
              Leave room
            </button>
          </div>
        </div>

        {/* Connected device list */}
        <div className="mt-8 border-t border-gray-100 pt-6 dark:border-slate-800">
          <div className="mb-5 flex flex-wrap items-center justify-between gap-2">
            <h3 className="text-sm font-bold text-gray-900 dark:text-slate-100">
              Connected devices
            </h3>

            <span className="rounded-full border border-gray-200 bg-gray-50 px-3 py-1 text-xs font-medium text-gray-600 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-400">
              {devices.length} connected
            </span>
          </div>

          {devices.length > 0 ? (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {devices.map((device) => {
                const isUpdating = updatingDeviceId === device.deviceId;
                const currentPermissions = device.permissions ?? [];

                return (
                  <article
                    key={device.deviceId}
                    className="group rounded-2xl border border-gray-200 bg-white p-5 shadow-sm transition-all duration-300 hover:border-blue-300 hover:shadow-md hover:shadow-blue-500/5 dark:border-slate-800 dark:bg-[#111C30] dark:shadow-none dark:hover:border-blue-500/40 dark:hover:shadow-lg dark:hover:shadow-blue-500/5"
                  >
                    {/* Device information */}
                    <div className="flex items-center justify-between gap-3">
                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold text-gray-900 dark:text-slate-100">
                          {device.deviceName}
                        </p>

                        <p className="mt-1 text-xs text-gray-500 dark:text-slate-400">
                          {device.role}
                        </p>
                      </div>

                      {device.role === "HOST" && (
                        <span className="shrink-0 rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700 dark:border-blue-400/20 dark:bg-blue-500/10 dark:text-blue-400">
                          Host
                        </span>
                      )}
                    </div>

                    {/* Host permission controls */}
                    {canManagePermissions && device.role !== "HOST" && (
                      <div className="mt-5 border-t border-gray-100 pt-4 dark:border-slate-800">
                        <div className="mb-4 flex items-center justify-between gap-2">
                          <h4 className="text-xs font-bold uppercase tracking-wider text-gray-800 dark:text-slate-200">
                            Playback permissions
                          </h4>

                          {isUpdating && (
                            <span
                              className="text-xs font-medium text-blue-600 dark:text-blue-400"
                              role="status"
                            >
                              Saving...
                            </span>
                          )}
                        </div>

                        <div className="space-y-4">
                          {PERMISSION_OPTIONS.map((option) => {
                            const isChecked = currentPermissions.includes(
                              option.value,
                            );

                            return (
                              <label
                                key={option.value}
                                className={`flex items-start gap-3 ${
                                  isUpdating
                                    ? "cursor-not-allowed opacity-60"
                                    : "cursor-pointer"
                                }`}
                              >
                                <input
                                  type="checkbox"
                                  checked={isChecked}
                                  disabled={isUpdating}
                                  onChange={(event) =>
                                    handlePermissionChange(
                                      device.deviceId,
                                      currentPermissions,
                                      option.value,
                                      event.target.checked,
                                    )
                                  }
                                  className="mt-0.5 h-4 w-4 shrink-0 rounded border-gray-300 text-blue-600 accent-blue-600 focus:ring-2 focus:ring-blue-500 dark:border-slate-600 dark:bg-slate-800 dark:accent-blue-500 dark:focus:ring-blue-400"
                                />

                                <span className="min-w-0">
                                  <span className="block text-sm font-medium text-gray-800 dark:text-slate-200">
                                    {option.label}
                                  </span>

                                  <span className="mt-0.5 block text-xs leading-5 text-gray-500 dark:text-slate-400">
                                    {option.description}
                                  </span>
                                </span>
                              </label>
                            );
                          })}
                        </div>

                        <p className="mt-4 text-xs text-gray-500 dark:text-slate-400">
                          Changes are saved automatically.
                        </p>
                      </div>
                    )}

                    {/* Permission summary for viewers */}
                    {!canManagePermissions && device.role !== "HOST" && (
                      <div className="mt-5 border-t border-gray-100 pt-4 dark:border-slate-800">
                        <p className="text-xs font-bold uppercase tracking-wider text-gray-700 dark:text-slate-300">
                          Assigned permissions
                        </p>

                        <p className="mt-2 text-xs leading-6 text-gray-500 dark:text-slate-400">
                          {currentPermissions.length > 0
                            ? PERMISSION_OPTIONS.filter((option) =>
                                currentPermissions.includes(option.value),
                              )
                                .map((option) => option.label)
                                .join(", ")
                            : "No playback permissions"}
                        </p>
                      </div>
                    )}
                  </article>
                );
              })}
            </div>
          ) : (
            <div className="rounded-xl border border-dashed border-gray-300 bg-gray-50 px-4 py-8 text-center dark:border-slate-700 dark:bg-slate-900/50">
              <p className="text-sm text-gray-500 dark:text-slate-400">
                No device information available yet.
              </p>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}

export default RoomSession;

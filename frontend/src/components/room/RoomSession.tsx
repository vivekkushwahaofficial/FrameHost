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
    <section className="border-b border-gray-200 bg-white px-6 py-6">
      <div className="mx-auto max-w-7xl">
        {/* Room information */}
        <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-center">
          <div>
            <p className="text-sm text-gray-500">Your room</p>

            <div className="mt-1 flex flex-wrap items-center gap-3">
              <h2 className="text-2xl font-bold tracking-wider text-gray-900">
                {roomCode}
              </h2>

              <button
                type="button"
                onClick={() => {
                  void navigator.clipboard.writeText(roomCode);
                }}
                className="rounded-md border border-gray-300 px-3 py-1.5 text-xs font-semibold text-gray-700 hover:bg-gray-50"
              >
                Copy code
              </button>
            </div>

            <p className="mt-2 text-sm text-gray-600">
              {deviceName} · {role}
            </p>
          </div>

          {/* Device count and connection status */}
          <div className="flex flex-wrap items-center gap-4">
            <div>
              <p className="text-xs text-gray-500">Devices</p>
              <p className="text-sm font-semibold text-gray-900">
                {deviceCount} / {maxDevices}
              </p>
            </div>

            <div>
              <p className="text-xs text-gray-500">Connection</p>
              <p
                className={`text-sm font-semibold ${
                  connected ? "text-green-600" : "text-yellow-600"
                }`}
              >
                {connectionStatus}
              </p>
            </div>

            <button
              type="button"
              onClick={onLeave}
              className="rounded-lg border border-red-200 px-4 py-2 text-sm font-semibold text-red-600 hover:bg-red-50"
            >
              Leave
            </button>
          </div>
        </div>

        {/* Connected device list */}
        <div className="mt-6 border-t border-gray-100 pt-5">
          <h3 className="mb-3 text-sm font-semibold text-gray-900">
            Connected devices ({devices.length})
          </h3>

          {devices.length > 0 ? (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {devices.map((device) => {
                const isUpdating = updatingDeviceId === device.deviceId;

                const currentPermissions = device.permissions ?? [];

                return (
                  <div
                    key={device.deviceId}
                    className="rounded-lg border border-gray-200 p-4"
                  >
                    {/* Device information */}
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-sm font-semibold text-gray-900">
                          {device.deviceName}
                        </p>

                        <p className="mt-1 text-xs text-gray-500">
                          {device.role}
                        </p>
                      </div>

                      {device.role === "HOST" && (
                        <span className="rounded-full bg-blue-50 px-2 py-1 text-xs font-medium text-blue-700">
                          Host
                        </span>
                      )}
                    </div>

                    {/* Host permission controls */}
                    {canManagePermissions && device.role !== "HOST" && (
                      <div className="mt-4 border-t border-gray-100 pt-3">
                        <div className="mb-3 flex items-center justify-between">
                          <h4 className="text-xs font-semibold text-gray-900">
                            Playback permissions
                          </h4>

                          {isUpdating && (
                            <span className="text-xs text-blue-600">
                              Saving...
                            </span>
                          )}
                        </div>

                        <div className="space-y-3">
                          {PERMISSION_OPTIONS.map((option) => {
                            const isChecked = currentPermissions.includes(
                              option.value,
                            );

                            return (
                              <label
                                key={option.value}
                                className={`flex items-start gap-2 ${
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
                                  className="mt-0.5 h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                />

                                <span>
                                  <span className="block text-sm font-medium text-gray-800">
                                    {option.label}
                                  </span>

                                  <span className="block text-xs text-gray-500">
                                    {option.description}
                                  </span>
                                </span>
                              </label>
                            );
                          })}
                        </div>

                        <p className="mt-3 text-xs text-gray-500">
                          Changes are saved automatically.
                        </p>
                      </div>
                    )}

                    {/* Permission summary for Host and other viewers */}
                    {!canManagePermissions && device.role !== "HOST" && (
                      <div className="mt-3 border-t border-gray-100 pt-3">
                        <p className="text-xs font-semibold text-gray-700">
                          Assigned permissions
                        </p>

                        <p className="mt-1 text-xs text-gray-500">
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
                  </div>
                );
              })}
            </div>
          ) : (
            <p className="text-sm text-gray-500">
              No device information available yet.
            </p>
          )}
        </div>
      </div>
    </section>
  );
}

export default RoomSession;

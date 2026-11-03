package com.framehost.room.controller;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.framehost.common.AuthenticationException;
import com.framehost.common.AuthorizationException;
import com.framehost.common.GlobalExceptionHandler;
import com.framehost.device.DeviceRole;
import com.framehost.device.dto.DeviceManagementResponse;
import com.framehost.device.dto.DeviceResponse;
import com.framehost.device.service.DeviceNotFoundException;
import com.framehost.room.dto.EndRoomResponse;
import com.framehost.room.model.RoomStatus;
import com.framehost.room.service.RoomOperationConflictException;
import com.framehost.room.service.RoomService;

@WebMvcTest(RoomController.class)
@Import(GlobalExceptionHandler.class)
class RoomControllerTest {

    private static final String ROOM_CODE = "ABC123";
    private static final String HOST_AUTH = "Bearer host-token";
    private static final String FOLLOWER_AUTH = "Bearer follower-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @Test
    void shouldReturnDeviceListForHost() throws Exception {

        List<DeviceResponse> devices = List.of(
                new DeviceResponse(
                        "device-1",
                        "Host",
                        DeviceRole.HOST,
                        Instant.parse("2026-11-02T10:00:00Z"),
                        Set.of()
                ),
                new DeviceResponse(
                        "device-2",
                        "Laptop",
                        DeviceRole.FOLLOWER,
                        Instant.parse("2026-11-02T10:05:00Z"),
                        Set.of()
                )
        );

        when(roomService.getDevices(ROOM_CODE, HOST_AUTH))
                .thenReturn(devices);

        mockMvc.perform(
                get("/api/v1/rooms/{roomCode}/devices", ROOM_CODE)
                        .header("Authorization", HOST_AUTH)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("HOST"))
                .andExpect(jsonPath("$[1].role").value("FOLLOWER"))
                .andExpect(jsonPath("$[0].permissions").isEmpty())
                .andExpect(jsonPath("$[1].permissions").isEmpty());
    }

    @Test
    void shouldReturn401WhenAuthenticationIsMissing() throws Exception {

        when(roomService.getDevices(ROOM_CODE, null))
                .thenThrow(new AuthenticationException());

        mockMvc.perform(
                get("/api/v1/rooms/{roomCode}/devices", ROOM_CODE)
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn403WhenFollowerRequestsDeviceList() throws Exception {

        when(roomService.getDevices(ROOM_CODE, FOLLOWER_AUTH))
                .thenThrow(new AuthorizationException("Host access required"));

        mockMvc.perform(
                get("/api/v1/rooms/{roomCode}/devices", ROOM_CODE)
                        .header("Authorization", FOLLOWER_AUTH)
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldAllowFollowerToLeaveRoom() throws Exception {

        DeviceManagementResponse response = new DeviceManagementResponse(
                ROOM_CODE,
                "device-2",
                "Device left successfully",
                1
        );

        when(roomService.leaveRoom(ROOM_CODE, FOLLOWER_AUTH))
                .thenReturn(response);

        mockMvc.perform(
                post("/api/v1/rooms/{roomCode}/leave", ROOM_CODE)
                        .header("Authorization", FOLLOWER_AUTH)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode").value(ROOM_CODE))
                .andExpect(jsonPath("$.deviceId").value("device-2"))
                .andExpect(jsonPath("$.deviceCount").value(1));
    }

    @Test
    void shouldReturn409WhenHostAttemptsToLeave() throws Exception {

        when(roomService.leaveRoom(ROOM_CODE, HOST_AUTH))
                .thenThrow(new RoomOperationConflictException(
                        "Host cannot leave an active room"
                ));

        mockMvc.perform(
                post("/api/v1/rooms/{roomCode}/leave", ROOM_CODE)
                        .header("Authorization", HOST_AUTH)
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldAllowHostToRemoveFollower() throws Exception {

        DeviceManagementResponse response = new DeviceManagementResponse(
                ROOM_CODE,
                "device-2",
                "Device removed successfully",
                1
        );

        when(roomService.removeDevice(
                ROOM_CODE,
                "device-2",
                HOST_AUTH
        )).thenReturn(response);

        mockMvc.perform(
                delete(
                        "/api/v1/rooms/{roomCode}/devices/{deviceId}",
                        ROOM_CODE,
                        "device-2"
                )
                        .header("Authorization", HOST_AUTH)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("device-2"))
                .andExpect(jsonPath("$.deviceCount").value(1));
    }

    @Test
    void shouldReturn403WhenFollowerAttemptsToRemoveDevice() throws Exception {

        when(roomService.removeDevice(
                ROOM_CODE,
                "device-1",
                FOLLOWER_AUTH
        )).thenThrow(new AuthorizationException("Host access required"));

        mockMvc.perform(
                delete(
                        "/api/v1/rooms/{roomCode}/devices/{deviceId}",
                        ROOM_CODE,
                        "device-1"
                )
                        .header("Authorization", FOLLOWER_AUTH)
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldReturn404WhenTargetDeviceDoesNotExist() throws Exception {

        when(roomService.removeDevice(
                ROOM_CODE,
                "unknown-device",
                HOST_AUTH
        )).thenThrow(new DeviceNotFoundException("unknown-device"));

        mockMvc.perform(
                delete(
                        "/api/v1/rooms/{roomCode}/devices/{deviceId}",
                        ROOM_CODE,
                        "unknown-device"
                )
                        .header("Authorization", HOST_AUTH)
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // Host should successfully end an active room.
    @Test
    void shouldAllowHostToEndRoom() throws Exception {

        EndRoomResponse response = new EndRoomResponse(
                ROOM_CODE,
                RoomStatus.ENDED,
                Instant.parse("2026-11-03T00:00:00Z"),
                "Room ended successfully."
        );

        when(roomService.endRoom(ROOM_CODE, HOST_AUTH))
                .thenReturn(response);

        mockMvc.perform(
                delete("/api/v1/rooms/{roomCode}", ROOM_CODE)
                        .header("Authorization", HOST_AUTH)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode").value(ROOM_CODE))
                .andExpect(jsonPath("$.status").value("ENDED"))
                .andExpect(jsonPath("$.endedAt").exists())
                .andExpect(jsonPath("$.message")
                        .value("Room ended successfully."));
    }

    // Missing authentication should return HTTP 401.
    @Test
    void shouldReturn401WhenHostAuthenticationIsMissing() throws Exception {

        when(roomService.endRoom(ROOM_CODE, null))
                .thenThrow(new AuthenticationException());

        mockMvc.perform(
                delete("/api/v1/rooms/{roomCode}", ROOM_CODE)
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
    }

    // Follower should not be allowed to end a room.
    @Test
    void shouldReturn403WhenFollowerAttemptsToEndRoom() throws Exception {

        when(roomService.endRoom(ROOM_CODE, FOLLOWER_AUTH))
                .thenThrow(new AuthorizationException("Host access required"));

        mockMvc.perform(
                delete("/api/v1/rooms/{roomCode}", ROOM_CODE)
                        .header("Authorization", FOLLOWER_AUTH)
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // A revoked Host token must return HTTP 401 after room termination.
    @Test
    void shouldReturn401WhenHostTokenIsRevokedAfterRoomEnds() throws Exception {

        when(roomService.endRoom(ROOM_CODE, HOST_AUTH))
                .thenThrow(new AuthenticationException());

        mockMvc.perform(
                delete("/api/v1/rooms/{roomCode}", ROOM_CODE)
                        .header("Authorization", HOST_AUTH)
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
    }
}

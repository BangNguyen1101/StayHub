package com.quanlynhatro.room.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quanlynhatro.auth.api.AuthExceptionHandler;
import com.quanlynhatro.auth.security.AppUserDetailsService;
import com.quanlynhatro.auth.security.JwtService;
import com.quanlynhatro.auth.security.SecurityConfig;
import com.quanlynhatro.room.service.RoomNotFoundException;
import com.quanlynhatro.room.service.RoomService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

@WebMvcTest(RoomController.class)
@Import({SecurityConfig.class, AuthExceptionHandler.class})
class RoomControllerSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RoomService roomService;

    @MockitoBean private AppUserDetailsService userDetailsService;

    @MockitoBean private JwtService jwtService;

    @Test
    void unauthenticatedCannotAccessAdminRooms() throws Exception {
        mockMvc.perform(get("/api/admin/rooms")).andExpect(status().isUnauthorized());
    }

    @Test
    void tenantCannotAccessAdminRooms() throws Exception {
        mockMvc.perform(get("/api/admin/rooms").with(user("tenant").roles("TENANT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadRoomDetail() throws Exception {
        when(roomService.getById(1L)).thenReturn(roomResponse());

        mockMvc.perform(get("/api/admin/rooms/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode").value("P01"));
    }

    @Test
    void adminCanUpdateRoom() throws Exception {
        when(roomService.update(eq(1L), any(RoomUpdateRequest.class))).thenReturn(roomResponse());

        mockMvc.perform(
                        put("/api/admin/rooms/1")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content(validUpdateJson()))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanUpdateRoomStatusToMaintenance() throws Exception {
        when(roomService.updateStatus(eq(1L), any(RoomStatusUpdateRequest.class)))
                .thenReturn(roomResponse());

        mockMvc.perform(
                        patch("/api/admin/rooms/1/status")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content("{\"status\":\"MAINTENANCE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void tenantCannotMutateRoom() throws Exception {
        mockMvc.perform(
                        post("/api/admin/rooms")
                                .with(user("tenant").roles("TENANT"))
                                .contentType("application/json")
                                .content(validCreateJson()))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        put("/api/admin/rooms/1")
                                .with(user("tenant").roles("TENANT"))
                                .contentType("application/json")
                                .content(validUpdateJson()))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        patch("/api/admin/rooms/1/status")
                                .with(user("tenant").roles("TENANT"))
                                .contentType("application/json")
                                .content("{\"status\":\"MAINTENANCE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotAccessRoomDetailOrMutations() throws Exception {
        mockMvc.perform(get("/api/admin/rooms/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/api/admin/rooms")
                                .contentType("application/json")
                                .content(validCreateJson()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        put("/api/admin/rooms/1")
                                .contentType("application/json")
                                .content(validUpdateJson()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        patch("/api/admin/rooms/1/status")
                                .contentType("application/json")
                                .content("{\"status\":\"MAINTENANCE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanListRoomsWithFiltersAndPagination() throws Exception {
        when(roomService.search(eq("P"), eq("VACANT"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(roomResponse())));

        mockMvc.perform(
                        get("/api/admin/rooms")
                                .param("keyword", "P")
                                .param("status", "VACANT")
                                .param("page", "0")
                                .param("size", "10")
                                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].roomCode").value("P01"));
    }

    @Test
    void adminCanCreateRoom() throws Exception {
        when(roomService.create(any(RoomCreateRequest.class))).thenReturn(roomResponse());

        mockMvc.perform(
                        post("/api/admin/rooms")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content(
                                        """
                                        {
                                          "roomCode":"P01",
                                          "roomName":"Room 01",
                                          "floorNumber":1,
                                          "areaM2":20.00,
                                          "baseRent":3000000.00,
                                          "maxOccupants":2,
                                          "allowsSharing":true,
                                          "notes":""
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").value("P01"));
    }

    @Test
    void invalidCreateRequestReturnsValidationError() throws Exception {
        mockMvc.perform(
                        post("/api/admin/rooms")
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content("{\"roomCode\":\"\",\"floorNumber\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verify(roomService, never()).create(any(RoomCreateRequest.class));
    }

    @Test
    void tenantCanReadAvailableRooms() throws Exception {
        when(roomService.available(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(roomResponse())));

        mockMvc.perform(
                        get("/api/rooms/available")
                                .param("page", "0")
                                .param("size", "10")
                                .with(user("tenant").roles("TENANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("VACANT"));
    }

    @Test
    void unauthenticatedCannotReadAvailableRooms() throws Exception {
        mockMvc.perform(get("/api/rooms/available")).andExpect(status().isUnauthorized());
    }

    @Test
    void notFoundRoomIsMappedToUnifiedError() throws Exception {
        when(roomService.getById(99L)).thenThrow(new RoomNotFoundException(99L));

        mockMvc.perform(get("/api/admin/rooms/99").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    private RoomResponse roomResponse() {
        return new RoomResponse(
                1L,
                "P01",
                "Room 01",
                1,
                new BigDecimal("20.00"),
                new BigDecimal("3000000.00"),
                2,
                true,
                RoomStatus.VACANT.name(),
                null,
                null,
                null);
    }

    private String validCreateJson() {
        return validUpdateJson();
    }

    private String validUpdateJson() {
        return """
               {
                 "roomCode":"P01",
                 "roomName":"Room 01",
                 "floorNumber":1,
                 "areaM2":20.00,
                 "baseRent":3000000.00,
                 "maxOccupants":2,
                 "allowsSharing":true,
                 "notes":""
               }
               """;
    }
}

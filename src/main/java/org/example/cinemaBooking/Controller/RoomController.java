package org.example.cinemaBooking.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Room.CreateRoomRequest;
import org.example.cinemaBooking.DTO.Request.Room.UpdateRoomRequest;
import org.example.cinemaBooking.DTO.Response.Room.RoomResponse;
import org.example.cinemaBooking.Service.Room.RoomService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Room", description = "quản lý phòng chiếu")
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Room.BASE)
public class RoomController {
    RoomService roomService;

    @Operation(summary = "Tạo phòng chiếu mới",
            description = "Tạo một phòng chiếu mới. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RoomResponse> createRoom(@RequestBody @Valid CreateRoomRequest request) {
        RoomResponse response = roomService.createRoom(request);
        log.info("[ROOM_CONTROLLER] Created room with id: {}", response.id());
        return ApiResponse.<RoomResponse>builder().success(true).message("Room created successfully").data(response).build();
    }

    @Operation(summary = "Cập nhật phòng chiếu",
            description = "Cập nhật thông tin phòng chiếu theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<RoomResponse> updateRoom(@PathVariable String id, @RequestBody @Valid UpdateRoomRequest request) {
        RoomResponse response = roomService.updateRoom(id, request);
        log.info("[ROOM_CONTROLLER] Updated room with id: {}", response.id());
        return ApiResponse.<RoomResponse>builder()
                .success(true)
                .message("Room updated successfully")
                .data(response).build();
    }

    @Operation(summary = "Xóa phòng chiếu",
            description = "Xóa phòng chiếu theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRoom(@PathVariable String id) {
        roomService.deleteRoomByID(id);
        log.info("[ROOM_CONTROLLER] Deleted room with id: {}", id);
        return ApiResponse.<Void>builder().success(true).message("Room deleted successfully").build();
    }

    @Operation(summary = "Lấy thông tin phòng chiếu",
            description = "Lấy thông tin phòng chiếu theo ID.")
    @GetMapping("/{id}")
    public ApiResponse<RoomResponse> getRoomById(@PathVariable String id) {
        RoomResponse response = roomService.getRoomByID(id);
        log.info("[ROOM_CONTROLLER] Retrieved room with id: {}", id);
        return ApiResponse.<RoomResponse>builder().success(true).message("Room retrieved successfully").data(response).build();
    }

    @Operation(summary = "Bật/tắt trạng thái phòng chiếu",
            description = "Bật hoặc tắt trạng thái hoạt động của phòng chiếu theo ID. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/toggle-status")
    public ApiResponse<RoomResponse> toggleRoomStatus(@PathVariable String id) {
        roomService.toggleRoomStatus(id);
        log.info("[ROOM_CONTROLLER] Toggled status for room with id: {}", id);
        return ApiResponse.<RoomResponse>builder().success(true).message("Room status toggled successfully").data(roomService.getRoomByID(id)).build();
    }
    @Operation(summary = "Lấy danh sách phòng chiếu theo Cinema",
            description = "Lấy danh sách phân trang các phòng chiếu theo Cinema. Hỗ trợ tìm kiếm theo tên. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/cinema/{cinemaId}")
    public ApiResponse<PageResponse<RoomResponse>> getRoomsByCinema(
            @PathVariable String cinemaId,
            @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size,
                                                               @RequestParam(defaultValue = "createdAt") String sortBy,
                                                               @RequestParam(defaultValue = "desc") String direction
                                                               ) {
        return ApiResponse.<PageResponse<RoomResponse>>builder().
                success(true).message("Rooms retrieved successfully").
                data(roomService.getRoomByCinema(cinemaId ,page, size, sortBy, direction)).build();
    }

    @Operation(summary = "Lấy danh sách phòng chiếu",
            description = "Lấy danh sách phân trang các phòng chiếu. Hỗ trợ tìm kiếm theo tên. Yêu cầu quyền ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping()
    public ApiResponse<PageResponse<RoomResponse>> getAllRooms(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size,
                                                               @RequestParam(defaultValue = "createdAt") String sortBy,
                                                               @RequestParam(defaultValue = "desc") String direction,
                                                               @RequestParam(required = false) String keyword) {
        return ApiResponse.<PageResponse<RoomResponse>>builder().
                success(true).message("Rooms retrieved successfully").
                data(roomService.getAllRooms(page, size, sortBy, direction, keyword)).build();
    }
}
package org.example.cinemaBooking.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.User.ChangeAvatarRequest;
import org.example.cinemaBooking.DTO.Request.User.ChangePasswordRequest;
import org.example.cinemaBooking.DTO.Request.User.CreateUserRequest;
import org.example.cinemaBooking.DTO.Request.User.UpdateProfileRequest;
import org.example.cinemaBooking.DTO.Response.User.UserResponse;
import org.example.cinemaBooking.Service.User.UserService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(ApiPaths.API_V1 + ApiPaths.User.BASE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "User", description = "quản lý người dùng và hồ sơ")
public class UserController {
    UserService userService;

    @Operation(summary = "Lấy thông tin người dùng hiện tại",
            description = "Trả về thông tin hồ sơ của người dùng đang đăng nhập.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(ApiPaths.User.ME)
    public ApiResponse<UserResponse> getCurrentUser() {
        UserResponse userResponse = userService.getMyInfo();
        log.info("[USER CONTROLLER] Get user info for user: {}", userResponse.getUsername());
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .data(userResponse)
                .build();

    }

    @Operation(summary = "Cập nhật hồ sơ",
            description = "Cập nhật thông tin hồ sơ của người dùng đang đăng nhập.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping(ApiPaths.User.ME)
    public ApiResponse<UserResponse> updateCurrentUser(@RequestBody @Valid UpdateProfileRequest request) {
        UserResponse userResponse = userService.updateMyInfo(request);
        log.info("[USER CONTROLLER] Update user info for user: {}", userResponse.getUsername());
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .data(userResponse)
                .build();
    }

    @Operation(summary = "Đổi mật khẩu",
            description = "Cho phép người dùng đang đăng nhập thay đổi mật khẩu của mình.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping(ApiPaths.User.CHANGE_PASSWORD)
    public ApiResponse<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(request);
        log.info("[USER CONTROLLER] Change password for user: {}", request.getOldPassword());
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @Operation(summary = "Cập nhật avatar",
            description = "Cập nhật ảnh đại diện của người dùng.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping(ApiPaths.User.CHANGE_AVATAR)
    public ApiResponse<UserResponse> changeAvatar(@RequestBody @Valid ChangeAvatarRequest request) {
        UserResponse userResponse = userService.changeAvatar(request);
        log.info("[USER CONTROLLER] Change avatar for user: {}", userResponse.getUsername());
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .data(userResponse)
                .build();
    }

    @Operation(summary = "Khóa người dùng (ADMIN)",
            description = "Khóa tài khoản người dùng theo id (chỉ ADMIN).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping(ApiPaths.User.LOCK + "/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> lockUser(@PathVariable String id) {
        userService.lockUser(id);
        log.info("[USER CONTROLLER] Lock user with id: {}", id);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }

    @Operation(summary = "Mở khóa người dùng ",
            description = "Mở khóa tài khoản người dùng theo id.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping(ApiPaths.User.UNLOCK + "/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> unlockUser(@PathVariable String id) {
        userService.unlockUser(id);
        log.info("[USER CONTROLLER] Unlock user");
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }


    @Operation(summary = "Lấy người dùng theo id",
            description = "Lấy thông tin chi tiết của người dùng theo id.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> getUserById(@PathVariable String id) {
        UserResponse userResponse = userService.getUserById(id);
        log.info("[USER CONTROLLER] Get user info for user with id: {}", id);
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .data(userResponse)
                .build();
    }

    @Operation(summary = "Lấy người dùng theo username ",
            description = "Lấy thông tin người dùng theo username.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> getUserByUsername(@PathVariable String username) {
        UserResponse userResponse = userService.getUserByUsername(username);
        log.info("[USER CONTROLLER] Get user info for user with username: {}", username);
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .data(userResponse)
                .build();
    }


    @Operation(summary = "Lấy danh sách người dùng",
            description = "Lấy danh sách người dùng theo phân trang và tìm kiếm.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserResponse>> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size,
                                                               @RequestParam(required = false) String key) {
        PageResponse<UserResponse> pageResponse = userService.getALlUser(page, size, key);
        log.info("[USER CONTROLLER] Get all users with page: {}, size: {}, key {}", page, size, key);
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .success(true)
                .data(pageResponse)
                .build();
    }

    @Operation(summary = "Tạo người dùng (ADMIN)",
            description = "Tạo tài khoản người dùng mới (chỉ ADMIN).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
        UserResponse userResponse = userService.createUser(request);
        log.info("[USER CONTROLLER] Create user with username: {}", userResponse.getUsername());
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .data(userResponse)
                .build();
    }

    @Operation(summary = "Lấy danh sách nhân viên",
            description = "Lấy danh sách nhân viên theo phân trang và tìm kiếm.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(ApiPaths.User.STAFF)
    public ApiResponse<PageResponse<UserResponse>> getAllStaffs(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size,
                                                               @RequestParam(required = false) String key) {
        PageResponse<UserResponse> pageResponse = userService.getALlStaff(page, size, key);
        log.info("[USER CONTROLLER] Get all staffs with page: {}, size: {}, key {}", page, size, key);
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .success(true)
                .data(pageResponse)
                .build();
    }
}

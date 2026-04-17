package org.example.cinemaBooking.Service.User;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.Entity.RoleEntity;
import org.example.cinemaBooking.Entity.UserEntity;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.UserMapper;
import org.example.cinemaBooking.DTO.Request.User.*;
import org.example.cinemaBooking.DTO.Response.User.UserResponse;
import org.example.cinemaBooking.Repository.RoleRepository;
import org.example.cinemaBooking.Repository.UserRepository;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    RoleRepository roleRepository;

    /**
     * Lấy thông tin user hiện tại (từ SecurityContext).
     * Cache theo username để giảm query DB.
     */
    @Cacheable(value = "myInfo", key = "#root.target.getCurrentUsername()")
    public UserResponse getMyInfo() {

        String userName = getCurrentUsername();

        UserEntity user = userRepository
                .findUserEntityByUsername(userName)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        log.info("[USER SERVICE] Get my info: {}", userName);
        return userMapper.toUserResponse(user);
    }

    /**
     * Cập nhật thông tin user hiện tại.
     * Evict cache myInfo để đảm bảo dữ liệu mới.
     */
    @CacheEvict(value = "myInfo", key = "#root.target.getCurrentUsername()")
    public UserResponse updateMyInfo(UpdateProfileRequest request) {

        String userName = getCurrentUsername();

        UserEntity user = userRepository
                .findUserEntityByUsername(userName)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getEmail() != null &&
                userRepository.existsByEmail(request.getEmail()) &&
                !user.getEmail().equals(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        userMapper.updateUser(request, user);
        userRepository.save(user);

        log.info("[USER SERVICE] Update my info: {}", userName);
        return userMapper.toUserResponse(user);
    }

    /**
     * Đổi mật khẩu user hiện tại.
     */
    public void changePassword(ChangePasswordRequest request) {

        String userName = getCurrentUsername();

        UserEntity user = userRepository
                .findUserEntityByUsername(userName)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_OLD_PASSWORD);
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new AppException(ErrorCode.SAME_OLD_PASSWORD);
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("[USER SERVICE] Change password: {}", userName);
    }

    /**
     * Cập nhật avatar user hiện tại.
     * Evict cache myInfo.
     */
    @CacheEvict(value = "myInfo", key = "#root.target.getCurrentUsername()")
    public UserResponse changeAvatar(ChangeAvatarRequest request) {

        String userName = getCurrentUsername();

        UserEntity user = userRepository
                .findUserEntityByUsername(userName)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setAvatarUrl(request.getAvatarUrl());
        userRepository.save(user);

        log.info("[USER SERVICE] Change avatar: {}", userName);
        return userMapper.toUserResponse(user);
    }

    /**
     * Khóa user (admin).
     * Xóa cache user liên quan.
     */
    @CacheEvict(value = {"userById", "userByUsername"}, allEntries = true)
    public void lockUser(String userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setStatus(false);
        userRepository.save(user);

        log.info("[USER SERVICE] Lock user: {}", user.getId());
    }

    /**
     * Mở khóa user (admin).
     */
    @CacheEvict(value = {"userById", "userByUsername"}, allEntries = true)
    public void unlockUser(String userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setStatus(true);
        userRepository.save(user);

        log.info("[USER SERVICE] Unlock user: {}", user.getId());
    }

    /**
     * Lấy user theo ID.
     * Cache giúp giảm query DB.
     */
    @Cacheable(value = "userById", key = "#userId")
    public UserResponse getUserById(String userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toUserResponse(user);
    }

    /**
     * Lấy user theo username.
     */
    @Cacheable(value = "userByUsername", key = "#username")
    public UserResponse getUserByUsername(String username) {

        UserEntity user = userRepository.findUserEntityByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toUserResponse(user);
    }

    /**
     * Lấy danh sách user có phân trang + search.
     * KHÔNG CACHE vì dữ liệu thay đổi liên tục.
     */
    public PageResponse<UserResponse> getAllUser(int page, int size, String key) {

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);

        Page<UserEntity> userPage = userRepository.searchUsers(key, pageable);

        List<UserResponse> items = userPage.getContent()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();

        return PageResponse.<UserResponse>builder()
                .items(items)
                .page(page)
                .size(size)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .build();
    }

    /**
     * Lấy danh sách staff.
     */
    public PageResponse<UserResponse> getAllStaff(int page, int size, String key) {

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);

        Page<UserEntity> userPage = userRepository.searchStaffs(key, pageable);

        List<UserResponse> items = userPage.getContent()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();

        return PageResponse.<UserResponse>builder()
                .items(items)
                .page(page)
                .size(size)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .build();
    }

    /**
     * Tạo user mới (admin).
     */
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(true);

        Set<RoleEntity> roles = request.getRoles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXIST)))
                .collect(Collectors.toSet());

        user.setRoles(roles);

        userRepository.save(user);

        log.info("[USER SERVICE] Admin created user: {}", user.getUsername());

        return userMapper.toUserResponse(user);
    }

    /**
     * Helper: lấy username từ SecurityContext.
     */
    private String getCurrentUsername() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }
}
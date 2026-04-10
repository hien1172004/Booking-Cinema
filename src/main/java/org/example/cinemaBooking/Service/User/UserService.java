package org.example.cinemaBooking.Service.User;


import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.Entity.RoleEntity;
import org.example.cinemaBooking.Entity.UserEntity;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.UserMapper;
import org.example.cinemaBooking.DTO.Request.User.ChangeAvatarRequest;
import org.example.cinemaBooking.DTO.Request.User.ChangePasswordRequest;
import org.example.cinemaBooking.DTO.Request.User.CreateUserRequest;
import org.example.cinemaBooking.DTO.Request.User.UpdateProfileRequest;
import org.example.cinemaBooking.DTO.Response.User.UserResponse;
import org.example.cinemaBooking.Repository.RoleRepository;
import org.example.cinemaBooking.Repository.UserRepository;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(
        level = lombok.AccessLevel.PRIVATE,
        makeFinal = true
)
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    RoleRepository roleRepository;

    public UserResponse getMyInfo(){
        var userName = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        UserEntity user = userRepository
                .findUserEntityByUsername(userName)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        log.info("[USER SERVICE] Get user info for user: {}", userName);
        return userMapper.toUserResponse(user);
    }

    public UserResponse updateMyInfo(UpdateProfileRequest request){
        var userName = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        UserEntity user = userRepository
                .findUserEntityByUsername(userName)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if(request.getEmail() != null &&
                userRepository.existsByEmail(request.getEmail()) && !user.getEmail().equals(request.getEmail())){
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }
        userMapper.updateUser(request, user);

        userRepository.save(user);
        log.info("[USER SERVICE] Update user info for user: {}", userName);
        return userMapper.toUserResponse(user);
    }

    public  void changePassword(ChangePasswordRequest request){
        var userName = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        UserEntity user = userRepository
                .findUserEntityByUsername(userName)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if(!passwordEncoder.matches(request.getOldPassword(), user.getPassword())){
            throw new AppException(ErrorCode.PASSWORD_SAME_AS_OLD);
        }
        if(!request.getNewPassword().equals(request.getConfirmPassword())){
            throw new AppException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("[USER SERVICE] Change password for user: {}", userName);
    }

    public UserResponse changeAvatar(ChangeAvatarRequest request){
        var userName = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        UserEntity user = userRepository
                .findUserEntityByUsername(userName)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setAvatarUrl(request.getAvatarUrl());
        userRepository.save(user);
        log.info("[USER SERVICE] Change avatar for user: {}", userName);
        return userMapper.toUserResponse(user);
    }

    public void lockUser(String userId) {
        UserEntity user = userRepository
                .findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setStatus(false);
        userRepository.save(user);
        log.info("[USER SERVICE] Lock user: {}", user.getId());

    }

    public void unlockUser(String userId) {
        UserEntity user = userRepository
                .findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setStatus(true);
        userRepository.save(user);
        log.info("[USER SERVICE] Unlock user: {}", user.getId());

    }

    public UserResponse getUserById(String userId) {
        UserEntity user = userRepository
                .findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        log.info("[USER SERVICE] Get user info for user: {}", user.getUsername());
        return userMapper.toUserResponse(user);
    }

    public UserResponse getUserByUsername(String username) {
        UserEntity user = userRepository
                .findUserEntityByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        log.info("[USER SERVICE] Get user info for user: {}", username);
        return userMapper.toUserResponse(user);
    }

    public PageResponse<UserResponse> getALlUser(int page, int size, String key) {
        int pageNumber = 0;
        if(page > 0){
            pageNumber = page - 1;
        }
        Pageable pageable = PageRequest.of(pageNumber, size);
        Page<UserEntity> userPage  = userRepository.searchUsers(key, pageable);

        List<UserResponse> userResponses = userPage.getContent().stream()
                .map(userMapper::toUserResponse)
                .toList();
        log.info("[USER SERVICE] Get all users with key: {}, page: {}, size: {}", key, page, size);
        return PageResponse.<UserResponse>builder()
                .items(userResponses)
                .page(page)
                .size(size)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .build();
    }

    public PageResponse<UserResponse> getALlStaff(int page, int size, String key) {
        int pageNumber = 0;
        if(page > 0){
            pageNumber = page - 1;
        }
        Pageable pageable = PageRequest.of(pageNumber, size);
        Page<UserEntity> userPage  = userRepository.searchStaffs(key, pageable);
        List<UserResponse> userResponses = userPage.getContent().stream()
                .map(userMapper::toUserResponse)
                .toList();
        log.info("[USER SERVICE] Get all staff with key: {}, page: {}, size: {}", key, page, size);
        return PageResponse.<UserResponse>builder()
                .items(userResponses)
                .page(page)
                .size(size)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .build();
    }

    public UserResponse createUser(CreateUserRequest request){

        if(userRepository.existsByUsername(request.getUsername())){
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        if(userRepository.existsByEmail(request.getEmail())){
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
}

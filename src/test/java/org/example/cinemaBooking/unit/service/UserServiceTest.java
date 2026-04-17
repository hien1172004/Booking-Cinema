package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Request.User.ChangeAvatarRequest;
import org.example.cinemaBooking.DTO.Request.User.ChangePasswordRequest;
import org.example.cinemaBooking.DTO.Request.User.CreateUserRequest;
import org.example.cinemaBooking.DTO.Request.User.UpdateProfileRequest;
import org.example.cinemaBooking.DTO.Response.User.UserResponse;
import org.example.cinemaBooking.Entity.RoleEntity;
import org.example.cinemaBooking.Entity.UserEntity;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.UserMapper;
import org.example.cinemaBooking.Repository.RoleRepository;
import org.example.cinemaBooking.Repository.UserRepository;
import org.example.cinemaBooking.Service.User.UserService;
import org.example.cinemaBooking.Shared.enums.Gender;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserMapper userMapper;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    RoleRepository roleRepository;

    @InjectMocks
    UserService userService;

    @Mock
    Authentication authentication;

    @Mock
    SecurityContext securityContext;

    private final String TEST_USERNAME = "testuser";
    private final String TEST_USER_ID = "user-123";
    private final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    private void mockSecurityContext(String username) {
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn(username);
    }

    // --- Helper Methods ---

    private UserEntity createMockUserEntity() {
        UserEntity user = UserEntity.builder()
                .username(TEST_USERNAME)
                .email(TEST_EMAIL)
                .password("encoded_old_password")
                .fullName("Test User")
                .phone("0123456789")
                .dob(LocalDate.of(1995, 5, 20))
                .gender(Gender.MALE)
                .status(true)
                .avatarUrl("https://example.com/old-avatar.png")
                .build();
        user.setId(TEST_USER_ID);
        return user;
    }

    private UserResponse createMockUserResponse() {
        return UserResponse.builder()
                .id(TEST_USER_ID)
                .username(TEST_USERNAME)
                .email(TEST_EMAIL)
                .fullName("Test User")
                .phone("0123456789")
                .status(true)
                .build();
    }

    // --- Test Groups ---

    @Nested
    @DisplayName("GetMyInfo Tests")
    class GetMyInfoTests {

        @Test
        @DisplayName("Should return user info when user is authenticated and exists")
        void givenAuthenticatedUser_whenGetMyInfo_thenReturnsUserResponse() {
            // Given
            mockSecurityContext(TEST_USERNAME);
            UserEntity user = createMockUserEntity();
            UserResponse response = createMockUserResponse();

            when(userRepository.findUserEntityByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
            when(userMapper.toUserResponse(user)).thenReturn(response);

            // When
            UserResponse result = userService.getMyInfo();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(TEST_USERNAME);
            verify(userRepository).findUserEntityByUsername(TEST_USERNAME);
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND when authenticated user no longer exists in DB")
        void givenAuthenticatedUserNotExists_whenGetMyInfo_thenThrowsAppException() {
            // Given
            mockSecurityContext(TEST_USERNAME);
            when(userRepository.findUserEntityByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.getMyInfo())
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("UpdateMyInfo Tests")
    class UpdateMyInfoTests {

        @Test
        @DisplayName("Should update info successfully when email is unchanged")
        void givenValidRequestSameEmail_whenUpdateMyInfo_thenReturnsUpdatedResponse() {
            // Given
            mockSecurityContext(TEST_USERNAME);
            UserEntity user = createMockUserEntity();
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .email(TEST_EMAIL) // Same email
                    .fullName("New Name")
                    .build();
            UserResponse response = createMockUserResponse();
            response.setFullName("New Name");

            when(userRepository.findUserEntityByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
            // Mock existsByEmail to true so the third condition (!equals) is executed
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);
            when(userMapper.toUserResponse(user)).thenReturn(response);

            // When
            UserResponse result = userService.updateMyInfo(request);

            // Then
            assertThat(result.getFullName()).isEqualTo("New Name");
            verify(userRepository).existsByEmail(TEST_EMAIL);
            verify(userMapper).updateUser(request, user);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should update info successfully when email is changed and not taken")
        void givenValidRequestNewEmail_whenUpdateMyInfo_thenReturnsUpdatedResponse() {
            // Given
            mockSecurityContext(TEST_USERNAME);
            UserEntity user = createMockUserEntity();
            String newEmail = "new@example.com";
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .email(newEmail)
                    .build();

            when(userRepository.findUserEntityByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail(newEmail)).thenReturn(false);
            when(userMapper.toUserResponse(user)).thenReturn(createMockUserResponse());

            // When
            userService.updateMyInfo(request);

            // Then
            verify(userRepository).existsByEmail(newEmail);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should throw EMAIL_EXISTED when updating to an email already owned by another user")
        void givenExistentEmail_whenUpdateMyInfo_thenThrowsAppException() {
            // Given
            mockSecurityContext(TEST_USERNAME);
            UserEntity user = createMockUserEntity();
            String takenEmail = "taken@example.com";
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .email(takenEmail)
                    .build();

            when(userRepository.findUserEntityByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail(takenEmail)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.updateMyInfo(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_EXISTED);
        }

        @Test
        @DisplayName("Should skip email check when email is null in request")
        void givenNullEmail_whenUpdateMyInfo_thenSavesSuccessfully() {
            // Given
            mockSecurityContext(TEST_USERNAME);
            UserEntity user = createMockUserEntity();
            UpdateProfileRequest request = UpdateProfileRequest.builder().email(null).build();
            when(userRepository.findUserEntityByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
            when(userMapper.toUserResponse(user)).thenReturn(createMockUserResponse());

            // When
            userService.updateMyInfo(request);

            // Then
            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND when user does not exist during update")
        void givenNonExistentUser_whenUpdateMyInfo_thenThrowsAppException() {
            // Given
            mockSecurityContext(TEST_USERNAME);
            when(userRepository.findUserEntityByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.updateMyInfo(new UpdateProfileRequest()))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("ChangePassword Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully with valid request")
        void givenValidRequest_whenChangePassword_thenSavesNewEncodedPassword() {
            // Given
            mockSecurityContext(TEST_USERNAME);
            UserEntity user = createMockUserEntity();
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .oldPassword("old_pass")
                    .newPassword("new_pass123")
                    .confirmPassword("new_pass123")
                    .build();

            when(userRepository.findUserEntityByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("old_pass", user.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("new_pass123")).thenReturn("encoded_new_pass");

            // When
            userService.changePassword(request);

            // Then
            assertThat(user.getPassword()).isEqualTo("encoded_new_pass");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should throw INVALID_OLD_PASSWORD when old password matches fail")
        void givenWrongOldPassword_whenChangePassword_thenThrowsAppException() {
            // Given
            mockSecurityContext(TEST_USERNAME);
            UserEntity user = createMockUserEntity();
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .oldPassword("wrong_pass")
                    .build();

            when(userRepository.findUserEntityByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong_pass", user.getPassword())).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> userService.changePassword(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_OLD_PASSWORD);
        }

        @Test
        @DisplayName("Should throw SAME_OLD_PASSWORD when new password is identical to old one")
        void givenSameNewPassword_whenChangePassword_thenThrowsAppException() {
            // Given
            mockSecurityContext(TEST_USERNAME);
            UserEntity user = createMockUserEntity();
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .oldPassword("same_pass")
                    .newPassword("same_pass")
                    .build();

            when(userRepository.findUserEntityByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("same_pass", user.getPassword())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.changePassword(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SAME_OLD_PASSWORD);
        }

        @Test
        @DisplayName("Should throw PASSWORD_CONFIRM_NOT_MATCH when confirm password differs")
        void givenMismatchConfirmPassword_whenChangePassword_thenThrowsAppException() {
            // Given
            mockSecurityContext(TEST_USERNAME);
            UserEntity user = createMockUserEntity();
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .oldPassword("atest")
                    .newPassword("new1")
                    .confirmPassword("new2")
                    .build();

            when(userRepository.findUserEntityByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.changePassword(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND when user does not exist during password change")
        void givenNonExistentUser_whenChangePassword_thenThrowsAppException() {
            // Given
            mockSecurityContext(TEST_USERNAME);
            when(userRepository.findUserEntityByUsername(anyString())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.changePassword(new ChangePasswordRequest()))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Lock & Unlock User Tests")
    class AccountStatusTests {

        @Test
        @DisplayName("Should set status to false when locking user")
        void givenUserId_whenLockUser_thenStatusIsFalse() {
            // Given
            UserEntity user = createMockUserEntity();
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

            // When
            userService.lockUser(TEST_USER_ID);

            // Then
            assertThat(user.isStatus()).isFalse();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should set status to true when unlocking user")
        void givenUserId_whenUnlockUser_thenStatusIsTrue() {
            // Given
            UserEntity user = createMockUserEntity();
            user.setStatus(false);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

            // When
            userService.unlockUser(TEST_USER_ID);

            // Then
            assertThat(user.isStatus()).isTrue();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND when user does not exist during lock/unlock")
        void givenNonExistentUser_whenLockOrUnlock_thenThrowsAppException() {
            // Given
            when(userRepository.findById(anyString())).thenReturn(Optional.empty());

            // When & Then (Lock)
            assertThatThrownBy(() -> userService.lockUser("none"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            // When & Then (Unlock)
            assertThatThrownBy(() -> userService.unlockUser("none"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Change Avatar Tests")
    class ChangeAvatarTests {

        @Test
        @DisplayName("Should change avatar successfully")
        void givenAvatarRequest_whenChangeAvatar_thenSavesNewAvatarUrl() {
            // Given
            mockSecurityContext(TEST_USERNAME);
            UserEntity user = createMockUserEntity();
            String newAvatar = "https://example.com/new.png";
            ChangeAvatarRequest request = new ChangeAvatarRequest(newAvatar);

            when(userRepository.findUserEntityByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
            when(userMapper.toUserResponse(user)).thenReturn(createMockUserResponse());

            // When
            userService.changeAvatar(request);

            // Then
            assertThat(user.getAvatarUrl()).isEqualTo(newAvatar);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND when user does not exist during avatar change")
        void givenNonExistentUser_whenChangeAvatar_thenThrowsAppException() {
            // Given
            mockSecurityContext(TEST_USERNAME);
            when(userRepository.findUserEntityByUsername(anyString())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.changeAvatar(new ChangeAvatarRequest("any")))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Get User Detail Tests")
    class GetUserDetailTests {

        @Test
        @DisplayName("Should return user when searching by ID")
        void givenUserId_whenGetUserById_thenReturnsUserResponse() {
            // Given
            UserEntity user = createMockUserEntity();
            UserResponse response = createMockUserResponse();
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toUserResponse(user)).thenReturn(response);

            // When
            UserResponse result = userService.getUserById(TEST_USER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should return user when searching by Username")
        void givenUsername_whenGetUserByUsername_thenReturnsUserResponse() {
            // Given
            UserEntity user = createMockUserEntity();
            UserResponse response = createMockUserResponse();
            when(userRepository.findUserEntityByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
            when(userMapper.toUserResponse(user)).thenReturn(response);

            // When
            UserResponse result = userService.getUserByUsername(TEST_USERNAME);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(TEST_USERNAME);
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND when searching by non-existent ID")
        void givenInvalidId_whenGetUserById_thenThrowsAppException() {
            when(userRepository.findById(anyString())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.getUserById("none"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND when searching by non-existent Username")
        void givenInvalidUsername_whenGetUserByUsername_thenThrowsAppException() {
            when(userRepository.findUserEntityByUsername(anyString())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.getUserByUsername("none"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Get User List Tests")
    class UserListTests {

        @Test
        @DisplayName("Should return paged user responses")
        void givenPageParams_whenGetAllUser_thenReturnsPageResponse() {
            // Given
            UserEntity user = createMockUserEntity();
            Page<UserEntity> page = new PageImpl<>(List.of(user));
            when(userRepository.searchUsers(anyString(), any(Pageable.class))).thenReturn(page);
            when(userMapper.toUserResponse(user)).thenReturn(createMockUserResponse());

            // When
            PageResponse<UserResponse> result = userService.getAllUser(1, 10, "keyword");

            // Then
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getPage()).isEqualTo(1);
            verify(userRepository).searchUsers(eq("keyword"), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty result when no users found")
        void givenNoMatch_whenGetAllUser_thenReturnsEmptyItems() {
            // Given
            Page<UserEntity> emptyPage = Page.empty();
            when(userRepository.searchUsers(anyString(), any(Pageable.class))).thenReturn(emptyPage);

            // When
            PageResponse<UserResponse> result = userService.getAllUser(1, 10, "none");

            // Then
            assertThat(result.getItems()).isEmpty();
        }

        @Test
        @DisplayName("Should handle page number less than 1 correctly")
        void givenPageZero_whenGetAllUser_thenUsesPageZero() {
            // Given
            Page<UserEntity> emptyPage = Page.empty();
            when(userRepository.searchUsers(anyString(), any(Pageable.class))).thenReturn(emptyPage);

            // When
            userService.getAllUser(0, 10, "test");

            // Then
            verify(userRepository).searchUsers(anyString(), argThat(p -> p.getPageNumber() == 0));
        }

        @Test
        @DisplayName("Should return paged staff responses")
        void givenPageParams_whenGetAllStaff_thenReturnsPageResponse() {
            // Given
            UserEntity staff = createMockUserEntity();
            Page<UserEntity> page = new PageImpl<>(List.of(staff));
            when(userRepository.searchStaffs(anyString(), any(Pageable.class))).thenReturn(page);
            when(userMapper.toUserResponse(staff)).thenReturn(createMockUserResponse());

            // When
            PageResponse<UserResponse> result = userService.getAllStaff(1, 10, "staff");

            // Then
            assertThat(result.getItems()).hasSize(1);
            verify(userRepository).searchStaffs(eq("staff"), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user successfully with valid data and role")
        void givenValidData_whenCreateUser_thenSavesUserWithEncodedPassword() {
            // Given
            CreateUserRequest request = CreateUserRequest.builder()
                    .username("new_boss")
                    .email("boss@cinema.com")
                    .password("secret")
                    .roles(Set.of("ADMIN"))
                    .build();
            RoleEntity adminRole = RoleEntity.builder().name("ADMIN").build();

            when(userRepository.existsByUsername("new_boss")).thenReturn(false);
            when(userRepository.existsByEmail("boss@cinema.com")).thenReturn(false);
            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
            when(passwordEncoder.encode("secret")).thenReturn("encoded_secret");

            // When
            userService.createUser(request);

            // Then
            verify(userRepository).save(argThat(u ->
                u.getUsername().equals("new_boss") &&
                u.getPassword().equals("encoded_secret") &&
                u.getRoles().contains(adminRole)
            ));
        }

        @Test
        @DisplayName("Should throw USER_EXISTED when username is already taken")
        void givenExistingUsername_whenCreateUser_thenThrowsAppException() {
            // Given
            CreateUserRequest request = CreateUserRequest.builder().username("exists").build();
            when(userRepository.existsByUsername("exists")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_EXISTED);
        }

        @Test
        @DisplayName("Should throw ROLE_NOT_EXIST when provided role name is invalid")
        void givenInvalidRole_whenCreateUser_thenThrowsAppException() {
            // Given
            CreateUserRequest request = CreateUserRequest.builder()
                    .username("new")
                    .email("new@mail.com")
                    .roles(Set.of("GOD_MODE"))
                    .build();

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(roleRepository.findByName("GOD_MODE")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_NOT_EXIST);
        }

        @Test
        @DisplayName("Should throw EMAIL_EXISTED when email is already taken")
        void givenExistingEmail_whenCreateUser_thenThrowsAppException() {
            // Given
            CreateUserRequest request = CreateUserRequest.builder()
                    .username("new")
                    .email("exists@mail.com")
                    .build();

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail("exists@mail.com")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_EXISTED);
        }
    }
}

package org.example.cinemaBooking.unit.service;

import com.nimbusds.jwt.SignedJWT;
import org.example.cinemaBooking.DTO.Request.Auth.*;
import org.example.cinemaBooking.DTO.Request.User.RegisterRequest;
import org.example.cinemaBooking.DTO.Response.User.UserInfoResponse;
import org.example.cinemaBooking.Entity.RoleEntity;
import org.example.cinemaBooking.Entity.UserEntity;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.UserMapper;
import org.example.cinemaBooking.Repository.RoleRepository;
import org.example.cinemaBooking.Repository.UserRepository;
import org.example.cinemaBooking.Service.Auth.AuthService;
import org.example.cinemaBooking.Service.redis.TokenBlacklistService;
import org.example.cinemaBooking.Shared.constant.PredefinedRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String USERNAME = "test";
    private static final String PASSWORD = "password";

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = new byte[64];
        new SecureRandom().nextBytes(keyBytes);
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);

        ReflectionTestUtils.setField(authService, "signerKey", base64Key);
        ReflectionTestUtils.setField(authService, "validDuration", 3600L);
        ReflectionTestUtils.setField(authService, "refreshableDuration", 86400L);

        RoleEntity role = new RoleEntity();
        role.setName("CUSTOMER");

        user = new UserEntity();
        user.setUsername(USERNAME);
        user.setPassword("encoded");
        user.setStatus(true);
        user.setRoles(Set.of(role));
    }

    // ================= LOGIN =================

    @Test
    void login_shouldReturnToken_whenValid() {
        when(userRepository.findUserEntityByUsername(USERNAME))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, "encoded"))
                .thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setUsername(USERNAME);
        request.setPassword(PASSWORD);

        var res = authService.loginUser(request);

        assertTrue(res.isSuccess());
        assertNotNull(res.getAccessToken());

        verify(userRepository).findUserEntityByUsername(USERNAME);
        verify(passwordEncoder).matches(PASSWORD, "encoded");
    }

    @Test
    void login_shouldThrow_whenPasswordInvalid() {
        when(userRepository.findUserEntityByUsername(USERNAME))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded"))
                .thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setUsername(USERNAME);
        request.setPassword("wrong");

        var ex = assertThrows(AppException.class,
                () -> authService.loginUser(request));

        assertEquals(ErrorCode.PASSWORD_INVALID, ex.getErrorCode());
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        when(userRepository.findUserEntityByUsername("nope"))
                .thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setUsername("nope");
        request.setPassword("123");

        assertThrows(AppException.class,
                () -> authService.loginUser(request));
    }

    @Test
    void login_shouldThrow_whenUserBanned() {
        user.setStatus(false);

        when(userRepository.findUserEntityByUsername(USERNAME))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any()))
                .thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setUsername(USERNAME);
        request.setPassword(PASSWORD);

        var ex = assertThrows(AppException.class,
                () -> authService.loginUser(request));

        assertEquals(ErrorCode.USER_BANNED, ex.getErrorCode());
    }

    // ================= REGISTER =================

    @Test
    void register_shouldSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .username("test1")
                .password("123456")
                .confirmPassword("123456")
                .build();

        UserEntity entity = new UserEntity();
        entity.setUsername("test1");
        entity.setPassword("123456");
        RoleEntity role = new RoleEntity();
        role.setName(PredefinedRole.CUSTOMER_ROLE);

        when(userRepository.existsByUsername("test1")).thenReturn(false);
        when(userMapper.toUserEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        when(roleRepository.findByName(PredefinedRole.CUSTOMER_ROLE))
                .thenReturn(Optional.of(role));
        when(userMapper.toUserInfoResponse(any()))
                .thenReturn(new UserInfoResponse());

        var res = authService.registerUser(request);

        assertNotNull(res.getToken());
        assertNotNull(res.getUserInfoResponse());
        assertTrue(entity.getRoles().contains(role));

        verify(userRepository).save(entity);
    }

    @Test
    void register_shouldThrow_whenUsernameExists() {
        when(userRepository.existsByUsername("test"))
                .thenReturn(true);

        RegisterRequest request = RegisterRequest.builder()
                .username("test")
                .password("123")
                .confirmPassword("123")
                .build();

        var ex = assertThrows(AppException.class,
                () -> authService.registerUser(request));

        assertEquals(ErrorCode.USER_EXISTED, ex.getErrorCode());
    }

    @Test
    void register_shouldThrow_whenPasswordMismatch() {
        when(userRepository.existsByUsername("test"))
                .thenReturn(false);

        RegisterRequest request = RegisterRequest.builder()
                .username("test")
                .password("123")
                .confirmPassword("456")
                .build();

        var ex = assertThrows(AppException.class,
                () -> authService.registerUser(request));

        assertEquals(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH, ex.getErrorCode());
    }

    // ================= TOKEN =================

    @Test
    void verifyToken_shouldSuccess() throws Exception {
        String token = authService.generateToken(user);

        when(tokenBlacklistService.isBlacklisted(any()))
                .thenReturn(false);

        SignedJWT jwt = authService.verifyToken(token, false);

        assertEquals(USERNAME, jwt.getJWTClaimsSet().getSubject());
    }

    @Test
    void verifyToken_shouldThrow_whenBlacklisted() throws Exception {
        String token = authService.generateToken(user);

        var jwt = SignedJWT.parse(token);
        String jwtId = jwt.getJWTClaimsSet().getJWTID();

        when(tokenBlacklistService.isBlacklisted(jwtId))
                .thenReturn(true);

        assertThrows(AppException.class,
                () -> authService.verifyToken(token, false));
    }

    @Test
    void verifyToken_shouldThrow_whenExpired(){
        ReflectionTestUtils.setField(authService, "validDuration", -1L);

        String token = authService.generateToken(user);

        assertThrows(AppException.class,
                () -> authService.verifyToken(token, false));
    }


    @Test
    void verifyToken_shouldWorkInRefreshMode() throws Exception {
        String token = authService.generateToken(user);

        when(tokenBlacklistService.isBlacklisted(any()))
                .thenReturn(false);

        SignedJWT jwt = authService.verifyToken(token, true);

        assertEquals(USERNAME, jwt.getJWTClaimsSet().getSubject());
    }

    // ================= REFRESH =================

    @Test
    void refreshToken_shouldSuccess() throws Exception {
        String token = authService.generateToken(user);

        when(tokenBlacklistService.isBlacklisted(any()))
                .thenReturn(false);
        when(userRepository.findUserEntityByUsername(USERNAME))
                .thenReturn(Optional.of(user));

        RefreshRequest request = new RefreshRequest();
        request.setToken(token);

        var res = authService.refreshToken(request);

        assertNotNull(res.getToken());

        verify(tokenBlacklistService).blacklist(any(), any());
        verify(userRepository).findUserEntityByUsername(USERNAME);
    }

    @Test
    void refreshToken_shouldThrow_whenExpired() {
        ReflectionTestUtils.setField(authService, "refreshableDuration", -1L);

        String token = authService.generateToken(user);

        RefreshRequest request = new RefreshRequest();
        request.setToken(token);

        assertThrows(AppException.class,
                () -> authService.refreshToken(request));
    }

    // ================= LOGOUT =================

    @Test
    void logout_shouldBlacklistToken() throws Exception {
        String token = authService.generateToken(user);

        when(tokenBlacklistService.isBlacklisted(any()))
                .thenReturn(false);

        LogoutRequest request = new LogoutRequest();
        request.setToken(token);

        authService.logout(request);

        verify(tokenBlacklistService).blacklist(any(), any());
    }
    @Test
    void logout_shouldSkipBlacklist_whenTokenExpired() throws Exception {
        String token = authService.generateToken(user);

        AuthService spyService = spy(authService);

        // giả lập token expired -> verifyToken ném AppException
        doThrow(new AppException(ErrorCode.UNAUTHENTICATED))
                .when(spyService).verifyToken(token, false);

        LogoutRequest request = new LogoutRequest();
        request.setToken(token);

        // không throw exception
        assertDoesNotThrow(() -> spyService.logout(request));

        // KHÔNG blacklist
        verify(tokenBlacklistService, never()).blacklist(any(), any());
    }

    @Test
    void introspect_shouldReturnTrue_whenTokenValid() throws Exception {
        String token = authService.generateToken(user);

        when(tokenBlacklistService.isBlacklisted(any()))
                .thenReturn(false);

        IntrospectReq req = new IntrospectReq();
        req.setToken(token);

        var res = authService.introspect(req);

        assertTrue(res.isValid());
    }

    @Test
    void introspect_shouldReturnFalse_whenTokenInvalid() {
        IntrospectReq req = new IntrospectReq();
        req.setToken("invalid-token");

        var res = authService.introspect(req);

        assertFalse(res.isValid());
    }

    @Test
    void introspect_shouldReturnFalse_whenTokenBlacklisted() throws Exception {
        String token = authService.generateToken(user);

        SignedJWT jwt = SignedJWT.parse(token);
        String jwtId = jwt.getJWTClaimsSet().getJWTID();

        when(tokenBlacklistService.isBlacklisted(jwtId))
                .thenReturn(true);

        IntrospectReq req = new IntrospectReq();
        req.setToken(token);

        var res = authService.introspect(req);

        assertFalse(res.isValid());
    }
}
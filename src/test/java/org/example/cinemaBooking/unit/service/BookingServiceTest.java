package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Request.Booking.CreateBookingRequest;
import org.example.cinemaBooking.DTO.Response.Booking.BookingResponse;
import org.example.cinemaBooking.DTO.Response.ValidationResultResponse;
import org.example.cinemaBooking.Entity.*;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.BookingMapper;
import org.example.cinemaBooking.Repository.*;
import org.example.cinemaBooking.Service.Booking.BookingService;
import org.example.cinemaBooking.Service.Promotion.PromotionService;
import org.example.cinemaBooking.Service.Showtime.ShowTimeSeatService;
import org.example.cinemaBooking.Shared.enums.BookingStatus;
import org.example.cinemaBooking.Shared.enums.SeatStatus;
import org.example.cinemaBooking.Shared.enums.ShowTimeStatus;
import org.example.cinemaBooking.Shared.enums.TicketStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

        @Mock
        private BookingRepository bookingRepository;
        @Mock
        private ShowtimeSeatRepository showtimeSeatRepository;
        @Mock
        private ShowtimeRepository showtimeRepository;
        @Mock
        private PromotionRepository promotionRepository;
        @Mock
        private ProductRepository productRepository;
        @Mock
        private ComboRepository comboRepository;
        @Mock
        private BookingMapper bookingMapper;
        @Mock
        private UserRepository userRepository;
        @Mock
        private ShowTimeSeatService showtimeSeatService;
        @Mock
        private PromotionService promotionService;

        @InjectMocks
        private BookingService bookingService;

        private MockedStatic<SecurityContextHolder> securityContextHolderMock;
        private UserEntity user;
        private Showtime showtime;
        private ShowtimeSeat showtimeSeat;
        private CreateBookingRequest createRequest;
        private Booking booking;

        @BeforeEach
        void setUp() {
                securityContextHolderMock = mockStatic(SecurityContextHolder.class);
                SecurityContext securityContext = mock(SecurityContext.class);
                Authentication authentication = mock(Authentication.class);
                lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
                lenient().when(authentication.isAuthenticated()).thenReturn(true);
                lenient().when(authentication.getName()).thenReturn("testuser");
                securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                user = new UserEntity();
                user.setId("user-001");
                user.setUsername("testuser");

                SeatType seatType = new SeatType();
                seatType.setPriceModifier(BigDecimal.ZERO);

                Seat seat = Seat.builder().seatType(seatType).build();
                seat.setId("seat-001");

                showtime = Showtime.builder()
                                .startTime(LocalDateTime.now().plusHours(2))
                                .status(ShowTimeStatus.SCHEDULED)
                                .basePrice(BigDecimal.valueOf(50000))
                                .build();
                showtime.setId("st-001");

                showtimeSeat = ShowtimeSeat.builder()
                                .showtime(showtime)
                                .seat(seat)
                                .status(SeatStatus.LOCKED)
                                .lockedByUser("user-001")
                                .lockedUntil(LocalDateTime.now().plusMinutes(10))
                                .build();

                createRequest = new CreateBookingRequest(
                                Collections.singletonList("seat-001"), "st-001", null, new ArrayList<>());

                booking = Booking.builder()
                                .bookingCode("BK123")
                                .user(user)
                                .showtime(showtime)
                                .status(BookingStatus.PENDING)
                                .tickets(new ArrayList<>())
                                .build();
        }

        @AfterEach
        void tearDown() {
                securityContextHolderMock.close();
        }

        @Nested
        class CreateBookingTests {
                @Test
                void createBooking_Success() {
                        // Given
                        when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));
                        when(showtimeRepository.findByIdWithDetails("st-001")).thenReturn(Optional.of(showtime));
                        when(showtimeSeatRepository.findByShowtimeIdAndSeatIds(eq("st-001"), anyList()))
                                        .thenReturn(Collections.singletonList(showtimeSeat));
                        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
                        when(userRepository.getReferenceById("user-001")).thenReturn(user);

                        // When
                        bookingService.createBooking(createRequest);

                        // Then
                        verify(bookingRepository).save(any(Booking.class));
                }

                @Test
                void createBooking_ShowtimeNotBookable_ThrowsException() {
                        // Given
                        showtime.setStatus(ShowTimeStatus.FINISHED);
                        when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));
                        when(showtimeRepository.findByIdWithDetails("st-001")).thenReturn(Optional.of(showtime));

                        // When & Then
                        assertThatThrownBy(() -> bookingService.createBooking(createRequest))
                                        .isInstanceOf(AppException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SHOWTIME_STATE_INVALID);
                }

                @Test
                void createBooking_SeatNotFound_ThrowsException() {
                        // Given
                        when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));
                        when(showtimeRepository.findByIdWithDetails("st-001")).thenReturn(Optional.of(showtime));
                        when(showtimeSeatRepository.findByShowtimeIdAndSeatIds(any(), any()))
                                        .thenReturn(Collections.emptyList());

                        // When & Then
                        assertThatThrownBy(() -> bookingService.createBooking(createRequest))
                                        .isInstanceOf(AppException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_NOT_FOUND);
                }

                @Test
                void createBooking_SeatNotLockedByCurrentUser_ThrowsException() {
                        // Given
                        showtimeSeat.setLockedByUser("other-user");
                        when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));
                        when(showtimeRepository.findByIdWithDetails("st-001")).thenReturn(Optional.of(showtime));
                        when(showtimeSeatRepository.findByShowtimeIdAndSeatIds(any(), any()))
                                        .thenReturn(Collections.singletonList(showtimeSeat));

                        // When & Then
                        assertThatThrownBy(() -> bookingService.createBooking(createRequest))
                                        .isInstanceOf(AppException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_LOCK_FORBIDDEN);
                }

                @Test
                void createBooking_SeatLockExpired_ThrowsException() {
                        // Given
                        showtimeSeat.setLockedUntil(LocalDateTime.now().minusMinutes(1));
                        when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));
                        when(showtimeRepository.findByIdWithDetails("st-001")).thenReturn(Optional.of(showtime));
                        when(showtimeSeatRepository.findByShowtimeIdAndSeatIds(any(), any()))
                                        .thenReturn(Collections.singletonList(showtimeSeat));

                        // When & Then
                        assertThatThrownBy(() -> bookingService.createBooking(createRequest))
                                        .isInstanceOf(AppException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_LOCK_EXPIRED);
                }

                @Test
                void createBooking_WithPromotion_Success() {
                        // Given
                        CreateBookingRequest requestWithPromo = new CreateBookingRequest(
                                        Collections.singletonList("seat-001"), "st-001", "PROMO10", new ArrayList<>());
                        Promotion promo = new Promotion();
                        promo.setId("promo-001");

                        when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));
                        when(showtimeRepository.findByIdWithDetails("st-001")).thenReturn(Optional.of(showtime));
                        when(showtimeSeatRepository.findByShowtimeIdAndSeatIds(any(), any()))
                                        .thenReturn(Collections.singletonList(showtimeSeat));
                        when(promotionService.previewPromotion(anyString(), anyString(), any()))
                                        .thenReturn(new ValidationResultResponse(true, BigDecimal.valueOf(5000),
                                                        BigDecimal.valueOf(45000), "PROMO10", "10% Off"));
                        when(promotionRepository.findActiveByCode(anyString(), any())).thenReturn(Optional.of(promo));
                        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
                        when(userRepository.getReferenceById("user-001")).thenReturn(user);

                        // When
                        bookingService.createBooking(requestWithPromo);

                        // Then
                        verify(bookingRepository).save(
                                        argThat(b -> b.getDiscountAmount().compareTo(BigDecimal.valueOf(5000)) == 0));
                }

                @Test
                void createBooking_SeatLockExitedDefensive_ThrowsException() {
                        // Given
                        showtimeSeat.setLockedUntil(null); // Case line 108
                        when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));
                        when(showtimeRepository.findByIdWithDetails("st-001")).thenReturn(Optional.of(showtime));
                        when(showtimeSeatRepository.findByShowtimeIdAndSeatIds(any(), any()))
                                        .thenReturn(Collections.singletonList(showtimeSeat));

                        // When & Then
                        assertThatThrownBy(() -> bookingService.createBooking(createRequest))
                                        .isInstanceOf(AppException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_LOCK_EXPIRED);
                }

                @Test
                void createBooking_WithProductsAndCombos_Success() {
                        // Given
                        Product p = new Product();
                        p.setId("p1");
                        p.setName("Water");
                        p.setPrice(BigDecimal.valueOf(10000));
                        Combo c = new Combo();
                        c.setId("c1");
                        c.setName("Combo");
                        c.setPrice(BigDecimal.valueOf(50000));

                        List<org.example.cinemaBooking.DTO.Request.Booking.CreateBookingRequest.BookingProductItem> items = new ArrayList<>();
                        items.add(new org.example.cinemaBooking.DTO.Request.Booking.CreateBookingRequest.BookingProductItem("p1", org.example.cinemaBooking.Shared.enums.ItemType.PRODUCT, 1));
                        items.add(new org.example.cinemaBooking.DTO.Request.Booking.CreateBookingRequest.BookingProductItem("c1", org.example.cinemaBooking.Shared.enums.ItemType.COMBO, 1));

                        CreateBookingRequest request = new CreateBookingRequest(Collections.singletonList("seat-001"),
                                        "st-001", null, items);

                        when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));
                        when(showtimeRepository.findByIdWithDetails("st-001")).thenReturn(Optional.of(showtime));
                        when(showtimeSeatRepository.findByShowtimeIdAndSeatIds(any(), any()))
                                        .thenReturn(Collections.singletonList(showtimeSeat));
                        when(productRepository.findById("p1")).thenReturn(Optional.of(p));
                        when(comboRepository.findById("c1")).thenReturn(Optional.of(c));
                        when(bookingRepository.save(any())).thenReturn(booking);
                        when(userRepository.getReferenceById(any())).thenReturn(user);

                        // When
                        bookingService.createBooking(request);

                        // Then
                        verify(bookingRepository).save(argThat(b -> b.getBookingProducts().size() == 2));
                }

                @Test
                void createBooking_ProductNotFound_ThrowsException() {
                        // Given
                        List<org.example.cinemaBooking.DTO.Request.Booking.CreateBookingRequest.BookingProductItem> items = new ArrayList<>();
                        items.add(new org.example.cinemaBooking.DTO.Request.Booking.CreateBookingRequest.BookingProductItem("p1", org.example.cinemaBooking.Shared.enums.ItemType.PRODUCT, 1));
                        CreateBookingRequest request = new CreateBookingRequest(Collections.singletonList("seat-001"), "st-001", null, items);

                        when(userRepository.findUserEntityByUsername(any())).thenReturn(Optional.of(user));
                        when(showtimeRepository.findByIdWithDetails(any())).thenReturn(Optional.of(showtime));
                        when(showtimeSeatRepository.findByShowtimeIdAndSeatIds(any(), any())).thenReturn(Collections.singletonList(showtimeSeat));
                        when(productRepository.findById("p1")).thenReturn(Optional.empty());

                        // When & Then
                        assertThatThrownBy(() -> bookingService.createBooking(request))
                                        .isInstanceOf(AppException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
                }

                @Test
                void createBooking_ComboNotFound_ThrowsException() {
                        // Given
                        List<org.example.cinemaBooking.DTO.Request.Booking.CreateBookingRequest.BookingProductItem> items = new ArrayList<>();
                        items.add(new org.example.cinemaBooking.DTO.Request.Booking.CreateBookingRequest.BookingProductItem("c1", org.example.cinemaBooking.Shared.enums.ItemType.COMBO, 1));
                        CreateBookingRequest request = new CreateBookingRequest(Collections.singletonList("seat-001"), "st-001", null, items);

                        when(userRepository.findUserEntityByUsername(any())).thenReturn(Optional.of(user));
                        when(showtimeRepository.findByIdWithDetails(any())).thenReturn(Optional.of(showtime));
                        when(showtimeSeatRepository.findByShowtimeIdAndSeatIds(any(), any())).thenReturn(Collections.singletonList(showtimeSeat));
                        when(comboRepository.findById("c1")).thenReturn(Optional.empty());

                        // When & Then
                        assertThatThrownBy(() -> bookingService.createBooking(request))
                                        .isInstanceOf(AppException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMBO_NOT_FOUND);
                }
        }

        @Nested
        class AuthTests {
                @Test
                void getCurrentUser_NotAuthenticated_ThrowsException() {
                        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(mock(SecurityContext.class));
                        // Authentication null or not authenticated
                        assertThatThrownBy(() -> bookingService.createBooking(createRequest))
                                        .isInstanceOf(AppException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
                }
        }

        @Nested
        class ConfirmBookingTests {
                @Test
                void confirmBooking_Success() {
                        // Given
                        Ticket ticket = Ticket.builder().seat(Seat.builder().build()).build();
                        ticket.getSeat().setId("seat-001");
                        booking.getTickets().add(ticket);
                        booking.setExpiredAt(LocalDateTime.now().plusMinutes(5));

                        when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));

                        // When
                        bookingService.confirmBooking("bk-001");

                        // Then
                        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
                        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.VALID);
                        verify(showtimeSeatService).confirmBooking(any(), any(), any());
                        verify(bookingRepository).save(booking);
                }

                @Test
                void confirmBooking_PromotionOutOfStock_ContinuesWithoutDiscount() {
                        // Given
                        Promotion promo = new Promotion();
                        promo.setId("promo-123");
                        booking.setPromotion(promo);
                        booking.setExpiredAt(LocalDateTime.now().plusMinutes(5));
                        when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));
                        doThrow(new AppException(ErrorCode.PROMOTION_OUT_OF_STOCK)).when(promotionService)
                                        .applyPromotion(any(), any());

                        // When
                        bookingService.confirmBooking("bk-001");

                        // Then
                        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
                        assertThat(booking.getPromotion()).isNull();
                        assertThat(booking.getDiscountAmount()).isEqualTo(BigDecimal.ZERO);
                }

                @Test
                void confirmBooking_PromotionApplyGenericAppException_ThrowsException() {
                        // Given
                        Promotion promo = new Promotion(); promo.setId("p1");
                        booking.setPromotion(promo);
                        booking.setExpiredAt(LocalDateTime.now().plusMinutes(5));
                        when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));
                        doThrow(new AppException(ErrorCode.PROMOTION_NOT_FOUND)).when(promotionService).applyPromotion(any(), any());

                        // When & Then
                        assertThatThrownBy(() -> bookingService.confirmBooking("bk-001"))
                                .isInstanceOf(AppException.class)
                                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROMOTION_NOT_FOUND);
                }

                @Test
                void confirmBooking_StatusInvalid_ThrowsException() {
                        // Given
                        booking.setStatus(BookingStatus.CANCELLED);
                        when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));

                        // When & Then
                        assertThatThrownBy(() -> bookingService.confirmBooking("bk-001"))
                                        .isInstanceOf(AppException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_STATUS_INVALID);
                }

                @Test
                void confirmBooking_Expired_ThrowsException() {
                        // Given
                        booking.setExpiredAt(LocalDateTime.now().minusMinutes(1));
                        when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));

                        // When & Then
                        assertThatThrownBy(() -> bookingService.confirmBooking("bk-001"))
                                        .isInstanceOf(AppException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_EXPIRED);
                }
        }

        @Nested
        class CancelTests {
                @Test
                void cancelBooking_Success() {
                        // Given
                        when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));
                        when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));

                        // When
                        bookingService.cancelBooking("bk-001");

                        // Then
                        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
                        verify(showtimeSeatService).releaseLockedSeats(any(), any());
                }

                @Test
                void cancelBooking_AlreadyCancelled_ThrowsException() {
                        // Given
                        booking.setStatus(BookingStatus.CANCELLED);
                        when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));
                        when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));

                        // When & Then
                        assertThatThrownBy(() -> bookingService.cancelBooking("bk-001"))
                                        .isInstanceOf(AppException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_STATUS_INVALID);
                }

                @Test
                void cancelBooking_AlreadyConfirmed_ThrowsException() {
                        // Given
                        booking.setStatus(BookingStatus.CONFIRMED);
                        when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));
                        when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));

                        // When & Then
                        assertThatThrownBy(() -> bookingService.cancelBooking("bk-001"))
                                        .isInstanceOf(AppException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_ALREADY_CONFIRMED);
                }

                @Test
                void cancelBookingInternal_AlreadyCancelled_EarlyReturn() {
                        booking.setStatus(BookingStatus.CANCELLED);
                        when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));
                        bookingService.cancelBookingInternal("bk-001");
                        verify(bookingRepository, never()).save(any());
                }

                @Test
                void cancelBookingInternal_Confirmed_ReleasesBookedSeats() {
                        booking.setStatus(BookingStatus.CONFIRMED);
                        when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));
                        bookingService.cancelBookingInternal("bk-001");
                        verify(showtimeSeatService).releaseBookedSeats(any(), any());
                }

                @Test
                void cancelBooking_NotOwner_ThrowsException() {
                        // Given
                        UserEntity differentUser = new UserEntity();
                        differentUser.setId("other-user");
                        booking.setUser(differentUser);
                        when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));
                        when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));

                        // When & Then
                        assertThatThrownBy(() -> bookingService.cancelBooking("bk-001"))
                                        .isInstanceOf(AppException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
                }

                @Test
                void cancelBooking_BookingNotFound_ThrowsException() {
                        when(bookingRepository.findByIdWithDetails("unknown")).thenReturn(Optional.empty());
                        assertThatThrownBy(() -> bookingService.cancelBooking("unknown"))
                                        .isInstanceOf(AppException.class)
                                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_NOT_FOUND);
                }
        }

        @Nested
        class JobTests {
                @Test
                void expireStaleBookings_Success() {
                        // Given
                        Ticket ticket = Ticket.builder().seat(Seat.builder().build()).build();
                        ticket.getSeat().setId("seat-001");
                        booking.getTickets().add(ticket);

                        when(bookingRepository.findExpiredPendingBookings(any()))
                                        .thenReturn(Collections.singletonList(booking));

                        // When
                        bookingService.expireStaleBookings();

                        // Then
                        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
                        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
                        verify(showtimeSeatService).releaseLockedSeats(any(), anyList());
                        verify(bookingRepository).saveAll(anyList());
                }

                @Test
                void expireStaleBookings_EmptyList_EarlyReturn() {
                        when(bookingRepository.findExpiredPendingBookings(any())).thenReturn(Collections.emptyList());
                        bookingService.expireStaleBookings();
                        verify(bookingRepository, never()).saveAll(any());
                }
        }

        @Test
        void getBookingById_Owner_Success() {
                when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));
                when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));
                when(bookingMapper.toResponse(booking)).thenReturn(mock(BookingResponse.class));
                assertThat(bookingService.getBookingById("bk-001")).isNotNull();
        }

        @Test
        void getBookingById_NotOwner_ThrowsException() {
                UserEntity differentUser = new UserEntity();
                differentUser.setId("other-user");
                booking.setUser(differentUser);
                when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));
                when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));

                assertThatThrownBy(() -> bookingService.getBookingById("bk-001"))
                        .isInstanceOf(AppException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        }

        @Test
        void getCurrentUser_UserNotFound_ThrowsException() {
                when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.empty());
                
                // createBooking calls getCurrentUser first thing
                assertThatThrownBy(() -> bookingService.createBooking(createRequest))
                        .isInstanceOf(AppException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
}

package org.example.cinemaBooking.unit.service;

import org.example.cinemaBooking.DTO.Response.Ticket.CheckInResponse;
import org.example.cinemaBooking.DTO.Response.Ticket.TicketResponse;
import org.example.cinemaBooking.Entity.*;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.TicketMapper;
import org.example.cinemaBooking.Repository.BookingRepository;
import org.example.cinemaBooking.Repository.TicketRepository;
import org.example.cinemaBooking.Repository.UserRepository;
import org.example.cinemaBooking.Service.Ticket.TicketService;
import org.example.cinemaBooking.Shared.enums.BookingStatus;
import org.example.cinemaBooking.Shared.enums.ShowTimeStatus;
import org.example.cinemaBooking.Shared.enums.TicketStatus;
import org.example.cinemaBooking.Shared.utils.QRCodeUtil;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TicketMapper ticketMapper;

    @InjectMocks
    private TicketService ticketService;

    private MockedStatic<SecurityContextHolder> securityContextHolderMock;
    private MockedStatic<QRCodeUtil> qrCodeUtilMock;
    private UserEntity user;
    private Booking booking;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        securityContextHolderMock = mockStatic(SecurityContextHolder.class);
        qrCodeUtilMock = mockStatic(QRCodeUtil.class);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn("testuser");
        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        user = new UserEntity();
        user.setId("user-001");
        user.setUsername("testuser");

        Showtime showtime = Showtime.builder()
                .movie(Movie.builder().title("Movie A").build())
                .room(Room.builder().name("Room A").cinema(Cinema.builder().name("Cinema A").build()).build())
                .startTime(LocalDateTime.now().plusHours(1))
                .status(ShowTimeStatus.ONGOING) // set for check-in test
                .build();

        booking = Booking.builder()
                .bookingCode("BK123")
                .user(user)
                .showtime(showtime)
                .status(BookingStatus.CONFIRMED)
                .build();
        booking.setId("bk-001");

        ticket = Ticket.builder()
                .ticketCode("TK123")
                .booking(booking)
                .seat(Seat.builder().seatRow("A").seatNumber(1).seatType(new SeatType()).build())
                .status(TicketStatus.VALID)
                .build();
        ticket.setId("t-001");
    }

    @AfterEach
    void tearDown() {
        securityContextHolderMock.close();
        qrCodeUtilMock.close();
    }

    @Nested
    class GetTicketTests {
        @Test
        void getTicketsByBooking_Success() {
            // Given
            when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));
            when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));
            when(ticketRepository.findAllByBookingId("bk-001")).thenReturn(Collections.singletonList(ticket));
            when(ticketMapper.toResponse(any())).thenReturn(mock(TicketResponse.class));

            // When
            List<TicketResponse> results = ticketService.getTicketsByBooking("bk-001");

            // Then
            assertThat(results).hasSize(1);
            verify(ticketRepository).findAllByBookingId("bk-001");
        }

        @Test
        void getTicketsByBooking_Unauthorized_ThrowsException() {
            // Given
            user.setId("user-001"); // current user
            UserEntity owner = new UserEntity();
            owner.setId("user-999"); // different owner
            booking.setUser(owner);

            when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));
            when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));

            // When & Then
            assertThatThrownBy(() -> ticketService.getTicketsByBooking("bk-001"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        }

        @Test
        void getTicketsByBooking_BookingNotFound_ThrowsException() {
            when(bookingRepository.findByIdWithDetails("unknown")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> ticketService.getTicketsByBooking("unknown"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_NOT_FOUND);
        }

        @Test
        void getTicketsByBooking_StatusInvalid_ThrowsException() {
            booking.setStatus(BookingStatus.PENDING);
            when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));
            when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));
            assertThatThrownBy(() -> ticketService.getTicketsByBooking("bk-001"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_STATUS_INVALID);
        }

        @Test
        void getMyTickets_Success() {
            when(userRepository.findUserEntityByUsername("testuser")).thenReturn(Optional.of(user));
            when(ticketRepository.findAllByUserId("user-001")).thenReturn(Collections.singletonList(ticket));
            when(ticketMapper.toResponse(any())).thenReturn(mock(TicketResponse.class));

            List<TicketResponse> results = ticketService.getMyTickets();
            assertThat(results).hasSize(1);
            verify(ticketRepository).findAllByUserId("user-001");
        }

        @Test
        void getCurrentUser_NotAuthenticated_ThrowsException() {
            SecurityContextHolder.getContext().setAuthentication(null); 
            // In setup we mocked it, so we need to override the mock for getContext
            // Wait, securityContextHolderMock is already returning a mocked SecurityContext. Let's provide a null Authentication.
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(null);
            securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            assertThatThrownBy(() -> ticketService.getMyTickets())
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        }

        @Test
        void getBookingQR_Success() {
            // Given
            when(bookingRepository.findByBookingCode("BK123")).thenReturn(Optional.of(booking));
            qrCodeUtilMock.when(() -> QRCodeUtil.generateBase64QR("BK123")).thenReturn("base64data");

            // When
            String qr = ticketService.getBookingQR("BK123");

            // Then
            assertThat(qr).isEqualTo("base64data");
        }

        @Test
        void getBookingQR_BookingNotFound_ThrowsException() {
            when(bookingRepository.findByBookingCode("UNKNOWN")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> ticketService.getBookingQR("UNKNOWN"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_NOT_FOUND);
        }

        @Test
        void getBookingQR_StatusInvalid_ThrowsException() {
            booking.setStatus(BookingStatus.CANCELLED);
            when(bookingRepository.findByBookingCode("BK123")).thenReturn(Optional.of(booking));
            assertThatThrownBy(() -> ticketService.getBookingQR("BK123"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_STATUS_INVALID);
        }
    }

    @Nested
    class CheckInTests {
        @Test
        void checkInByBookingCode_Success() {
            // Given
            when(ticketRepository.findAllByBookingCode("BK123")).thenReturn(Collections.singletonList(ticket));

            // When
            CheckInResponse response = ticketService.checkInByBookingCode("BK123");

            // Then
            assertThat(response).isNotNull();
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
            verify(ticketRepository).saveAll(anyList());
        }

        @Test
        void checkInByBookingCode_ShowtimeNotOngoing_ThrowsException() {
            // Given
            booking.getShowtime().setStatus(ShowTimeStatus.SCHEDULED);
            when(ticketRepository.findAllByBookingCode("BK123")).thenReturn(Collections.singletonList(ticket));

            // When & Then
            assertThatThrownBy(() -> ticketService.checkInByBookingCode("BK123"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SHOWTIME_NOT_ONGOING);
        }

        @Test
        void checkInByBookingCode_EmptyTickets_ThrowsException() {
            when(ticketRepository.findAllByBookingCode("BK123")).thenReturn(Collections.emptyList());
            assertThatThrownBy(() -> ticketService.checkInByBookingCode("BK123"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TICKET_NOT_FOUND);
        }

        @Test
        void checkInByBookingCode_BookingStatusInvalid_ThrowsException() {
            booking.setStatus(BookingStatus.CANCELLED);
            when(ticketRepository.findAllByBookingCode("BK123")).thenReturn(Collections.singletonList(ticket));
            assertThatThrownBy(() -> ticketService.checkInByBookingCode("BK123"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_STATUS_INVALID);
        }

        @Test
        void checkInByBookingCode_HasInvalidTicket_Skips() {
            ticket.setStatus(TicketStatus.EXPIRED); 
            when(ticketRepository.findAllByBookingCode("BK123")).thenReturn(Collections.singletonList(ticket));
            CheckInResponse response = ticketService.checkInByBookingCode("BK123");
            
            // Then
            assertThat(response).isNotNull();
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.EXPIRED); // Unchanged
            verify(ticketRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    class JobTests {
        @Test
        void expireUnusedTickets_Success() {
            // Given
            when(ticketRepository.findValidTicketsOfFinishedShowtimes()).thenReturn(Collections.singletonList(ticket));

            // When
            ticketService.expireUnusedTickets();

            // Then
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.EXPIRED);
            verify(ticketRepository).saveAll(anyList());
        }

        @Test
        void expireUnusedTickets_Empty_Returns() {
            when(ticketRepository.findValidTicketsOfFinishedShowtimes()).thenReturn(Collections.emptyList());
            ticketService.expireUnusedTickets();
            verify(ticketRepository, never()).saveAll(anyList());
        }
    }
}

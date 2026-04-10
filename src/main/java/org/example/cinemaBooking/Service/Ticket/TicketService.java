package org.example.cinemaBooking.Service.Ticket;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Response.Ticket.BookingProductInfo;
import org.example.cinemaBooking.DTO.Response.Ticket.CheckInResponse;
import org.example.cinemaBooking.DTO.Response.Ticket.TicketInfo;
import org.example.cinemaBooking.DTO.Response.Ticket.TicketResponse;
import org.example.cinemaBooking.Entity.Booking;
import org.example.cinemaBooking.Entity.Showtime;
import org.example.cinemaBooking.Entity.Ticket;
import org.example.cinemaBooking.Entity.UserEntity;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.TicketMapper;
import org.example.cinemaBooking.Repository.BookingRepository;
import org.example.cinemaBooking.Repository.TicketRepository;
import org.example.cinemaBooking.Repository.UserRepository;
import org.example.cinemaBooking.Shared.enums.BookingStatus;
import org.example.cinemaBooking.Shared.enums.ShowTimeStatus;
import org.example.cinemaBooking.Shared.enums.TicketStatus;
import org.example.cinemaBooking.Shared.utils.QRCodeUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class TicketService {

    TicketRepository  ticketRepository;
    BookingRepository bookingRepository;
    UserRepository    userRepository;
    TicketMapper      ticketMapper;

    // ─────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────

    /** Lấy tất cả vé của 1 booking — user chỉ xem được booking của mình */
    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByBooking(String bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        checkOwnership(booking);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);
        }

        return ticketRepository.findAllByBookingId(bookingId)
                .stream().map(ticketMapper::toResponse).toList();
    }

    /** Lấy tất cả vé của user hiện tại */
    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets() {
        String userId = getCurrentUser().getId();
        return ticketRepository.findAllByUserId(userId)
                .stream().map(ticketMapper::toResponse).toList();
    }

    /**
     * Lấy QR code cho booking — gửi trong email sau khi CONFIRMED.
     *
     * FIX T3: chỉ booking CONFIRMED mới được lấy QR.
     * Booking PENDING chưa thanh toán không có QR để tránh lạm dụng.
     */
    @Transactional(readOnly = true)
    public String getBookingQR(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // FIX T3: chỉ CONFIRMED mới có QR hợp lệ
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);
        }

        return QRCodeUtil.generateBase64QR(bookingCode);
    }

    // ─────────────────────────────────────────────────────────────────
    // CHECK-IN — nhân viên scan QR tại cửa rạp
    // ─────────────────────────────────────────────────────────────────

    /**
     * Check-in toàn bộ vé trong 1 booking bằng bookingCode từ QR.
     *
     * FIX T1: validate booking.status == CONFIRMED trước khi cho check-in.
     * FIX T2: không dùng peek() để save — collect xong rồi saveAll().
     */
    @Transactional
    public CheckInResponse checkInByBookingCode(String bookingCode) {
        List<Ticket> tickets = ticketRepository.findAllByBookingCode(bookingCode);
        var showtime = getShowtime(tickets);
        LocalDateTime now = LocalDateTime.now();

        // Products — chỉ lấy 1 lần từ booking
        List<BookingProductInfo> products = tickets.isEmpty()
                ? List.of()
                : tickets.get(0).getBooking().getBookingProducts()
                .stream()
                .map(bp -> new BookingProductInfo(
                        bp.getItemName(),
                        bp.getItemType().name(),
                        bp.getQuantity()
                ))
                .toList();

        // Check-in từng ghế hợp lệ
        List<Ticket> toCheckIn = new ArrayList<>();
        List<TicketInfo> ticketInfos = new ArrayList<>();

        for (Ticket t : tickets) {
            if (t.getStatus() != TicketStatus.VALID) continue;
            t.setStatus(TicketStatus.USED);
            t.setCheckedInAt(now);
            toCheckIn.add(t);
            ticketInfos.add(new TicketInfo(
                    t.getTicketCode(),
                    t.getSeat().getSeatRow(),
                    t.getSeat().getSeatNumber(),
                    t.getSeat().getSeatType().getName(),
                    t.getStatus(),
                    t.getCheckedInAt()
            ));
        }

        if (!toCheckIn.isEmpty()) {
            ticketRepository.saveAll(toCheckIn);
        }

        log.info("Checked in {} ticket(s) for bookingCode={}", toCheckIn.size(), bookingCode);

        return new CheckInResponse(
                bookingCode,
                showtime.getMovie().getTitle(),
                showtime.getRoom().getName(),
                showtime.getStartTime(),
                ticketInfos,
                products
        );
    }

    private static Showtime getShowtime(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            throw new AppException(ErrorCode.TICKET_NOT_FOUND);
        }

        Booking booking = tickets.getFirst().getBooking();

        // FIX T1: check booking đã được CONFIRMED (đã thanh toán)
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);
        }

        var showtime = booking.getShowtime();
        if (showtime.getStatus() != ShowTimeStatus.ONGOING) {
            throw new AppException(ErrorCode.SHOWTIME_NOT_ONGOING);
        }
        return showtime;
    }

    // ─────────────────────────────────────────────────────────────────
    // SCHEDULED — expire vé sau khi suất chiếu kết thúc
    // ─────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireUnusedTickets() {
        List<Ticket> tickets = ticketRepository.findValidTicketsOfFinishedShowtimes();
        if (tickets.isEmpty()) return;

        tickets.forEach(t -> t.setStatus(TicketStatus.EXPIRED));
        ticketRepository.saveAll(tickets);

        log.info("Expired {} unused ticket(s)", tickets.size());
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE
    // ─────────────────────────────────────────────────────────────────

    /**
     * FIX T4: getCurrentUser() query DB để lấy userId thực,
     * thay vì dùng auth.getName() trả về username (không phải UUID).
     */
    private UserEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return userRepository.findUserEntityByUsername(auth.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void checkOwnership(Booking booking) {
        String currentUserId = getCurrentUser().getId();
        if (!booking.getUser().getId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
}
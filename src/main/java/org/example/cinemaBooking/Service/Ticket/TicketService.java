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

/**
 * Dịch vụ quản lý Ticket (vé).
 * Chức năng chính:
 * - Lấy vé theo booking / user
 * - Sinh QR code cho booking đã được thanh toán
 * - Check-in vé (nhân viên quét QR tại cửa)
 * - Expire vé chưa sử dụng sau khi suất chiếu kết thúc
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class TicketService {

    TicketRepository  ticketRepository;
    BookingRepository bookingRepository;
    UserRepository    userRepository;
    TicketMapper      ticketMapper;


    /**
     * Lấy tất cả vé của một booking — chỉ owner (user) mới xem được.
     *
     * @param bookingId ID của booking
     * @return danh sách TicketResponse
     */
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

    /**
     * Lấy tất cả vé của user hiện tại.
     *
     * @return danh sách TicketResponse của user
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets() {
        String userId = getCurrentUser().getId();
        return ticketRepository.findAllByUserId(userId)
                .stream().map(ticketMapper::toResponse).toList();
    }

    /**
     * Sinh QR code (Base64) cho booking đã được CONFIRMED.
     * Lưu ý: chỉ booking đã thanh toán (CONFIRMED) mới được cấp QR để tránh lạm dụng.
     *
     * @param bookingCode mã booking
     * @return chuỗi Base64 biểu diễn QR
     */
    @Transactional(readOnly = true)
    public String getBookingQR(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // chỉ CONFIRMED mới có QR hợp lệ
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);
        }

        return QRCodeUtil.generateBase64QR(bookingCode);
    }


    /**
     * Check-in toàn bộ vé trong một booking bằng bookingCode (khi quét QR tại cửa).
     * Quy tắc:
     * - Chỉ vé có status = VALID mới được check-in.
     * - Sau check-in: status -> USED, ghi thời gian checkedInAt.
     * - Trả về thông tin showtime và các vé đã check-in cùng sản phẩm kèm theo.
     *
     * @param bookingCode mã booking (lấy từ QR)
     * @return CheckInResponse chứa thông tin showtime, vé đã check-in và products
     */
    @Transactional
    public CheckInResponse checkInByBookingCode(String bookingCode) {
        List<Ticket> tickets = ticketRepository.findAllByBookingCode(bookingCode);
        var showtime = getShowtime(tickets);
        LocalDateTime now = LocalDateTime.now();

        // Products — chỉ lấy 1 lần từ booking
        List<BookingProductInfo> products = tickets.isEmpty()
                ? List.of()
                : tickets.stream()
                .findFirst()
                .map(t -> t.getBooking().getBookingProducts()
                        .stream()
                        .map(bp -> new BookingProductInfo(
                                bp.getItemName(),
                                bp.getItemType().name(),
                                bp.getQuantity()
                        ))
                        .toList())
                .orElse(List.of());

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

    /**
     * Lấy đối tượng Showtime từ danh sách tickets kèm một số kiểm tra hợp lệ.
     *
     * @param tickets danh sách vé liên quan đến một booking
     * @return Showtime liên quan
     */
    private static Showtime getShowtime(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            throw new AppException(ErrorCode.TICKET_NOT_FOUND);
        }

        Booking booking = tickets.stream()
                .findFirst()
                .map(Ticket::getBooking)
                .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND));

        // booking phải ở trạng thái CONFIRMED
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);
        }

        var showtime = booking.getShowtime();
        if (showtime.getStatus() != ShowTimeStatus.ONGOING) {
            throw new AppException(ErrorCode.SHOWTIME_NOT_ONGOING);
        }
        return showtime;
    }


    /**
     * Job chạy hàng giờ để expire các vé VALID của những suất chiếu đã kết thúc.
     */
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
     * Lấy user hiện tại (query DB để lấy UserEntity đầy đủ).
     *
     * @return UserEntity của user đang đăng nhập
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
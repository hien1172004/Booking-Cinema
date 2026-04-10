package org.example.cinemaBooking.Service.Booking;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Booking.CreateBookingRequest;
import org.example.cinemaBooking.DTO.Request.Payment.CreatePaymentRequest;
import org.example.cinemaBooking.DTO.Response.Booking.BookingResponse;
import org.example.cinemaBooking.DTO.Response.Booking.BookingSummaryResponse;
import org.example.cinemaBooking.DTO.Response.Payment.PaymentResponse;
import org.example.cinemaBooking.Entity.*;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.BookingMapper;
import org.example.cinemaBooking.Repository.*;
import org.example.cinemaBooking.Service.Notification.NotificationService;
import org.example.cinemaBooking.Service.Payment.PaymentService;
import org.example.cinemaBooking.Service.Promotion.PromotionService;
import org.example.cinemaBooking.Service.Showtime.ShowTimeSeatService;
import org.example.cinemaBooking.Shared.enums.BookingStatus;
import org.example.cinemaBooking.Shared.enums.ItemType;
import org.example.cinemaBooking.Shared.enums.SeatStatus;
import org.example.cinemaBooking.Shared.enums.TicketStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingService {

    BookingRepository     bookingRepository;
    TicketRepository      ticketRepository;
    ShowtimeSeatRepository showtimeSeatRepository;
    ShowtimeRepository    showtimeRepository;
    PromotionRepository   promotionRepository;
    ProductRepository     productRepository;
    ComboRepository       comboRepository;
    BookingMapper         bookingMapper;
    UserRepository        userRepository;
    ShowTimeSeatService   showtimeSeatService;
    PromotionService      promotionService;
    NotificationService   notificationService;
    // Thời gian user có để hoàn tất payment — phải >= LOCK_DURATION_MINUTES
    static final int BOOKING_EXPIRY_MINUTES = 10;

    // ─────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────

    /**
     * Tạo booking PENDING.
     *
     * Flow A:
     *   1. Validate ghế LOCKED bởi đúng user, lockedUntil còn hạn   (FIX Bug-7)
     *   2. Tính giá vé + đồ ăn + promotion
     *   3. Lưu Booking + Ticket (status = PENDING_PAYMENT)           (FIX Bug-2)
     *   4. KHÔNG chuyển ghế → ghế vẫn LOCKED cho đến khi payment IPN confirm
     *
     * Ghế sẽ được chuyển LOCKED → BOOKED bởi PaymentService sau khi IPN thành công.
     */
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        String userId = getCurrentUser().getId();

        Showtime showtime = showtimeRepository.findByIdWithDetails(request.showtimeId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOWTIME_NOT_FOUND));

        if (!showtime.isBookable()) {
            throw new AppException(ErrorCode.SHOWTIME_STATE_INVALID);
        }

        List<ShowtimeSeat> lockedSeats = showtimeSeatRepository
                .findByShowtimeIdAndSeatIds(request.showtimeId(), request.seatIds());

        if (lockedSeats.size() != request.seatIds().size()) {
            throw new AppException(ErrorCode.SEAT_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();

        lockedSeats.forEach(ss -> {
            if (ss.getStatus() != SeatStatus.LOCKED) {
                throw new AppException(ErrorCode.SEAT_NOT_LOCKED);
            }
            if (!userId.equals(ss.getLockedByUser())) {
                throw new AppException(ErrorCode.SEAT_LOCK_FORBIDDEN);
            }
            // FIX Bug-7: check lockedUntil còn hạn
            if (ss.getLockedUntil() == null || ss.getLockedUntil().isBefore(now)) {
                throw new AppException(ErrorCode.SEAT_LOCK_EXPIRED);
            }
        });

        // Tính giá vé
        BigDecimal totalTicketPrice = lockedSeats.stream()
                .map(ss -> showtime.getBasePrice()
                        .add(ss.getSeat().getSeatType().getPriceModifier()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính giá đồ ăn/combo
        List<BookingProduct> bookingProducts = resolveProducts(request.products());
        BigDecimal totalProductPrice = bookingProducts.stream()
                .map(p -> p.getItemPrice().multiply(BigDecimal.valueOf(p.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPrice = totalTicketPrice.add(totalProductPrice);

        // Áp promotion nếu có
        BigDecimal discountAmount = BigDecimal.ZERO;
        Promotion promotion = null;
        if (request.promotionCode() != null) {
            var preview = promotionService.previewPromotion(
                    request.promotionCode(), userId, totalPrice);

            promotion = promotionRepository
                    .findActiveByCode(request.promotionCode(), LocalDate.now())
                    .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

            discountAmount = preview.discountAmount();
        }

        BigDecimal finalPrice = totalPrice.subtract(discountAmount).max(BigDecimal.ZERO);


        // Tạo Booking
        Booking booking = Booking.builder()
                .bookingCode(generateBookingCode())
                .user(getUserRef(userId))
                .showtime(showtime)
                .promotion(promotion)
                .totalPrice(totalPrice)
                .discountAmount(discountAmount)
                .finalPrice(finalPrice)
                .expiredAt(now.plusMinutes(BOOKING_EXPIRY_MINUTES))
                .build();


        List<Ticket> tickets = lockedSeats.stream()
                .map(ss -> Ticket.builder()
                        .ticketCode(generateTicketCode())
                        .booking(booking)
                        .seat(ss.getSeat())
                        .price(showtime.getBasePrice()
                                .add(ss.getSeat().getSeatType().getPriceModifier()))
                        .status(TicketStatus.PENDING_PAYMENT)
                        .build())
                .toList();

        bookingProducts.forEach(p -> p.setBooking(booking));
        booking.getTickets().addAll(tickets);
        booking.getBookingProducts().addAll(bookingProducts);

        Booking saved = bookingRepository.save(booking);


        log.info("Booking created: code={}, user={}, showtime={}, seats={}, finalPrice={}",
                saved.getBookingCode(), userId, request.showtimeId(),
                request.seatIds().size(), finalPrice);

        return bookingMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(String bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        checkOwnership(booking);
        return bookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingSummaryResponse> getMyBookings() {
        String userId = getCurrentUser().getId();
        return bookingRepository.findAllByUserId(userId)
                .stream().map(bookingMapper::toSummary).toList();
    }

    // ─────────────────────────────────────────────────────────────────
    // CONFIRM — gọi bởi PaymentService sau IPN thành công
    // ─────────────────────────────────────────────────────────────────

    /**
     * Confirm booking sau khi payment IPN thành công.
     *
     * Gọi bởi PaymentService — KHÔNG gọi trực tiếp từ controller.
     * PaymentService cần đảm bảo transaction độc lập (REQUIRES_NEW).
     *
     * FIX Bug-1 (BookingService): ghế được chuyển LOCKED→BOOKED tại đây.
     * FIX Bug-2: tickets chuyển PENDING_PAYMENT → VALID tại đây.
     * FIX RC-4: confirmBooking trong ShowTimeSeatService đã check lockedUntil.
     */
    @Transactional
    public void confirmBooking(String bookingId) {
        Booking booking = getBookingOrThrow(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);
        }
        if (booking.isExpired()) {
            throw new AppException(ErrorCode.BOOKING_EXPIRED);
        }

        // Chuyển ghế LOCKED → BOOKED
        List<String> seatIds = booking.getTickets().stream()
                .map(t -> t.getSeat().getId()).toList();

        showtimeSeatService.confirmBooking(
                booking.getShowtime().getId(),
                seatIds,
                booking.getUser().getId()
        );

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setExpiredAt(null);

        // tickets chuyển PENDING_PAYMENT → VALID
        booking.getTickets().forEach(t -> t.setStatus(TicketStatus.VALID));

        // Áp promotion sau khi payment thực sự thành công
        if (booking.getPromotion() != null) {
            try {
                promotionService.applyPromotion(
                        booking.getPromotion().getId(),
                        booking.getUser().getId()
                );
            } catch (AppException e) {
                if (e.getErrorCode() == ErrorCode.PROMOTION_OUT_OF_STOCK) {
                    // Hết slot promotion → bỏ discount, vẫn confirm booking
                    log.warn("Promotion out of stock at confirm: bookingCode={}",
                            booking.getBookingCode());
                    booking.setDiscountAmount(BigDecimal.ZERO);
                    booking.setFinalPrice(booking.getTotalPrice());
                    booking.setPromotion(null);
                } else {
                    throw e;
                }
            }
        }

        bookingRepository.save(booking);
        log.info("Booking confirmed: code={}", booking.getBookingCode());
    }

    // ─────────────────────────────────────────────────────────────────
    // CANCEL
    // ─────────────────────────────────────────────────────────────────

    /**
     * User chủ động cancel booking.
     *
     * FIX Bug-6: phân biệt ghế đang LOCKED (PENDING) vs BOOKED (CONFIRMED)
     * để gọi đúng method release.
     */
    @Transactional
    public BookingResponse cancelBooking(String bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        checkOwnership(booking);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CONFIRMED);
        }

        doCancel(booking);

        Booking saved = bookingRepository.save(booking);
        log.warn("Booking cancelled by user: code={}", booking.getBookingCode());
        return bookingMapper.toResponse(saved);
    }

    /**
     * Internal cancel — dùng bởi cả user cancel lẫn scheduled job.
     * FIX Bug-3 & Bug-6: booking PENDING → ghế đang LOCKED → dùng releaseLockedSeats().
     */
    @Transactional
    public void cancelBookingInternal(String bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) return;
        doCancel(booking);
        bookingRepository.save(booking);
        log.warn("Booking cancelled (internal): code={}", booking.getBookingCode());
    }

    // ─────────────────────────────────────────────────────────────────
    // SCHEDULED — expire booking PENDING quá hạn
    // ─────────────────────────────────────────────────────────────────

    /**
     * FIX Bug-3: gọi releaseLockedSeats() thay vì releaseBookedSeats()
     * vì booking PENDING → ghế đang LOCKED, chưa BOOKED.
     *
     * FIX RC-4 (BookingService): dùng pessimistic lock trên booking query
     * để tránh xung đột với confirmBooking() đang chạy đồng thời.
     * (Cần thêm @Lock(PESSIMISTIC_WRITE) vào findExpiredPendingBookings
     * hoặc xử lý bằng status check sau khi acquire.)
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void expireStaleBookings() {
        List<Booking> expired = bookingRepository
                .findExpiredPendingBookings(LocalDateTime.now());

        if (expired.isEmpty()) return;

        expired.forEach(b -> {
            b.setStatus(BookingStatus.CANCELLED);
            b.getTickets().forEach(t -> t.setStatus(TicketStatus.CANCELLED));

            List<String> seatIds = b.getTickets().stream()
                    .map(t -> t.getSeat().getId()).toList();

            // FIX Bug-3: PENDING booking → ghế LOCKED → release locked seats
            showtimeSeatService.releaseLockedSeats(b.getShowtime().getId(), seatIds);
        });

        bookingRepository.saveAll(expired);
        log.info("Expired {} stale booking(s)", expired.size());
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Dùng chung cho user cancel và internal cancel.
     * Phân biệt trạng thái ghế theo booking status.
     */
    private void doCancel(Booking booking) {
        BookingStatus oldStatus = booking.getStatus(); // lưu trước

        booking.setStatus(BookingStatus.CANCELLED);
        booking.getTickets().forEach(t -> t.setStatus(TicketStatus.CANCELLED));

        List<String> seatIds = booking.getTickets().stream()
                .map(t -> t.getSeat().getId()).toList();

        String showtimeId = booking.getShowtime().getId();

        if (oldStatus == BookingStatus.CONFIRMED) {
            showtimeSeatService.releaseBookedSeats(showtimeId, seatIds);
        } else {
            showtimeSeatService.releaseLockedSeats(showtimeId, seatIds);
        }
    }

    private List<BookingProduct> resolveProducts(
            List<CreateBookingRequest.BookingProductItem> items) {
        return items.stream().map(item -> {
            String name;
            BigDecimal price;
            if (item.itemType() == ItemType.PRODUCT) {
                Product p = productRepository.findById(item.itemId())
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
                name = p.getName();
                price = p.getPrice();
            } else {
                Combo c = comboRepository.findById(item.itemId())
                        .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));
                name = c.getName();
                price = c.getPrice();
            }
            return BookingProduct.builder()
                    .itemType(item.itemType())
                    .itemId(item.itemId())
                    .itemName(name)
                    .itemPrice(price)
                    .quantity(item.quantity())
                    .build();
        }).toList();
    }

    /**
     * FIX Bug-5: dùng UUID thay vì currentTimeMillis để tránh trùng lặp.
     * Unique constraint trên DB là tầng bảo vệ cuối.
     */
    private String generateBookingCode() {
        return "BK" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private String generateTicketCode() {
        return "TK" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private Booking getBookingOrThrow(String id) {
        return bookingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
    }

    private void checkOwnership(Booking booking) {
        String currentUserId = getCurrentUser().getId();
        if (!booking.getUser().getId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private UserEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return userRepository.findUserEntityByUsername(auth.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private UserEntity getUserRef(String userId) {
        return userRepository.getReferenceById(userId);
    }
}
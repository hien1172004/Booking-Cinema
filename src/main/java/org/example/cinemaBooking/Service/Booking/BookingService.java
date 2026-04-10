package org.example.cinemaBooking.Service.Booking;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Booking.CreateBookingRequest;
import org.example.cinemaBooking.DTO.Response.Booking.BookingResponse;
import org.example.cinemaBooking.DTO.Response.Booking.BookingSummaryResponse;
import org.example.cinemaBooking.Entity.*;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.BookingMapper;
import org.example.cinemaBooking.Repository.*;
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

/**
 * Dịch vụ quản lý Booking (đặt vé).
 *
 * Trách nhiệm chính:
 * - Tạo booking (PENDING)
 * - Xác nhận booking sau khi thanh toán thành công
 * - Huỷ booking (do user hoặc tự động khi quá hạn)
 * - Quản lý trạng thái vé/ghế tương ứng (thông qua ShowTimeSeatService)
 *
 * Ghi chú:
 * - Các thao tác liên quan tới trạng thái ghế (LOCKED/BOOKED) được thực hiện
 *   bởi `ShowTimeSeatService` để đảm bảo tính nhất quán đồng bộ trên hàng ghế.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingService {

    BookingRepository     bookingRepository;
    ShowtimeSeatRepository showtimeSeatRepository;
    ShowtimeRepository    showtimeRepository;
    PromotionRepository   promotionRepository;
    ProductRepository     productRepository;
    ComboRepository       comboRepository;
    BookingMapper         bookingMapper;
    UserRepository        userRepository;
    ShowTimeSeatService   showtimeSeatService;
    PromotionService      promotionService;
    // Thời gian user có để hoàn tất payment — phải >= LOCK_DURATION_MINUTES
    static final int BOOKING_EXPIRY_MINUTES = 10;


    /**
     * Tạo một Booking ở trạng thái PENDING (chờ thanh toán).
     *
     * Quy trình tóm tắt:
     *  1) Xác thực ghế phải đang ở trạng thái LOCKED bởi chính user và chưa hết hạn lock.
     *  2) Tính tổng tiền vé + sản phẩm (combo/product) và áp dụng khuyến mãi (nếu có).
     *  3) Lưu Booking và các Ticket kèm trạng thái Ticket = PENDING_PAYMENT.
     *
     * Lưu ý:
     * - Phương thức này KHÔNG chuyển trạng thái ghế sang BOOKED. Việc chuyển LOCKED→BOOKED
     *   sẽ được thực hiện khi có xác nhận thanh toán (IPN) bởi `confirmBooking`.
     *
     * @param request Yêu cầu tạo booking (chứa showtimeId, danh sách seatIds, products, promotionCode)
     * @return BookingResponse thông tin booking vừa tạo
     * @throws AppException khi dữ liệu đầu vào không hợp lệ, ghế không tồn tại/không được khoá, hoặc showtime không thể book
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


    /**
     * Lấy chi tiết booking theo ID. Chỉ owner (user cùng ID) được truy xuất.
     *
     * @param bookingId ID booking
     * @return BookingResponse chi tiết booking
     * @throws AppException nếu booking không tồn tại hoặc không thuộc quyền sở hữu
     */
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(String bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        checkOwnership(booking);
        return bookingMapper.toResponse(booking);
    }

    /**
     * Lấy danh sách tóm tắt các booking của người đang đăng nhập.
     *
     * @return danh sách BookingSummaryResponse
     */
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
     * Xác nhận booking sau khi thanh toán thành công (IPN).
     *
     * Thao tác sẽ:
     *  - Kiểm tra trạng thái Booking hợp lệ và chưa hết hạn
     *  - Chuyển trạng thái ghế LOCKED → BOOKED (qua `ShowTimeSeatService`)
     *  - Chuyển trạng thái Ticket → VALID
     *  - Áp dụng promotion (tăng counter) nếu còn slot
     *
     * Lưu ý: phương thức này thường được gọi từ `PaymentService` trong một giao dịch riêng
     * để đảm bảo tính nguyên tử giữa cập nhật trạng thái giao dịch và booking.
     *
     * @param bookingId ID của booking cần confirm
     * @throws AppException nếu booking không tồn tại, hết hạn hoặc trạng thái không hợp lệ
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


    /**
     * Hủy booking do user yêu cầu.
     *
     * Quy tắc:
     * - Không cho hủy booking đã được confirm (để đảm bảo luồng thanh toán và refund rõ ràng)
     *
     * @param bookingId ID booking cần huỷ
     * @return BookingResponse thông tin booking sau khi hủy
     * @throws AppException khi booking không tồn tại, không thuộc quyền sở hữu, hoặc đã xác nhận
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
     * Hủy nội bộ (dùng cho scheduled job hoặc logic backend).
     *
     * Hành vi giống `cancelBooking` nhưng không kiểm quyền user.
     *
     * @param bookingId ID booking cần huỷ
     */
    @Transactional
    public void cancelBookingInternal(String bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) return;
        doCancel(booking);
        bookingRepository.save(booking);
        log.warn("Booking cancelled (internal): code={}", booking.getBookingCode());
    }

    /**
     * Job định kỳ chạy mỗi phút để hủy các booking PENDING đã quá hạn.
     *
     * Lưu ý implement:
     * - Nên dùng truy vấn kèm lock phù hợp (pessimistic write) ở tầng repository
     *   nếu có khả năng xung đột với `confirmBooking` đang chạy đồng thời.
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

    /**
     * Hủy và trả ghế tùy theo trạng thái booking (LOCKED vs BOOKED).
     *
     * @param booking đối tượng booking đang được hủy
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
     * Sinh mã booking ngẫu nhiên (BKxxxxx). Dùng UUID để giảm khả năng trùng.
     * @return mã booking dạng chuỗi
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

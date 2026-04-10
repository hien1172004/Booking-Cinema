package org.example.cinemaBooking.Service.Payment;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.Config.VNPayConfig;
import org.example.cinemaBooking.DTO.Request.Payment.CreatePaymentRequest;
import org.example.cinemaBooking.DTO.Response.Payment.PaymentResponse;
import org.example.cinemaBooking.Entity.Booking;
import org.example.cinemaBooking.Entity.Payment;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.PaymentMapper;
import org.example.cinemaBooking.Repository.BookingRepository;
import org.example.cinemaBooking.Repository.PaymentRepository;
import org.example.cinemaBooking.Service.Auth.EmailService;
import org.example.cinemaBooking.Service.Booking.BookingService;
import org.example.cinemaBooking.Service.Notification.NotificationService;
import org.example.cinemaBooking.Service.Showtime.ShowTimeSeatService;
import org.example.cinemaBooking.Shared.enums.BookingStatus;
import org.example.cinemaBooking.Shared.enums.PaymentMethod;
import org.example.cinemaBooking.Shared.enums.PaymentStatus;
import org.example.cinemaBooking.Shared.enums.VNPayUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class PaymentService {

    PaymentRepository paymentRepository;
    BookingRepository bookingRepository;
    BookingService bookingService;
    ShowTimeSeatService showtimeSeatService;
    PaymentMapper paymentMapper;
    VNPayConfig vnPayConfig;
    NotificationService notificationService;
    EmailService emailService;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request,
                                         HttpServletRequest httpRequest) {
        Booking booking = bookingRepository.findByIdWithDetails(request.bookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);
        }
        if (booking.isExpired()) {
            throw new AppException(ErrorCode.BOOKING_EXPIRED);
        }

        String clientIp = resolveClientIp(request, httpRequest);

        var existing = paymentRepository.findByBookingId(booking.getId());
        if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.PENDING) {
            Payment existingPayment = existing.get();
            String url = buildVNPayUrl(booking, existingPayment, clientIp);
            PaymentResponse response = paymentMapper.toResponse(existingPayment);
            return withPaymentUrl(response, url);
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .amount(booking.getFinalPrice())
                .build();

        Payment saved = paymentRepository.save(payment);
        String paymentUrl = buildVNPayUrl(booking, saved, clientIp);

        log.info("Payment created: bookingCode={}, amount={}, ip={}",
                booking.getBookingCode(), booking.getFinalPrice(), clientIp);

        return withPaymentUrl(paymentMapper.toResponse(saved), paymentUrl);
    }

    public String handleIPN(Map<String, String> params) {
        if (!VNPayUtil.verifySecureHash(params, vnPayConfig.getHashSecret())) {
            log.warn("IPN invalid signature: {}", params);
            return "{\"RspCode\":\"97\",\"Message\":\"Invalid signature\"}";
        }

        String bookingCode = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        String amountStr = params.get("vnp_Amount");

        Payment payment = paymentRepository.findByBookingCode(bookingCode).orElse(null);
        if (payment == null) {
            log.warn("IPN: payment not found for bookingCode={}", bookingCode);
            return "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}";
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("IPN: already processed bookingCode={}", bookingCode);
            return "{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}";
        }

        BigDecimal expectedAmount = payment.getAmount().multiply(BigDecimal.valueOf(100));
        BigDecimal receivedAmount = new BigDecimal(amountStr);
        if (expectedAmount.compareTo(receivedAmount) != 0) {
            log.warn("IPN: amount mismatch booking={} expected={} received={}",
                    bookingCode, expectedAmount, receivedAmount);
            return "{\"RspCode\":\"04\",\"Message\":\"Amount invalid\"}";
        }

        if ("00".equals(responseCode)) {
            savePaymentStatus(payment.getId(), PaymentStatus.SUCCESS, transactionNo);

            try {
                bookingService.confirmBooking(payment.getBooking().getId());
            } catch (Exception e) {
                log.error("IPN: confirmBooking failed after payment saved! bookingCode={}, error={}",
                        bookingCode, e.getMessage(), e);
                return "{\"RspCode\":\"00\",\"Message\":\"Confirm success\"}";
            }

            log.info("IPN: payment success bookingCode={} txn={}", bookingCode, transactionNo);

            Booking booking = paymentRepository.findByBookingCode(bookingCode)
                    .map(Payment::getBooking)
                    .orElse(null);
            if (booking != null) {
                notificationService.notifyPaymentSuccess(booking);
                notificationService.notifyBookingSuccess(booking);
                emailService.sendBookingSuccessEmail(booking.getBookingCode());
            }
        } else {
            savePaymentStatus(payment.getId(), PaymentStatus.FAILED, transactionNo);

            log.warn("IPN: payment failed bookingCode={} responseCode={}",
                    bookingCode, responseCode);

            Booking booking = paymentRepository.findByBookingCode(bookingCode)
                    .map(Payment::getBooking)
                    .orElse(null);
            if (booking != null) {
                notificationService.notifyBookingCancelled(booking);
            }
        }

        return "{\"RspCode\":\"00\",\"Message\":\"Confirm success\"}";
    }

    @Transactional(readOnly = true)
    public PaymentResponse handleReturn(Map<String, String> params) {
        if (!VNPayUtil.verifySecureHash(params, vnPayConfig.getHashSecret())) {
            throw new AppException(ErrorCode.PAYMENT_INVALID_SIGNATURE);
        }

        String bookingCode = params.get("vnp_TxnRef");
        Payment payment = paymentRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public URI buildAppReturnUri(Map<String, String> params) {
        Map<String, String> queryParams = new HashMap<>();

        try {
            PaymentResponse payment = handleReturn(params);
            queryParams.put("bookingId", payment.bookingId());
            queryParams.put("bookingCode", payment.bookingCode());
            queryParams.put("status", safeStatusValue(payment.status(), params.get("vnp_ResponseCode")));
            if (payment.transactionId() != null && !payment.transactionId().isBlank()) {
                queryParams.put("transactionId", payment.transactionId());
            }
        } catch (AppException ex) {
            log.warn("VNPay return failed to resolve payment: {}", ex.getMessage());
            queryParams.put("status", "FAILED");
        }

        if (params.get("vnp_ResponseCode") != null) {
            queryParams.put("responseCode", params.get("vnp_ResponseCode"));
        }

        if (!queryParams.containsKey("bookingId")
                && params.get("vnp_TxnRef") != null
                && !params.get("vnp_TxnRef").isBlank()) {
            paymentRepository.findByBookingCode(params.get("vnp_TxnRef"))
                    .ifPresent(payment -> {
                        queryParams.put("bookingId", payment.getBooking().getId());
                        queryParams.put("bookingCode", payment.getBooking().getBookingCode());
                        if (!queryParams.containsKey("transactionId")
                                && payment.getTransactionId() != null
                                && !payment.getTransactionId().isBlank()) {
                            queryParams.put("transactionId", payment.getTransactionId());
                        }
                    });
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(vnPayConfig.getAppReturnUrl());
        queryParams.forEach(builder::queryParam);
        return builder.build(true).toUri();
    }

    @Transactional
    public PaymentResponse refund(String bookingId) {
        Payment payment = paymentRepository.findByBookingIdWithDetails(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new AppException(ErrorCode.PAYMENT_STATE_INVALID);
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment saved = paymentRepository.save(payment);

        Booking booking = payment.getBooking();
        bookingService.cancelBookingInternal(booking.getId());

        log.info("Payment refunded: bookingId={}, amount={}", bookingId, payment.getAmount());

        emailService.sendCancelledEmail(booking);
        notificationService.notifyBookingCancelled(booking);

        return paymentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(String bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        return paymentMapper.toResponse(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePaymentStatus(String paymentId, PaymentStatus status, String transactionNo) {
        paymentRepository.findById(paymentId).ifPresent(payment -> {
            payment.setStatus(status);
            payment.setTransactionId(transactionNo);
            paymentRepository.save(payment);
        });
    }

    private String buildVNPayUrl(Booking booking, Payment payment, String clientIp) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", vnPayConfig.getVersion());
        params.put("vnp_Command", vnPayConfig.getCommand());
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", toVNPayAmount(booking.getFinalPrice()));
        params.put("vnp_CurrCode", vnPayConfig.getCurrencyCode());
        params.put("vnp_TxnRef", booking.getBookingCode());
        params.put("vnp_OrderInfo", "Thanh toan ve xem phim " + booking.getBookingCode());
        params.put("vnp_OrderType", "billpayment");
        params.put("vnp_Locale", vnPayConfig.getLocale());
        params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", VNPayUtil.getCurrentTime());
        params.put("vnp_ExpireDate", VNPayUtil.getExpireTime(15));

        log.info("Client IP used for vnp_IpAddr: {}", clientIp);
        log.debug("VNPay return URL: {}", vnPayConfig.getReturnUrl());
        log.debug("VNPay app return URL: {}", vnPayConfig.getAppReturnUrl());
        log.debug("Payment pending entity id reused: {}", payment.getId());

        return VNPayUtil.buildPaymentUrl(
                vnPayConfig.getPaymentUrl(), params, vnPayConfig.getHashSecret());
    }

    private String toVNPayAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).toBigInteger().toString();
    }

    private String resolveClientIp(CreatePaymentRequest request,
                                   HttpServletRequest httpRequest) {
        if (request.clientIp() != null && !request.clientIp().isBlank()) {
            return request.clientIp();
        }
        String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return httpRequest.getRemoteAddr();
    }

    private PaymentResponse withPaymentUrl(PaymentResponse response, String url) {
        return new PaymentResponse(
                response.paymentId(),
                response.bookingId(),
                response.bookingCode(),
                response.paymentMethod(),
                response.status(),
                response.amount(),
                response.transactionId(),
                url,
                response.createdAt()
        );
    }

    private String safeStatusValue(PaymentStatus status, String responseCode) {
        if (status != null) {
            return status.name();
        }
        if ("00".equals(responseCode)) {
            return PaymentStatus.PENDING.name();
        }
        return PaymentStatus.FAILED.name();
    }
}

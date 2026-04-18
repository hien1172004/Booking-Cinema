package org.example.cinemaBooking.unit.service;

import jakarta.servlet.http.HttpServletRequest;
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
import org.example.cinemaBooking.Service.Payment.PaymentService;
import org.example.cinemaBooking.Service.Showtime.ShowTimeSeatService;
import org.example.cinemaBooking.Shared.enums.BookingStatus;
import org.example.cinemaBooking.Shared.enums.PaymentStatus;
import org.example.cinemaBooking.Shared.enums.VNPayUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingService bookingService;
    @Mock
    private ShowTimeSeatService showtimeSeatService;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private VNPayConfig vnPayConfig;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private PaymentService paymentService;

    private MockedStatic<VNPayUtil> vnPayUtilMock;
    private Booking booking;
    private Payment payment;

    @BeforeEach
    void setUp() {
        vnPayUtilMock = mockStatic(VNPayUtil.class);

        booking = Booking.builder()
                .bookingCode("BK123")
                .status(BookingStatus.PENDING)
                .finalPrice(BigDecimal.valueOf(100000))
                .build();
        booking.setId("bk-001");

        payment = Payment.builder()
                .booking(booking)
                .amount(BigDecimal.valueOf(100000))
                .status(PaymentStatus.PENDING)
                .build();
        payment.setId("pay-001");

        lenient().when(vnPayConfig.getHashSecret()).thenReturn("secret");
        lenient().when(vnPayConfig.getAppReturnUrl()).thenReturn("http://localhost:3000/return");
    }

    @AfterEach
    void tearDown() {
        vnPayUtilMock.close();
    }

    @Nested
    class CreatePaymentTests {
        @Test
        void createPayment_Success() {
            // Given
            CreatePaymentRequest request = new CreatePaymentRequest("bk-001", "127.0.0.1");
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);

            when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));
            when(paymentRepository.findByBookingId("bk-001")).thenReturn(Optional.empty());
            when(paymentRepository.save(any())).thenReturn(payment);
            when(paymentMapper.toResponse(any())).thenReturn(
                    new PaymentResponse("pay-001", "bk-001", "BK123", null, PaymentStatus.PENDING, BigDecimal.valueOf(100000), null, null, null)
            );
            
            vnPayUtilMock.when(() -> VNPayUtil.buildPaymentUrl(any(), any(), any())).thenReturn("http://vnpay.url");

            // When
            PaymentResponse response = paymentService.createPayment(request, httpRequest);

            // Then
            assertThat(response.paymentUrl()).isEqualTo("http://vnpay.url");
            verify(paymentRepository).save(any());
        }

        @Test
        void createPayment_BookingStatusInvalid_ThrowsException() {
            // Given
            booking.setStatus(BookingStatus.CONFIRMED);
            CreatePaymentRequest request = new CreatePaymentRequest("bk-001", "127.0.0.1");
            when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));

            // When & Then
            assertThatThrownBy(() -> paymentService.createPayment(request, mock(HttpServletRequest.class)))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_STATUS_INVALID);
        }

        @Test
        void createPayment_ReuseExistingPendingPayment() {
            // Given
            CreatePaymentRequest request = new CreatePaymentRequest("bk-001", "127.0.0.1");
            when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));
            when(paymentRepository.findByBookingId("bk-001")).thenReturn(Optional.of(payment));
            when(paymentMapper.toResponse(any())).thenReturn(
                    new PaymentResponse("pay-001", "bk-001", "BK123", null, PaymentStatus.PENDING, BigDecimal.valueOf(100000), null, null, null)
            );
            vnPayUtilMock.when(() -> VNPayUtil.buildPaymentUrl(any(), any(), any())).thenReturn("http://vnpay.url");

            // When
            PaymentResponse response = paymentService.createPayment(request, mock(HttpServletRequest.class));

            // Then
            assertThat(response.paymentUrl()).isEqualTo("http://vnpay.url");
            verify(paymentRepository, never()).save(any());
        }

        @Test
        void createPayment_BookingExpired_ThrowsException() {
            // Given
            booking.setExpiredAt(java.time.LocalDateTime.now().minusMinutes(1)); // Đã hết hạn
            CreatePaymentRequest request = new CreatePaymentRequest("bk-001", "127.0.0.1");
            when(bookingRepository.findByIdWithDetails("bk-001")).thenReturn(Optional.of(booking));

            // When & Then
            assertThatThrownBy(() -> paymentService.createPayment(request, mock(HttpServletRequest.class)))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_EXPIRED);
        }
    }

    @Nested
    class IPNTests {
        @Test
        void handleIPN_Success() {
            // Given
            Map<String, String> params = new HashMap<>();
            params.put("vnp_SecureHash", "hash");
            params.put("vnp_TxnRef", "BK123");
            params.put("vnp_ResponseCode", "00");
            params.put("vnp_TransactionNo", "TXN123");
            params.put("vnp_Amount", "10000000"); // 100,000 * 100

            when(vnPayConfig.getHashSecret()).thenReturn("secret");
            vnPayUtilMock.when(() -> VNPayUtil.verifySecureHash(any(), anyString())).thenReturn(true);
            when(paymentRepository.findByBookingCode("BK123")).thenReturn(Optional.of(payment));
            when(paymentRepository.findById(any())).thenReturn(Optional.of(payment));

            // When
            String result = paymentService.handleIPN(params);

            // Then
            assertThat(result).contains("00");
            verify(bookingService).confirmBooking("bk-001");
            verify(paymentRepository).save(payment);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        void handleIPN_InvalidSignature_ReturnsErrorCode() {
            // Given
            Map<String, String> params = new HashMap<>();
            vnPayUtilMock.when(() -> VNPayUtil.verifySecureHash(any(), any())).thenReturn(false);

            // When
            String result = paymentService.handleIPN(params);

            // Then
            assertThat(result).contains("97");
        }

        @Test
        void handleIPN_OrderNotFound_ReturnsErrorCode() {
            // Given
            Map<String, String> params = new HashMap<>();
            params.put("vnp_TxnRef", "UNKNOWN");
            vnPayUtilMock.when(() -> VNPayUtil.verifySecureHash(any(), any())).thenReturn(true);
            when(paymentRepository.findByBookingCode("UNKNOWN")).thenReturn(Optional.empty());

            // When
            String result = paymentService.handleIPN(params);

            // Then
            assertThat(result).contains("01");
        }

        @Test
        void handleIPN_AlreadyProcessed_ReturnsErrorCode() {
            // Given
            payment.setStatus(PaymentStatus.SUCCESS);
            Map<String, String> params = new HashMap<>();
            params.put("vnp_TxnRef", "BK123");
            vnPayUtilMock.when(() -> VNPayUtil.verifySecureHash(any(), any())).thenReturn(true);
            when(paymentRepository.findByBookingCode("BK123")).thenReturn(Optional.of(payment));

            // When
            String result = paymentService.handleIPN(params);

            // Then
            assertThat(result).contains("02");
        }

        @Test
        void handleIPN_AmountMismatch_ReturnsErrorCode() {
            // Given
            Map<String, String> params = new HashMap<>();
            params.put("vnp_TxnRef", "BK123");
            params.put("vnp_Amount", "5000000"); // 50,000 * 100 (Expected 100,000)
            vnPayUtilMock.when(() -> VNPayUtil.verifySecureHash(any(), any())).thenReturn(true);
            when(paymentRepository.findByBookingCode("BK123")).thenReturn(Optional.of(payment));

            // When
            String result = paymentService.handleIPN(params);

            // Then
            assertThat(result).contains("04");
        }

        @Test
        void handleIPN_PaymentFailed_SuccessResponseRecorded() {
            // Given
            Map<String, String> params = new HashMap<>();
            params.put("vnp_TxnRef", "BK123");
            params.put("vnp_ResponseCode", "99"); // Failed
            params.put("vnp_Amount", "10000000");
            vnPayUtilMock.when(() -> VNPayUtil.verifySecureHash(any(), any())).thenReturn(true);
            when(paymentRepository.findByBookingCode("BK123")).thenReturn(Optional.of(payment));
            when(paymentRepository.findById(any())).thenReturn(Optional.of(payment));

            // When
            String result = paymentService.handleIPN(params);

            // Then
            assertThat(result).contains("00"); // VNPay IPN always wants 00 if processed successfully
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            verify(notificationService).notifyBookingCancelled(any());
        }

        @Test
        void handleIPN_ConfirmBookingThrowsException_ReturnsSuccess() {
            // Given
            Map<String, String> params = new HashMap<>();
            params.put("vnp_SecureHash", "hash");
            params.put("vnp_TxnRef", "BK123");
            params.put("vnp_ResponseCode", "00");
            params.put("vnp_Amount", "10000000");

            vnPayUtilMock.when(() -> VNPayUtil.verifySecureHash(any(), anyString())).thenReturn(true);
            when(paymentRepository.findByBookingCode("BK123")).thenReturn(Optional.of(payment));
            when(paymentRepository.findById(any())).thenReturn(Optional.of(payment));
            doThrow(new RuntimeException("Confirm failed")).when(bookingService).confirmBooking(any());

            // When
            String result = paymentService.handleIPN(params);

            // Then
            assertThat(result).contains("00"); // Vẫn trả về thành công vì payment đã lưu
            verify(paymentRepository).save(payment);
        }
    }

    @Nested
    class DetailTests {
        @Test
        void handleReturn_Success() {
            // Given
            vnPayUtilMock.when(() -> VNPayUtil.verifySecureHash(any(), any())).thenReturn(true);
            when(paymentRepository.findByBookingCode("BK123")).thenReturn(Optional.of(payment));
            when(paymentMapper.toResponse(payment)).thenReturn(mock(PaymentResponse.class));

            Map<String, String> params = new HashMap<>();
            params.put("vnp_TxnRef", "BK123");

            // When
            PaymentResponse response = paymentService.handleReturn(params);

            // Then
            assertThat(response).isNotNull();
        }

        @Test
        void handleReturn_InvalidSignature_ThrowsException() {
            // Given
            vnPayUtilMock.when(() -> VNPayUtil.verifySecureHash(any(), any())).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> paymentService.handleReturn(new HashMap<>()))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_INVALID_SIGNATURE);
        }

        @Test
        void handleReturn_PaymentNotFound_ThrowsException() {
            // Given
            vnPayUtilMock.when(() -> VNPayUtil.verifySecureHash(any(), any())).thenReturn(true);
            when(paymentRepository.findByBookingCode("UNKNOWN")).thenReturn(Optional.empty());

            Map<String, String> params = new HashMap<>();
            params.put("vnp_TxnRef", "UNKNOWN");

            // When & Then
            assertThatThrownBy(() -> paymentService.handleReturn(params))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
        }

        @Test
        void buildAppReturnUri_Success() {
            // Given
            Map<String, String> params = new HashMap<>();
            params.put("vnp_SecureHash", "hash");
            params.put("vnp_TxnRef", "BK123");
            params.put("vnp_ResponseCode", "00");

            vnPayUtilMock.when(() -> VNPayUtil.verifySecureHash(any(), any())).thenReturn(true);
            when(paymentRepository.findByBookingCode("BK123")).thenReturn(Optional.of(payment));
            when(paymentMapper.toResponse(any())).thenReturn(
                    new PaymentResponse("pay-001", "bk-001", "BK123", null, PaymentStatus.SUCCESS, BigDecimal.valueOf(100), "TXN", null, null)
            );
            when(vnPayConfig.getAppReturnUrl()).thenReturn("myapp://payment");

            // When
            java.net.URI uri = paymentService.buildAppReturnUri(params);

            // Then
            assertThat(uri.toString()).contains("bookingId=bk-001");
            assertThat(uri.toString()).contains("status=SUCCESS");
        }

        @Test
        void buildAppReturnUri_HandleReturnThrowsAppException_Fallback() {
            // Given
            Map<String, String> params = new HashMap<>();
            params.put("vnp_TxnRef", "BK123");
            
            vnPayUtilMock.when(() -> VNPayUtil.verifySecureHash(any(), any())).thenReturn(false); // Sẽ gây AppException ở handleReturn
            when(vnPayConfig.getAppReturnUrl()).thenReturn("myapp://payment");
            when(paymentRepository.findByBookingCode("BK123")).thenReturn(Optional.of(payment));

            // When
            java.net.URI uri = paymentService.buildAppReturnUri(params);

            // Then
            assertThat(uri.toString()).contains("status=FAILED"); // Gặp lỗi ở handleReturn -> trả về FAILED
            assertThat(uri.toString()).contains("bookingId=bk-001"); // Fallback check at line 190
        }

        @Test
        void resolveClientIp_XForwardedFor() {
            // Given
            CreatePaymentRequest request = new CreatePaymentRequest("bk-001", null);
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");
            when(bookingRepository.findByIdWithDetails(any())).thenReturn(Optional.of(booking));
            when(paymentRepository.save(any())).thenReturn(payment);
            when(paymentMapper.toResponse(any())).thenReturn(mock(PaymentResponse.class));
            vnPayUtilMock.when(() -> VNPayUtil.buildPaymentUrl(any(), any(), any())).thenReturn("url");

            // When
            paymentService.createPayment(request, httpRequest);

            // Then
            // logic verify buildPaymentUrl received "10.0.0.1"
            vnPayUtilMock.verify(() -> VNPayUtil.buildPaymentUrl(any(), argThat(map -> map.get("vnp_IpAddr").equals("10.0.0.1")), any()));
        }

        @Test
        void resolveClientIp_DefaultRemoteAddr() {
            // Given
            CreatePaymentRequest request = new CreatePaymentRequest("bk-001", null);
            HttpServletRequest httpRequest = mock(HttpServletRequest.class);
            when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");
            
            when(bookingRepository.findByIdWithDetails(any())).thenReturn(Optional.of(booking));
            when(paymentRepository.save(any())).thenReturn(payment);
            when(paymentMapper.toResponse(any())).thenReturn(mock(PaymentResponse.class));
            vnPayUtilMock.when(() -> VNPayUtil.buildPaymentUrl(any(), any(), any())).thenReturn("url");

            // When
            paymentService.createPayment(request, httpRequest);

            // Then
            vnPayUtilMock.verify(() -> VNPayUtil.buildPaymentUrl(any(), argThat(map -> map.get("vnp_IpAddr").equals("192.168.1.1")), any()));
        }

        @Test
        void getPaymentByBookingId_NotFound_ThrowsException() {
            // Given
            when(paymentRepository.findByBookingId("ghost")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> paymentService.getPaymentByBookingId("ghost"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
        }
    }

    @Nested
    class RefundTests {
        @Test
        void refund_Success() {
            // Given
            payment.setStatus(PaymentStatus.SUCCESS);
            when(paymentRepository.findByBookingIdWithDetails("bk-001")).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any())).thenReturn(payment);
            when(paymentMapper.toResponse(any())).thenReturn(mock(PaymentResponse.class));

            // When
            paymentService.refund("bk-001");

            // Then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            verify(bookingService).cancelBookingInternal("bk-001");
        }

        @Test
        void refund_PaymentNotFound_ThrowsException() {
            when(paymentRepository.findByBookingIdWithDetails(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.refund("unknown"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
        }

        @Test
        void refund_StatusNotSuccess_ThrowsException() {
            payment.setStatus(PaymentStatus.PENDING);
            when(paymentRepository.findByBookingIdWithDetails("bk-001")).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.refund("bk-001"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_STATE_INVALID);
        }
    }
}

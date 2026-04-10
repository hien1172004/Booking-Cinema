package org.example.cinemaBooking.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Payment.CreatePaymentRequest;
import org.example.cinemaBooking.DTO.Response.Payment.PaymentResponse;
import org.example.cinemaBooking.Service.Payment.PaymentService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Payment.BASE)
@Tag(name = "Payment", description = "xu ly thanh toan, IPN va tra ve tu VNPay")
public class PaymentController {
    PaymentService paymentService;

    @Operation(summary = "Tao thanh toan moi",
            description = "Tao mot thanh toan moi cho mot dat ve. Yeu cau nguoi dung da xac thuc.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest
    ) {
        PaymentResponse paymentResponse = paymentService.createPayment(request, httpRequest);
        return ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Payment created successfully")
                .data(paymentResponse)
                .build();
    }

    @Operation(summary = "Xu ly tra ve tu VNPay",
            description = "Endpoint backend nhan redirect tu VNPay, verify xong se chuyen thang ve mobile deep link cua app.")
    @GetMapping("/vnpay/return")
    public ResponseEntity<Void> handleReturn(@RequestParam Map<String, String> params) {
        URI redirectUri = paymentService.buildAppReturnUri(params);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUri.toString())
                .build();
    }

    @Operation(summary = "IPN tu VNPay",
            description = "Endpoint nhan Instant Payment Notification (IPN) tu VNPay de cap nhat trang thai thanh toan.")
    @GetMapping("/vnpay/ipn")
    public ApiResponse<String> handleIPN(@RequestParam Map<String, String> params) {
        String result = paymentService.handleIPN(params);
        log.info("IPN processed with result: {}", result);
        return ApiResponse.<String>builder()
                .success(true)
                .message("IPN processed successfully")
                .data(result)
                .build();
    }

    @Operation(summary = "Lay thong tin thanh toan theo booking",
            description = "Lay chi tiet thanh toan lien quan den bookingId.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PaymentResponse> getPaymentByBooking(@PathVariable String bookingId) {
        return ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Payment retrieved successfully")
                .data(paymentService.getPaymentByBookingId(bookingId))
                .build();
    }

    @Operation(summary = "Hoan tien cho booking",
            description = "Thuc hien hoan tien cho mot booking cu the (chi ADMIN).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/booking/{bookingId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PaymentResponse> refund(@PathVariable String bookingId) {
        return ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Refund processed successfully")
                .data(paymentService.refund(bookingId))
                .build();
    }
}

// VNPayCallbackParams.java — map query params từ VNPay redirect/IPN
package org.example.cinemaBooking.DTO.Request.Payment;

public record VNPayCallbackParams(
    String vnp_TmnCode,
    String vnp_Amount,
    String vnp_BankCode,
    String vnp_BankTranNo,
    String vnp_CardType,
    String vnp_PayDate,
    String vnp_OrderInfo,
    String vnp_TransactionNo,
    String vnp_ResponseCode,    // "00" = success
    String vnp_TransactionStatus,
    String vnp_TxnRef,          // bookingCode
    String vnp_SecureHash
) {}
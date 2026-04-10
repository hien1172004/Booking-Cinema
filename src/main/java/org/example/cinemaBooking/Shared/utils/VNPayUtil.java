// VNPayUtil.java
package org.example.cinemaBooking.Shared.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class VNPayUtil {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VNPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** HMAC-SHA512 để tạo và verify secure hash */
    public static String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Cannot compute HMAC-SHA512", e);
        }
    }

    /**
     * Build query string + tính vnp_SecureHash.
     * Params phải được sort alphabet trước khi hash.
     */
    public static String buildPaymentUrl(String baseUrl,
                                         Map<String, String> params,
                                         String hashSecret) {
        // Lọc các field có value
        List<String> validFields = params.keySet().stream()
                .filter(f -> params.get(f) != null && !params.get(f).isEmpty())
                .sorted()
                .toList();

        StringBuilder hashData = new StringBuilder(); // dùng tính hash (KHÔNG encode)
        StringBuilder query = new StringBuilder();    // dùng làm URL (encode)

        for (int i = 0; i < validFields.size(); i++) {
            String field = validFields.get(i);
            String value = params.get(field);

            // HashData: key=value nguyên bản
            hashData.append(field).append('=').append(value);

            // Query: encode key + value
            query.append(URLEncoder.encode(field, StandardCharsets.US_ASCII))
                    .append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));

            if (i < validFields.size() - 1) {
                hashData.append('&');
                query.append('&');
            }
        }

        // Tính secure hash
        String secureHash = hmacSHA512(hashSecret, hashData.toString());

        // Trả về URL đầy đủ
        return baseUrl + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    /** Verify callback/IPN từ VNPay */
    public static boolean verifySecureHash(Map<String, String> params, String hashSecret) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null) return false;

        Map<String, String> copy = new HashMap<>(params);
        copy.remove("vnp_SecureHash");
        copy.remove("vnp_SecureHashType");

        List<String> validFields = copy.keySet().stream()
                .filter(f -> copy.get(f) != null && !copy.get(f).isEmpty())
                .sorted()
                .toList();

        StringBuilder hashData = new StringBuilder();
        for (int i = 0; i < validFields.size(); i++) {
            String field = validFields.get(i);
            String value = copy.get(field);
            hashData.append(field).append('=').append(value);
            if (i < validFields.size() - 1) hashData.append('&');
        }

        String computedHash = hmacSHA512(hashSecret, hashData.toString());
        return computedHash.equalsIgnoreCase(receivedHash);
    }

    public static String getCurrentTime() {
        return LocalDateTime.now(VNPAY_ZONE).format(FORMATTER);
    }

    public static String getExpireTime(int minutes) {
        return LocalDateTime.now(VNPAY_ZONE).plusMinutes(minutes).format(FORMATTER);
    }

}

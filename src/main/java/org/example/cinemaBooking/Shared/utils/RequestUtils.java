package org.example.cinemaBooking.Shared.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RequestUtils {

    /**
     * Lấy địa chỉ IP của client từ request, hỗ trợ các header truyền qua Proxy/Load Balancer.
     *
     * @param request HttpServletRequest
     * @return Client IP address
     */
    public String getClientIp(HttpServletRequest request) {
        // Cloudflare IP
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp;
        }

        // X-Forwarded-For (thường chứa chuỗi các IP, lấy cái đầu tiên)
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }

        // X-Real-IP (thường dùng ở Nginx)
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        // Mặc định
        return request.getRemoteAddr();
    }
}

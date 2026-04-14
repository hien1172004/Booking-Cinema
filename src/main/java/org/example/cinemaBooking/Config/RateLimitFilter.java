package org.example.cinemaBooking.Config;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Service.RateLimit.RateLimitService;
import org.example.cinemaBooking.Shared.constant.RateLimitConstants;
import org.example.cinemaBooking.Shared.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@Order(1)
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final HandlerExceptionResolver resolver;

    public RateLimitFilter(RateLimitService rateLimitService,
                           @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.rateLimitService = rateLimitService;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // ✅ bypass endpoint hệ thống
        if (path.startsWith("/actuator") || path.startsWith("/health")) {
            filterChain.doFilter(request, response);
            log.info("[RATE_LIMIT] BYPASS SYSTEM PATH={}", path);
            return;

        }

        String key = RateLimitConstants.PREFIX_GLOBAL + ":" + RequestUtils.getClientIp(request);
        ConsumptionProbe probe = rateLimitService.consume(key);

        // headers
        response.addHeader(RateLimitConstants.HEADER_LIMIT, String.valueOf(RateLimitConstants.CAPACITY));
        response.addHeader(RateLimitConstants.HEADER_REMAINING, String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            log.debug("[RATE_LIMIT] PASS ip={}, path={}, remaining={}",
                    RequestUtils.getClientIp(request),
                    path,
                    probe.getRemainingTokens());
            return;
        }

        long retryAfter = Math.max(1,
                probe.getNanosToWaitForRefill() / 1_000_000_000);

        response.addHeader(RateLimitConstants.HEADER_RETRY, String.valueOf(retryAfter));

        log.warn("[RATE_LIMIT] BLOCKED ip={}, path={}, remaining={}, retryAfter={}s",
                RequestUtils.getClientIp(request),
                path,
                probe.getRemainingTokens(),
                retryAfter);
        resolver.resolveException(
                request,
                response,
                null,
                new AppException(ErrorCode.TOO_MANY_REQUESTS)
        );
    }

}
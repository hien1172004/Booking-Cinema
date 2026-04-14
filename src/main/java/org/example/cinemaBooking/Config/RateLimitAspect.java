package org.example.cinemaBooking.Config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Shared.constraints.RateLimit;
import org.example.cinemaBooking.Shared.utils.RequestUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final ProxyManager<String> bucketProxyManager;
    private final Map<String, BucketConfiguration> configCache = new ConcurrentHashMap<>();


    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {

        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attrs == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attrs.getRequest();

        // 1. Lấy thông tin định danh (IP + Method Signature)
        String ip = RequestUtils.getClientIp(request);
        String methodName = joinPoint.getSignature().toLongString();

        // 2. Tạo Key ổn định (Tránh Path Variable explosion)
        String key = rateLimit.keyPrefix() + ":" + ip + ":" + methodName;

        // 3. Lấy Configuration từ Cache (Tối ưu hóa object creation)
        BucketConfiguration config = configCache.computeIfAbsent(methodName,
                k -> buildConfig(rateLimit));

        // 4. Kiểm tra Bucket
        Bucket bucket = bucketProxyManager.builder().build(key, () -> config);

        if (bucket.tryConsume(1)) {
            return joinPoint.proceed();
        }

        throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
    }


    private BucketConfiguration buildConfig(RateLimit rateLimit) {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(rateLimit.capacity())
                        .refillGreedy(rateLimit.refillPerMinute(), Duration.ofMinutes(1))
                        .build())
                .build();
    }
}
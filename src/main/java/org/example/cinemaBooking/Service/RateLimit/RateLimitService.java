package org.example.cinemaBooking.Service.RateLimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final ProxyManager<String> bucketProxyManager;
    private final BucketConfiguration bucketConfiguration;

    public ConsumptionProbe consume(String key) {

        Bucket bucket = bucketProxyManager.builder()
                .build("rate_limit:" + key, () -> bucketConfiguration);

        return bucket.tryConsumeAndReturnRemaining(1);
    }
}